package com.prismwave.app

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import java.io.IOException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
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
    private var saveJob: Job? = null
    private var plJob: Job? = null
    private var endedFor: String? = null
    private var retriedId: String? = null
    private var failStreak = 0
    private var fadeJob: Job? = null
    private var preloadJob: Job? = null
    private val planned = ArrayList<Song>()
    private var pausedByUnplug = false
    private var pausedAt = 0L
    private var fading = false
    var radio = false

    var screen by mutableStateOf(Screen.Home)
    var current by mutableStateOf<Song?>(null)
    var playing by mutableStateOf(false)
    var position by mutableStateOf(0f)
    var showPlayer by mutableStateOf(false)
    var glass by mutableStateOf(prefs.getFloat("glass", 0.75f))
    var batterySaver by mutableStateOf(prefs.getBoolean("batterySaver", false))
    var accent by mutableStateOf(Color(prefs.getInt("accent", Gold.toArgb())))
    var shuffle by mutableStateOf(false)
    var repeat by mutableStateOf(false)
    var crossfade by mutableStateOf(0f)
    var loading by mutableStateOf(false)
    var tempo by mutableStateOf(1f)
    var pitch by mutableStateOf(1f)

    fun applyTempoPitch() {
        controller?.playbackParameters = PlaybackParameters(tempo, pitch)
    }
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
    var blurRadius by mutableStateOf(prefs.getFloat("blur", 80f))
    var blurOn by mutableStateOf(prefs.getBoolean("blurOn", true))
    var reduceMotion by mutableStateOf(prefs.getBoolean("reduceMotion", false))
    var playerStyle by mutableStateOf(prefs.getInt("playerStyle", 0))
    var showLogin by mutableStateOf(false)
    var accountMsg by mutableStateOf("")
    var backupMsg by mutableStateOf("")
    var importing by mutableStateOf(false)
    var cookie by mutableStateOf(prefs.getString("ytCookie", "") ?: "")
    val signedIn: Boolean get() = cookie.contains("SAPISID")

    init {
        DownloadStore.ensure(ctx)
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
        watchHeadphones()
        if (signedIn && System.currentTimeMillis() - prefs.getLong("lastImport", 0L) > 6L * 3600 * 1000) {
            importAccount()
        }
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
                        loading = state == Player.STATE_BUFFERING
                        if (state == Player.STATE_READY) {
                            failStreak = 0
                            retriedId = null
                            if (status.startsWith("Retrying") || status.startsWith("Skipped")) status = ""
                        }
                        if (state == Player.STATE_ENDED) onEnded()
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        loading = false
                        handleError()
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

    // Saves are debounced and the JSON is built off the main thread (this used to cause stutter).
    private fun saveAll() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(1200)
            val likedSnap = liked.toList()
            val histSnap = history.toList()
            val knownSnap = known.values.take(300)
            val countsSnap = HashMap(counts)
            val pc = playCount
            val ls = listenSeconds
            withContext(Dispatchers.Default) {
                prefs.edit()
                    .putString("liked", JSONArray(likedSnap.map { it.toJson() }).toString())
                    .putString("history", JSONArray(histSnap.map { it.toJson() }).toString())
                    .putString("known", JSONArray(knownSnap.map { it.toJson() }).toString())
                    .putString("counts", JSONObject(countsSnap as Map<*, *>).toString())
                    .putInt("playCount", pc)
                    .putInt("listenSeconds", ls)
                    .apply()
            }
        }
    }

    fun saveSettings() {
        prefs.edit()
            .putFloat("glass", glass)
            .putFloat("blur", blurRadius)
            .putBoolean("batterySaver", batterySaver)
            .putBoolean("blurOn", blurOn)
            .putBoolean("reduceMotion", reduceMotion)
            .putInt("playerStyle", playerStyle)
            .putInt("accent", accent.toArgb())
            .apply()
    }

    // ---------- account ----------
    fun onLogin(c: String) {
        val already = signedIn && cookie == c
        cookie = c
        prefs.edit().putString("ytCookie", c).apply()
        showLogin = false
        if (already) return
        // like other apps: playlists appear automatically right after signing in
        screen = Screen.Library
        libTab = 1
        libraryOpen = null
        openPlaylist = null
        importAccount()
    }

    fun signOut() {
        cookie = ""
        prefs.edit().remove("ytCookie").apply()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        accountMsg = "Signed out"
    }

    private fun upsertPlaylist(p: Playlist) {
        val i = playlists.indexOfFirst { it.id == p.id }
        if (i >= 0) playlists[i] = p else playlists.add(p)
        savePlaylists()
    }

    fun importAccount() {
        if (importing || !signedIn) return
        importing = true
        accountMsg = "Importing from YouTube Music..."
        scope.launch {
            try {
                val remoteLiked = YtAccount.songs(cookie, "VLLM")
                var addedLiked = 0
                for (sg in remoteLiked) {
                    known[sg.id] = sg
                    if (!isLiked(sg)) {
                        liked.add(sg)
                        addedLiked++
                    }
                }
                val pls = YtAccount.playlists(cookie).take(40)
                var done = 0
                for (p in pls) {
                    accountMsg = "Importing playlist " + (done + 1) + " of " + pls.size + "..."
                    val songs = YtAccount.songs(cookie, p.browseId)
                    songs.forEach { known[it.id] = it }
                    upsertPlaylist(Playlist("yt_" + p.browseId, p.name, songs))
                    done++
                }
                saveAll()
                prefs.edit().putLong("lastImport", System.currentTimeMillis()).apply()
                accountMsg = if (done == 0 && remoteLiked.isEmpty()) {
                    "Signed in, but nothing was found. YouTube may have changed its format."
                } else {
                    "Done: " + done + " playlists, " + addedLiked + " new liked songs"
                }
            } catch (e: YtAccount.AuthException) {
                accountMsg = "Session expired, sign in again"
            } catch (e: Exception) {
                accountMsg = "Import failed: " + (e.message ?: "error").take(60)
            }
            importing = false
        }
    }

    // ---------- backup ----------
    private fun buildBackup(): String {
        val o = JSONObject()
        o.put("version", 1)
        o.put("liked", JSONArray(liked.map { it.toJson() }))
        o.put("history", JSONArray(history.map { it.toJson() }))
        o.put("playlists", JSONArray(playlists.map { it.toJson() }))
        o.put("known", JSONArray(known.values.map { it.toJson() }))
        o.put("counts", JSONObject(counts as Map<*, *>))
        o.put("playCount", playCount)
        o.put("listenSeconds", listenSeconds)
        return o.toString()
    }

    fun exportTo(uri: Uri) {
        try {
            ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(buildBackup().toByteArray()) }
            backupMsg = "Backup saved (" + liked.size + " liked, " + playlists.size + " playlists)"
        } catch (e: Exception) {
            backupMsg = "Backup failed: " + (e.message ?: "error").take(60)
        }
    }

    fun importFrom(uri: Uri) {
        try {
            val text = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: throw IOException("empty file")
            val o = JSONObject(text)
            fun songs(key: String): List<Song> {
                val a = o.optJSONArray(key) ?: return emptyList()
                return (0 until a.length()).map { a.getJSONObject(it).toSong() }
            }
            liked.clear()
            liked.addAll(songs("liked"))
            history.clear()
            history.addAll(songs("history"))
            known.clear()
            songs("known").forEach { known[it.id] = it }
            playlists.clear()
            val pa = o.optJSONArray("playlists")
            if (pa != null) for (i in 0 until pa.length()) playlists.add(pa.getJSONObject(i).toPlaylist())
            counts.clear()
            o.optJSONObject("counts")?.let { c -> c.keys().forEach { counts[it] = c.getInt(it) } }
            playCount = o.optInt("playCount", playCount)
            listenSeconds = o.optInt("listenSeconds", listenSeconds)
            saveAll()
            savePlaylists()
            backupMsg = "Restored " + liked.size + " liked, " + playlists.size + " playlists"
        } catch (e: Exception) {
            backupMsg = "Restore failed: " + (e.message ?: "bad file").take(60)
        }
    }

    // ---------- playlists ----------
    private fun savePlaylists() {
        plJob?.cancel()
        plJob = scope.launch {
            delay(800)
            val snap = playlists.toList()
            withContext(Dispatchers.Default) {
                prefs.edit().putString("playlists", JSONArray(snap.map { it.toJson() }).toString()).apply()
            }
        }
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

    fun setQueue(l: List<Song>) {
        queue = l
        planned.clear()
    }

    fun playNext(song: Song) {
        if (current == null) {
            play(song)
            return
        }
        val l = queue.filter { it.id != song.id }.toMutableList()
        val i = l.indexOfFirst { it.id == current?.id }
        l.add(i + 1, song)
        setQueue(l)
    }

    fun addToQueue(song: Song) {
        if (current == null) {
            play(song)
            return
        }
        setQueue(queue.filter { it.id != song.id } + song)
    }

    fun topArtists(): List<Pair<String, Int>> =
        counts.entries.mapNotNull { e -> known[e.key]?.let { it.artist to e.value } }
            .groupBy({ it.first }, { it.second })
            .map { it.key to it.value.sum() }
            .sortedByDescending { it.second }
            .take(5)

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
        if (from !== queue) {
            planned.clear()
            radio = false
        }
        fading = false
        pausedByUnplug = false
        current = song
        queue = from
        position = 0f
        playing = true
        status = "Loading..."
        loading = true
        endedFor = null
        playCount++
        known[song.id] = song
        counts[song.id] = (counts[song.id] ?: 0) + 1
        history.removeAll { it.id == song.id }
        history.add(0, song)
        while (history.size > 30) history.removeAt(history.size - 1)
        saveAll()
        scope.launch {
            try {
                val url = DownloadStore.localUri(song)
                    ?: (if (AudioCache.isFull(ctx, song.id)) "https://cached.invalid/" + song.id else Engine.streamUrl(song.url))
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
                    .setArtworkUri(song.thumb?.let { Uri.parse(artUrl(it, 800)) })
                    .build()
                c.setMediaItem(MediaItem.Builder().setUri(url).setMediaId(song.id).setCustomCacheKey(song.id).setMediaMetadata(meta).build())
                c.prepare()
                val soft = Opts.fade || Opts.crossfade > 0
                if (soft) c.volume = 0f
                c.play()
                if (soft) fade(0f, 1f, if (Opts.crossfade > 0) Opts.crossfade * 1000L else 450L) else c.volume = 1f
                status = ""
                preload()
            } catch (e: Exception) {
                status = "Can't play this one: " + (e.message ?: "error").take(50)
                loading = false
                skipBroken()
            }
        }
    }

    fun toggle() {
        val c = controller ?: return
        if (current == null) return
        if (playing) {
            if (Opts.fade && c.playbackState == Player.STATE_READY) {
                playing = false
                fade(c.volume, 0f, 260) {
                    c.pause()
                    c.volume = 1f
                }
            } else {
                c.pause()
            }
        } else {
            pausedByUnplug = false
            if (Opts.fade) {
                c.volume = 0f
                c.play()
                fade(0f, 1f, 320)
            } else {
                c.play()
            }
        }
    }

    fun seekTo(sec: Float) {
        position = sec
        controller?.seekTo((sec * 1000).toLong())
    }

    fun next() {
        val cur = current ?: return
        if (queue.isEmpty()) return
        val i = queue.indexOfFirst { it.id == cur.id }
        if (radio && !shuffle && i == queue.size - 1) {
            scope.launch {
                val more = try {
                    Engine.related(cur.url).filter { r -> queue.none { q -> q.id == r.id } }
                } catch (e: Exception) {
                    emptyList()
                }
                if (more.isNotEmpty()) {
                    setQueue(queue + more)
                    skipTo(more[0])
                } else {
                    skipTo(queue[0])
                }
            }
            return
        }
        val n = if (shuffle) (planned.firstOrNull { it.id != cur.id } ?: queue.random()) else queue[(i + 1) % queue.size]
        planned.remove(n)
        skipTo(n)
    }

    fun prev() {
        val cur = current ?: return
        if (position > 3f) {
            seekTo(0f)
            return
        }
        if (queue.isEmpty()) return
        val i = queue.indexOfFirst { it.id == cur.id }
        skipTo(queue[(i - 1 + queue.size) % queue.size])
    }

    private fun fade(from: Float, to: Float, ms: Long, done: () -> Unit = {}) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 10
            for (k in 1..steps) {
                controller?.volume = from + (to - from) * k / steps
                delay(ms / steps)
            }
            done()
        }
    }

    private fun skipTo(n: Song) {
        val c = controller
        if (Opts.fade && c != null && playing && c.playbackState == Player.STATE_READY) {
            fade(c.volume, 0f, 160) { play(n, queue) }
        } else {
            play(n, queue)
        }
    }

    // the next two songs, so they can be loaded while the current one plays
    private fun upcoming(): List<Song> {
        val cur = current ?: return emptyList()
        if (queue.size < 2) return emptyList()
        if (shuffle) {
            planned.removeAll { it.id == cur.id }
            while (planned.size < 2 && queue.size > planned.size + 1) {
                val r = queue.random()
                if (r.id != cur.id && planned.none { it.id == r.id }) planned.add(r)
            }
            return planned.toList()
        }
        planned.clear()
        val i = queue.indexOfFirst { it.id == cur.id }
        return (1..2).map { queue[(i + it) % queue.size] }.filter { it.id != cur.id }.distinctBy { it.id }
    }

    private fun preload() {
        preloadJob?.cancel()
        if (!Opts.preload || Opts.offline) return
        preloadJob = scope.launch {
            delay(2500)
            for (sg in upcoming()) {
                if (DownloadStore.localUri(sg) != null || AudioCache.isFull(ctx, sg.id)) continue
                try {
                    val url = Engine.streamUrl(sg.url)
                    AudioCache.warm(ctx, sg.id, url)
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun watchHeadphones() {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        fun isHp(d: AudioDeviceInfo) = d.isSink && (
            d.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                d.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                d.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                d.type == AudioDeviceInfo.TYPE_USB_HEADSET
            )
        am.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                if (Opts.headphones && pausedByUnplug &&
                    System.currentTimeMillis() - pausedAt < 10 * 60 * 1000 &&
                    addedDevices.any { isHp(it) }
                ) {
                    pausedByUnplug = false
                    controller?.play()
                }
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                if (Opts.headphones && playing && removedDevices.any { isHp(it) }) {
                    pausedByUnplug = true
                    pausedAt = System.currentTimeMillis()
                    controller?.pause()
                }
            }
        }, null)
    }

    fun offlineSongs(): List<Song> = (DownloadStore.songs.toList() + cachedSongs()).distinctBy { it.id }

    private fun onEnded() {
        val id = current?.id ?: return
        if (endedFor == id) return
        endedFor = id
        if (repeat) {
            endedFor = null
            controller?.seekTo(0)
            controller?.play()
        } else {
            next()
        }
    }

    // One retry with a fresh stream link, then skip to the next song instead of getting stuck.
    private fun handleError() {
        val cur = current ?: return
        if (retriedId == cur.id) {
            skipBroken()
            return
        }
        retriedId = cur.id
        status = "Retrying..."
        AudioCache.remove(ctx, cur.id)
        val resume = (position * 1000).toLong()
        scope.launch {
            try {
                val url = Engine.streamUrl(cur.url)
                val c = controller ?: return@launch
                if (current?.id != cur.id) return@launch
                val meta = MediaMetadata.Builder()
                    .setTitle(cur.title)
                    .setArtist(cur.artist)
                    .setArtworkUri(cur.thumb?.let { Uri.parse(artUrl(it, 800)) })
                    .build()
                c.setMediaItem(
                    MediaItem.Builder().setUri(url).setMediaId(cur.id).setCustomCacheKey(cur.id).setMediaMetadata(meta).build(),
                    resume
                )
                c.prepare()
                c.play()
            } catch (e: Exception) {
                skipBroken()
            }
        }
    }

    private fun skipBroken() {
        failStreak++
        if (failStreak <= 3 && queue.size > 1) {
            next()
            status = "Skipped an unplayable song"
        } else {
            playing = false
            loading = false
            status = "Can't play, check your connection"
        }
    }

    fun cachedSongs(): List<Song> = AudioCache.keys(ctx).mapNotNull { known[it] }

    // Songs imported with a missing length get the real one from the player once it is ready.
    private fun fixDuration(real: Int) {
        val cur = current ?: return
        if (real < 2 || kotlin.math.abs(cur.seconds - real) <= 2) return
        val fixed = cur.copy(seconds = real)
        current = fixed
        queue = queue.map { if (it.id == cur.id) fixed else it }
        known[cur.id] = fixed
        val li = liked.indexOfFirst { it.id == cur.id }
        if (li >= 0) liked[li] = fixed
        val hi = history.indexOfFirst { it.id == cur.id }
        if (hi >= 0) history[hi] = fixed
        for (i in playlists.indices) {
            val p = playlists[i]
            if (p.songs.any { it.id == cur.id }) {
                playlists[i] = p.copy(songs = p.songs.map { if (it.id == cur.id) fixed else it })
            }
        }
        savePlaylists()
        saveAll()
    }

    fun tick() {
        val c = controller
        if (c != null && current != null && c.playbackState == Player.STATE_ENDED && c.currentMediaItem?.mediaId == current?.id) onEnded()
        if (c != null && playing && c.playbackState == Player.STATE_READY) {
            position = c.currentPosition / 1000f
            val dur = c.duration
            if (dur > 0) fixDuration((dur / 1000).toInt())
            if (Opts.crossfade > 0 && dur > 0 && !fading && !repeat) {
                val left = dur / 1000f - position
                val cf = Opts.crossfade.toFloat()
                if (dur / 1000f > cf * 3 && left >= 0.5f && left <= cf) {
                    fading = true
                    fade(c.volume, 0f, (left * 1000).toLong() - 150)
                }
            }
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
