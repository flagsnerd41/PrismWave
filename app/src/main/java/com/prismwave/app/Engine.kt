package com.prismwave.app

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

private const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

class AppDownloader : Downloader() {
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val dataToSend = request.dataToSend()
        val body = dataToSend?.toRequestBody(null, 0, dataToSend.size)
        val rb = okhttp3.Request.Builder()
            .method(request.httpMethod(), body)
            .url(request.url())
            .addHeader("User-Agent", USER_AGENT)
        for ((name, values) in request.headers()) {
            rb.removeHeader(name)
            for (v in values) rb.addHeader(name, v)
        }
        val response = client.newCall(rb.build()).execute()
        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha challenge requested", request.url())
        }
        val text = response.body?.string()
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            text,
            response.request.url.toString()
        )
    }
}

object Engine {
    private var ready = false

    private fun init() {
        if (!ready) {
            NewPipe.init(AppDownloader(), Localization("en", "US"))
            ready = true
        }
    }

    private fun StreamInfoItem.toSong() = Song(
        id = url,
        title = name,
        artist = (uploaderName ?: "Unknown").removeSuffix(" - Topic"),
        seconds = duration.toInt().coerceAtLeast(0),
        url = url,
        thumb = thumbnails.maxByOrNull { it.height }?.url
    )

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        init()
        val yt = ServiceList.YouTube
        val handler = yt.searchQHFactory.fromQuery(
            query,
            listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS),
            ""
        )
        val info = SearchInfo.getInfo(yt, handler)
        info.relatedItems
            .filterIsInstance<StreamInfoItem>()
            .filter { it.duration > 0 }
            .map { it.toSong() }
    }

    // kind: 0 = All, 1 = Songs, 2 = Videos, 3 = Albums, 4 = Artists, 5 = Playlists
    suspend fun searchHits(query: String, kind: Int): List<Hit> = withContext(Dispatchers.IO) {
        init()
        val yt = ServiceList.YouTube
        fun run(filter: String): List<InfoItem> {
            val handler = yt.searchQHFactory.fromQuery(query, listOf(filter), "")
            return SearchInfo.getInfo(yt, handler).relatedItems
        }
        fun songs(filter: String, limit: Int): List<Hit> =
            run(filter).filterIsInstance<StreamInfoItem>().filter { it.duration > 0 }.take(limit).map {
                val sg = it.toSong()
                Hit(0, sg.title, sg.artist, sg.thumb, sg.url, sg)
            }
        fun lists(filter: String, k: Int): List<Hit> =
            run(filter).filterIsInstance<PlaylistInfoItem>().map {
                Hit(k, it.name, it.uploaderName ?: "", it.thumbnails.maxByOrNull { t -> t.height }?.url, it.url)
            }
        when (kind) {
            1 -> songs(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS, 50)
            2 -> songs(YoutubeSearchQueryHandlerFactory.MUSIC_VIDEOS, 50)
            3 -> lists(YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS, 1)
            4 -> run(YoutubeSearchQueryHandlerFactory.MUSIC_ARTISTS).filterIsInstance<ChannelInfoItem>().map {
                Hit(3, it.name, "Artist", it.thumbnails.maxByOrNull { t -> t.height }?.url, it.url)
            }
            5 -> lists(YoutubeSearchQueryHandlerFactory.MUSIC_PLAYLISTS, 2)
            else -> songs(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS, 20) +
                (try {
                    songs(YoutubeSearchQueryHandlerFactory.MUSIC_VIDEOS, 6)
                } catch (e: Exception) {
                    emptyList()
                })
        }
    }

    suspend fun related(url: String): List<Song> = withContext(Dispatchers.IO) {
        infoFor(url)
            .relatedItems.filterIsInstance<StreamInfoItem>().filter { it.duration > 0 }.map { it.toSong() }
    }

    suspend fun collection(url: String): List<Song> = withContext(Dispatchers.IO) {
        init()
        val info = PlaylistInfo.getInfo(ServiceList.YouTube, url)
        info.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toSong() }
    }

    // main = best video up to maxH, low = tiny copy for the blurred backdrop
    suspend fun videoPair(videoUrl: String, maxH: Int): Pair<String, String>? = withContext(Dispatchers.IO) {
        val info = infoFor(videoUrl)
        // a "song" (auto-generated Topic upload) only has a still picture, so no video for it
        val isSong = (info.uploaderName ?: "").endsWith(" - Topic") ||
            (info.description?.content ?: "").startsWith("Provided to YouTube by")
        if (isSong) return@withContext null
        val vs = info.videoOnlyStreams.filter { it.isUrl && it.height > 0 }
        if (vs.isEmpty()) return@withContext null
        val mp4 = vs.filter { it.format == MediaFormat.MPEG_4 }
        val pool = if (mp4.isNotEmpty()) mp4 else vs
        val main = (pool.filter { it.height <= maxH }.maxByOrNull { it.height } ?: pool.minByOrNull { it.height })?.content
        val low = (pool.filter { it.height <= 240 }.maxByOrNull { it.height } ?: pool.minByOrNull { it.height })?.content
        if (main == null || low == null) null else Pair(main, low)
    }

    private val urlCache = HashMap<String, Pair<String, Long>>()

    private val infoCache = HashMap<String, Pair<StreamInfo, Long>>()
    private val infoLocks = HashMap<String, kotlinx.coroutines.sync.Mutex>()

    fun forget(videoUrl: String) {
        synchronized(urlCache) { urlCache.remove(videoUrl) }
        synchronized(infoCache) { infoCache.remove(videoUrl) }
    }

    // the same song is only fetched once, even when several parts of the app ask at the same time
    private suspend fun infoFor(videoUrl: String): StreamInfo {
        val lock = synchronized(infoLocks) { infoLocks.getOrPut(videoUrl) { kotlinx.coroutines.sync.Mutex() } }
        lock.lock()
        try {
            val nowMs = System.currentTimeMillis()
            val hit = synchronized(infoCache) { infoCache[videoUrl] }
            if (hit != null && nowMs - hit.second < 4L * 3600 * 1000) return hit.first
            init()
            val fresh = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
            synchronized(infoCache) {
                if (infoCache.size > 8) infoCache.clear()
                infoCache[videoUrl] = Pair(fresh, nowMs)
            }
            return fresh
        } finally {
            lock.unlock()
        }
    }

    suspend fun streamUrl(videoUrl: String): String = withContext(Dispatchers.IO) {
        val nowMs = System.currentTimeMillis()
        val hit = synchronized(urlCache) { urlCache[videoUrl] }
        if (hit != null && nowMs - hit.second < 4L * 3600 * 1000) return@withContext hit.first
        val info = infoFor(videoUrl)
        val audio = info.audioStreams.filter { it.isUrl }
        val best = audio.filter { it.format == MediaFormat.M4A }.maxByOrNull { it.averageBitrate }
            ?: audio.maxByOrNull { it.averageBitrate }
        val link = best?.content ?: throw IOException("No audio stream found")
        synchronized(urlCache) { urlCache[videoUrl] = Pair(link, nowMs) }
        link
    }
}


// kind: 0 = song or video, 1 = album, 2 = playlist, 3 = artist
data class Hit(
    val kind: Int,
    val title: String,
    val sub: String,
    val thumb: String?,
    val url: String,
    val song: Song? = null
)
