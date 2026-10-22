package com.prismwave.app

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class Screen { Home, Search, Library, Settings }

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val seconds: Int,
    val url: String,
    val thumb: String?
) {
    val color: Color
        get() = Color.hsv((kotlin.math.abs(id.hashCode()) % 360).toFloat(), 0.5f, 0.55f)
}

private fun Song.toJson(): JSONObject = JSONObject()
    .put("id", id).put("title", title).put("artist", artist)
    .put("seconds", seconds).put("url", url).put("thumb", thumb ?: "")

private fun JSONObject.toSong(): Song = Song(
    getString("id"), getString("title"), getString("artist"),
    getInt("seconds"), getString("url"), optString("thumb").ifEmpty { null }
)

data class Playlist(val id: String, val name: String, val songs: List<Song>)

private fun Playlist.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("songs", JSONArray(songs.map { it.toJson() }))

private fun JSONObject.toPlaylist(): Playlist {
    val arr = getJSONArray("songs")
    return Playlist(
        getString("id"), getString("name"),
        (0 until arr.length()).map { arr.getJSONObject(it).toSong() }
    )
}

class AppState(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("prismwave", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private val counts = HashMap<String, Int>()
    private val known = HashMap<String, Song>()
    private val loadingFeeds = HashSet<String>()
    private var ticks = 0

    var screen by mutableStateOf(Screen.Home)
    var current by mutableStateOf<Song?>(null)
    var playing by mutableStateOf(false)
    var position by mutableStateOf(0f)
    var showPlayer by mutableStateOf(false)
    var glass by mutableStateOf(0.75f)
    var batterySaver by mutableStateOf(false)
    var accent by mutableStateOf(Gold)
    var shuffle by mutableStateOf(false)
    var repeat by mutableStateOf(false)
    var crossfade by mutableStateOf(0f)
    var sleepLeft by mutableStateOf(0)
    var status by mutableStateOf("")
    var searchQuery by mutableStateOf("")
    var playCount by mutableStateOf(prefs.getInt("playCount", 0))
    var listenSeconds by mutableStateOf(prefs.getInt("listenSeconds", 0))
    val liked = mutableStateListOf<Song>()
    val history = mutableStateListOf<Song>()
    val feeds = mutableStateMapOf<String, List<Song>>()
    var queue: List<Song> = emptyList()
    val playlists = mutableStateListOf<Playlist>()
    var libraryOpen by mutableStateOf<String?>(null)
    var openPlaylist by mutableStateOf<String?>(null)
    var libTab by mutableStateOf(0)
    var blurRadius by mutableStateOf(48f)

    init {
        liked.addAll(loadSongs("liked"))
        history.addAll(loadSongs("history"))
        loadSongs("known").forEach { known[it.id] = it }
        try {
            val arr = JSONArray(prefs.getString("playlists", "[]") ?: "[]")
            for (i in 0 until arr.length()) playlists.add(arr.getJSONObject(i).toPlaylist())
        } catch (e: Exception) {
        }
        try {
            val obj = JSONObject(prefs.getString("counts", "{}") ?: "{}")
            obj.keys().forEach { counts[it] = obj.getInt(it) }
        } catch (e: Exception) {
        }
        connect()
    }

    private fun connect() {
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val f = MediaController.Builder(ctx, token).buildAsync()
        f.addListener({
            try {
                val c = f.get()
                controller = c
                c.addListener(object : Player.Listener {
                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        playing = playWhenReady
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) onEnded()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        status = "Playback error, tap next"
                        playing = false
                    }
                })
            } catch (e: Exception) {
                status = "Player unavailable"
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    // ---------- storage ----------
    private fun loadSongs(key: String): List<Song> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getJSONObject(it).toSong() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveSongs(key: String, list: List<Song>) {
        prefs.edit().putString(key, JSONArray(list.map { it.toJson() }).toString()).apply()
    }

    private fun saveAll() {
        saveSongs("liked", liked)
        saveSongs("history", history)
        saveSongs("known", known.values.take(300))
        prefs.edit()
            .putString("counts", JSONObject(counts as Map<*, *>).toString())
            .putInt("playCount", playCount)
            .putInt("listenSeconds", listenSeconds)
            .apply()
    }

    // ---------- playlists ----------
    private fun savePlaylists() {
        prefs.edit().putString("playlists", JSONArray(playlists.map { it.toJson() }).toString()).apply()
    }

    fun createPlaylist(name: String): String {
        val id = System.currentTimeMillis().toString()
        playlists.add(Playlist(id, name.trim().ifEmpty { "New playlist" }, emptyList()))
        savePlaylists()
        return id
    }

    private fun updatePlaylist(id: String, change: (Playlist) -> Playlist) {
        val i = playlists.indexOfFirst { it.id == id }
        if (i < 0) return
        playlists[i] = change(playlists[i])
        savePlaylists()
    }

    fun renamePlaylist(id: String, name: String) =
        updatePlaylist(id) { it.copy(name = name.trim().ifEmpty { it.name }) }

    fun deletePlaylist(id: String) {
        playlists.removeAll { it.id == id }
        savePlaylists()
    }

    fun addToPlaylist(id: String, song: Song) {
        known[song.id] = song
        updatePlaylist(id) { p -> if (p.songs.any { it.id == song.id }) p else p.copy(songs = p.songs + song) }
    }

    fun removeFromPlaylist(id: String, songId: String) =
        updatePlaylist(id) { p -> p.copy(songs = p.songs.filter { it.id != songId }) }

    fun moveInPlaylist(id: String, index: Int, delta: Int) = updatePlaylist(id) { p ->
        val to = index + delta
        if (index !in p.songs.indices || to !in p.songs.indices) {
            p
        } else {
            val list = p.songs.toMutableList()
            val item = list.removeAt(index)
            list.add(to, item)
            p.copy(songs = list)
        }
    }

    // ---------- library ----------
    fun glassAlpha(): Float = if (batterySaver) 1f else glass

    fun isLiked(song: Song): Boolean = liked.any { it.id == song.id }

    fun toggleLike(song: Song) {
        if (isLiked(song)) liked.removeAll { it.id == song.id } else liked.add(0, song)
        known[song.id] = song
        saveAll()
    }

    fun topPlayed(): List<Song> =
        counts.entries.sortedByDescending { it.value }.take(50).mapNotNull { known[it.key] }

    fun loadFeed(key: String, query: String) {
        if (feeds.containsKey(key) || !loadingFeeds.add(key)) return
        scope.launch {
            try {
                feeds[key] = Engine.search(query)
            } catch (e: Exception) {
                status = "No connection to YouTube Music"
            }
            loadingFeeds.remove(key)
        }
    }

    // ---------- playback ----------
    fun play(song: Song, from: List<Song> = listOf(song)) {
        current = song
        queue = from
        position = 0f
        playing = true
        status = "Loading..."
        playCount++
        known[song.id] = song
        counts[song.id] = (counts[song.id] ?: 0) + 1
        history.removeAll { it.id == song.id }
        history.add(0, song)
        while (history.size > 30) history.removeAt(history.size - 1)
        saveAll()
        scope.launch {
            try {
                val url = Engine.streamUrl(song.url)
                var c = controller
                var tries = 0
                while (c == null && tries < 50) {
                    delay(100)
                    c = controller
                    tries++
                }
                if (c == null) {
                    status = "Player unavailable"
                    return@launch
                }
                if (current?.id != song.id) return@launch
                val meta = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setArtworkUri(song.thumb?.let { Uri.parse(it) })
                    .build()
                c.setMediaItem(MediaItem.Builder().setUri(url).setMediaId(song.id).setMediaMetadata(meta).build())
                c.prepare()
                c.play()
                status = ""
            } catch (e: Exception) {
                status = "Can't play this one: " + (e.message ?: "error").take(50)
                playing = false
            }
        }
    }

    fun toggle() {
        val c = controller ?: return
        if (current == null) return
        if (c.playWhenReady) c.pause() else c.play()
    }

    fun seekTo(sec: Float) {
        position = sec
        controller?.seekTo((sec * 1000).toLong())
    }

    fun next() {
        val cur = current ?: return
        if (queue.isEmpty()) return
        val i = queue.indexOfFirst { it.id == cur.id }
        val n = if (shuffle) queue.random() else queue[(i + 1) % queue.size]
        play(n, queue)
    }

    fun prev() {
        val cur = current ?: return
        if (position > 3f) {
            seekTo(0f)
            return
        }
        if (queue.isEmpty()) return
        val i = queue.indexOfFirst { it.id == cur.id }
        play(queue[(i - 1 + queue.size) % queue.size], queue)
    }

    private fun onEnded() {
        if (repeat) {
            controller?.seekTo(0)
            controller?.play()
        } else {
            next()
        }
    }

    fun tick() {
        val c = controller
        if (c != null && playing && c.playbackState == Player.STATE_READY) {
            position = c.currentPosition / 1000f
            listenSeconds++
            ticks++
            if (ticks % 30 == 0) saveAll()
        }
        if (sleepLeft > 0) {
            sleepLeft--
            if (sleepLeft == 0) c?.pause()
        }
    }
}
