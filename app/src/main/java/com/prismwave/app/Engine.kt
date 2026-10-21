package com.prismwave.app

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

    suspend fun streamUrl(videoUrl: String): String = withContext(Dispatchers.IO) {
        init()
        val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
        val audio = info.audioStreams.filter { it.isUrl }
        val best = audio.filter { it.format == MediaFormat.M4A }.maxByOrNull { it.averageBitrate }
            ?: audio.maxByOrNull { it.averageBitrate }
        best?.content ?: throw IOException("No audio stream found")
    }
}
