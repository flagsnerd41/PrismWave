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
    private var earlyId: String? = null
    private var failStreak = 0
    private var fadeJob: Job? = null
    private var preloadJob: Job? = null
    private val planned = ArrayList<Song>()
    private var pausedByUnplug = false
    private var pausedAt = 0L
    private var fading = false
    private var queuedId: String? = null
    private var warmingFor: String? = null
    private var autoAdvance = false
    var radio = false
    var coverPool: List<Song> = emptyList()
    private var warmedFor: String? = null
    // listening time per day (yyyyMMdd to seconds) and every play with its time, for the stats ranges
    val dayLog = HashMap<String, Int>()
    private val playLog = ArrayList<Pair<Long, String>>()

    var screen by mutableStateOf(Screen.Home)
    var current by mutableStateOf<Song?>(null)
    var playing by mutableStateOf(false)
    var position by mutableStateOf(0f)
    var showPlayer by mutableStateOf(false)
    var glass by mutableStateOf(prefs.getFloat("glass", 0.75f))
    var batterySaver by mutableStateOf(prefs.getBoolean("batterySaver", false))
    var accent by mutableStateOf(Color(prefs.getInt("accent", PrismBlue.toArgb())).let { if (it.toArgb() == OldGold.toArgb()) PrismBlue else it })
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
    val ytHome = mutableStateListOf<YtShelf>()
    private var ytHomeLoaded = false
    private var picksLoaded = false
    var queue: List<Song> = emptyList()
    val playlists = mutableStateListOf<Playlist>()
    var libraryOpen by mutableStateOf<String?>(null)
    var openPlaylist by mutableStateOf<String?>(null)
    var libTab by mutableStateOf(0)
    var blurRadius by mutableStateOf(prefs.getFloat("blur", 80f))
    var blurOn by mutableStateOf(prefs.getBoolean("blurOn", true))
    var reduceMotion by mutableStateOf(prefs.getBoolean("reduceMotion", false))
    var playerStyle by mutableStateOf(prefs.getInt("playerStyle", 1))
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
        loadStats()
        connect()
        watchHeadphones()
        if (signedIn && AccountPrefs.autoSync && System.currentTimeMillis() - prefs.getLong("lastImport", 0L) > 6L * 3600 * 1000) {
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

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                            syncFromPlayer()
                            return
                        }
                        val sg = mediaItem?.mediaId?.let { id -> known[id] ?: queue.firstOrNull { q -> q.id == id } } ?: return
                        if (sg.id == current?.id) return
                        onAutoAdvance(sg)
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
            val dl = dayLogJson()
            val pl = playLogJson()
            withContext(Dispatchers.Default) {
                prefs.edit()
                    .putString("liked", JSONArray(likedSnap.map { it.toJson() }).toString())
                    .putString("history", JSONArray(histSnap.map { it.toJson() }).toString())
                    .putString("known", JSONArray(knownSnap.map { it.toJson() }).toString())
                    .putString("counts", JSONObject(countsSnap as Map<*, *>).toString())
                    .putInt("playCount", pc)
                    .putInt("listenSeconds", ls)
                    .putString("dayLog", dl)
                    .putString("playLog", pl)
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

    fun replaceQueue(l: List<Song>) {
        queue = l
        planned.clear()
        refreshQueued()
    }

    fun playNext(song: Song) {
        if (current == null) {
            play(song)
            return
        }
        val l = queue.filter { it.id != song.id }.toMutableList()
        val i = l.indexOfFirst { it.id == current?.id }
        l.add(i + 1, song)
        replaceQueue(l)
    }

    fun addToQueue(song: Song) {
        if (current == null) {
            play(song)
            return
        }
        replaceQueue(queue.filter { it.id != song.id } + song)
    }

    // ---------- listening stats ----------
    private fun dayKey(ms: Long): String =
        java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date(ms))

    private fun loadStats() {
        try {
            val o = JSONObject(prefs.getString("dayLog", "{}") ?: "{}")
            o.keys().forEach { dayLog[it] = o.getInt(it) }
        } catch (e: Exception) {
        }
        try {
            val a = JSONArray(prefs.getString("playLog", "[]") ?: "[]")
            for (i in 0 until a.length()) {
                val r = a.getJSONArray(i)
                playLog.add(Pair(r.getLong(0), r.getString(1)))
            }
        } catch (e: Exception) {
        }
    }

    private fun dayLogJson(): String {
        val cut = dayKey(System.currentTimeMillis() - 400L * 86400000L)
        dayLog.keys.removeAll { it < cut }
        return JSONObject(HashMap(dayLog) as Map<*, *>).toString()
    }

    private fun playLogJson(): String {
        val a = JSONArray()
        for (p in playLog) a.put(JSONArray().put(p.first).put(p.second))
        return a.toString()
    }

    private fun logPlay(id: String) {
        playLog.add(Pair(System.currentTimeMillis(), id))
        while (playLog.size > 3000) playLog.removeAt(0)
    }

    // days = 0 means all time
    fun statSeconds(days: Int): Int {
        if (days == 0) return listenSeconds
        var sum = 0
        val now = System.currentTimeMillis()
        for (i in 0 until days) sum += dayLog[dayKey(now - i * 86400000L)] ?: 0
        return sum
    }

    fun statCounts(days: Int): Map<String, Int> {
        if (days == 0) return HashMap(counts)
        val cut = System.currentTimeMillis() - days * 86400000L
        val m = HashMap<String, Int>()
        for (p in playLog) if (p.first >= cut) m[p.second] = (m[p.second] ?: 0) + 1
        return m
    }

    fun songOf(id: String): Song? = known[id]

    fun topArtists(): List<Pair<String, Int>> =
        counts.entries.mapNotNull { e -> known[e.key]?.let { it.artist to e.value } }
            .groupBy({ it.first }, { it.second })
            .map { it.key to it.value.sum() }
            .sortedByDescending { it.second }
            .take(5)

    fun topPlayed(): List<Song> =
        counts.entries.sortedByDescending { it.value }.take(50).mapNotNull { known[it.key] }

    fun reloadYtHome() {
        ytHomeLoaded = false
        loadYtHome()
    }

    // the real YouTube Music Home of the signed-in account
    fun loadYtHome() {
        if (!signedIn || ytHomeLoaded) return
        ytHomeLoaded = true
        scope.launch {
            try {
                val shelves = YtAccount.home(cookie)
                ytHome.clear()
                ytHome.addAll(shelves)
            } catch (e: Exception) {
                ytHomeLoaded = false
            }
        }
    }

    // picks based on the songs you play most (used when you are not signed in)
    fun loadPicks() {
        if (picksLoaded || Opts.offline) return
        val seeds = (topPlayed().take(3) + history.take(3)).distinctBy { it.id }.take(4)
        if (seeds.isEmpty()) return
        picksLoaded = true
        scope.launch {
            try {
                val all = ArrayList<Song>()
                for (sd in seeds) all.addAll(Engine.related(sd.url).take(12))
                val seen = HashSet<String>()
                val mixed = all.filter { it.id !in counts && seen.add(it.id) }.shuffled().take(20)
                if (mixed.size >= 4) feeds["picks"] = mixed
            } catch (e: Exception) {
                picksLoaded = false
            }
        }
    }

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
        MuffleMix.clear()
        XFade.stop()
        fading = false
        queuedId = null
        warmingFor = null
        pausedByUnplug = false
        current = song
        queue = from
        // one song alone (from search for example): the songs after it come from YouTube Music's radio for this song
        if (from.size == 1 && from[0].id == song.id) radio = true
        position = 0f
        playing = true
        status = "Loading..."
        loading = true
        switchCount++
        endedFor = null
        playCount++
        known[song.id] = song
        counts[song.id] = (counts[song.id] ?: 0) + 1
        logPlay(song.id)
        history.removeAll { it.id == song.id }
        history.add(0, song)
        while (history.size > 30) history.removeAt(history.size - 1)
        saveAll()
        scope.launch {
            try {
                val url = DownloadStore.localUri(song)
                    ?: SongCache.uri(ctx, song.id) ?: Engine.streamUrl(song.url)
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
                val soft = false
                autoAdvance = false
                // a skip during the end fade must not leave the new song silent
                AudioShape.rampGain(1f, 1f, 1L)
                if (soft) c.volume = 0f
                c.play()
                if (soft) fade(0f, 1f, if (Opts.crossfade > 0) Opts.crossfade * 1000L else 450L) else c.volume = 1f
                status = ""
                preload()
                queueNextItem()
                cacheCurrent(song, url)
                scope.launch { AutoMix.analyze(ctx, song.id) }
            } catch (e: Exception) {
                status = "Can't play this one: " + (e.message ?: "error").take(50)
                loading = false
                skipBroken()
            } finally {
                if (switchCount > 0) switchCount--
            }
        }
    }

    // songs being switched by play() right now: the player still holds the old song for a moment, so do not sync
    private var switchCount = 0

    // The UI always follows the song that is really playing (also after the app was in the background).
    fun syncFromPlayer() {
        val c = controller ?: return
        if (switchCount > 0) return
        val id = c.currentMediaItem?.mediaId ?: return
        if (id != current?.id) {
            val sg = known[id] ?: queue.firstOrNull { it.id == id } ?: return
            onAutoAdvance(sg)
        }
        if (fadeJob?.isActive != true && c.playbackState != Player.STATE_IDLE && playing != c.playWhenReady) {
            playing = c.playWhenReady
        }
    }

    fun toggle() {
        val c = controller ?: return
        if (current == null) return
        if (playing) {
            XFade.stop()
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
        if (fading) {
            XFade.stop()
            AudioShape.rampGain(1f, 1f, 1L)
            fading = false
            fadeJob?.cancel()
            controller?.volume = 1f
            MuffleMix.clear()
        }
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
                    replaceQueue(queue + more)
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
        if (!Opts.offline) {
            scope.launch {
                try {
                    Engine.streamUrl(n.url)
                } catch (e: Exception) {
                }
            }
        }
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
                if (DownloadStore.localUri(sg) != null || SongCache.has(ctx, sg.id)) continue
                try {
                    val url = Engine.streamUrl(sg.url)
                    if (SongCache.fetch(ctx, sg.id, url)) {
                        AutoMix.analyze(ctx, sg.id)
                        if (queuedId == sg.id) refreshQueued()
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

    // the song you are listening to is saved too, so it replays instantly and works offline
    private fun cacheCurrent(song: Song, url: String) {
        if (!url.startsWith("http") || Opts.offline) return
        scope.launch {
            delay(1500)
            try {
                SongCache.fetch(ctx, song.id, url)
                AutoMix.analyze(ctx, song.id)
            } catch (e: Exception) {
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

    // The next song is added to the player's own list while the current one plays,
    // so Android never sees the audio stop between songs (this is what muted it in the background).
    private fun queueNextItem() {
        val c = controller ?: return
        val cur = current ?: return
        if (repeat) return
        if (radio && !shuffle && queue.lastOrNull()?.id == cur.id) {
            scope.launch {
                try {
                    val more = Engine.related(cur.url).filter { r -> queue.none { q -> q.id == r.id } }
                    if (more.isNotEmpty() && current?.id == cur.id) replaceQueue(queue + more)
                } catch (e: Exception) {
                }
            }
            return
        }
        val nxt = upcoming().firstOrNull() ?: return
        if (queuedId == nxt.id) return
        scope.launch {
            try {
                val url = DownloadStore.localUri(nxt)
                    ?: SongCache.uri(ctx, nxt.id) ?: Engine.streamUrl(nxt.url)
                if (current?.id != cur.id || upcoming().firstOrNull()?.id != nxt.id) return@launch
                while (c.mediaItemCount > c.currentMediaItemIndex + 1) c.removeMediaItem(c.mediaItemCount - 1)
                val meta = MediaMetadata.Builder()
                    .setTitle(nxt.title)
                    .setArtist(nxt.artist)
                    .setArtworkUri(nxt.thumb?.let { Uri.parse(artUrl(it, 800)) })
                    .build()
                c.addMediaItem(MediaItem.Builder().setUri(url).setMediaId(nxt.id).setCustomCacheKey(nxt.id).setMediaMetadata(meta).build())
                queuedId = nxt.id
            } catch (e: Exception) {
            }
        }
    }

    private fun refreshQueued() {
        val c = controller ?: return
        if (current == null) return
        try {
            while (c.mediaItemCount > c.currentMediaItemIndex + 1) c.removeMediaItem(c.mediaItemCount - 1)
        } catch (e: Exception) {
        }
        queuedId = null
        queueNextItem()
    }

    private fun onAutoAdvance(sg: Song) {
        val prevLead = if (AutoMix.on && fading) AutoMix.leadFor(current?.id ?: "") else null
        planned.removeAll { it.id == sg.id }
        fading = false
        current = sg
        position = 0f
        status = ""
        loading = false
        endedFor = null
        retriedId = null
        queuedId = null
        warmingFor = null
        // a helper player may only continue if it plays exactly this song, otherwise two songs play at once
        if ((XFade.active && XFade.songId != sg.id) || (!XFade.active && XFade.preparedId != null)) XFade.stop()
        playCount++
        known[sg.id] = sg
        counts[sg.id] = (counts[sg.id] ?: 0) + 1
        logPlay(sg.id)
        history.removeAll { it.id == sg.id }
        history.add(0, sg)
        while (history.size > 30) history.removeAt(history.size - 1)
        saveAll()
        if (MuffleMix.outgoing) {
            MuffleMix.outgoing = false
            val ms = ((prevLead ?: StudioPrefs.mixSecs.toFloat()) * 1000f).toLong()
            MuffleMix.ramp(StudioPrefs.mixStrength, 0f, ms)
            fade(0.3f, 1f, ms)
        } else if (XFade.active && XFade.songId == sg.id) {
            takeOverFromOverlap()
        } else if (Opts.crossfade > 0 || prevLead != null) {
            AudioShape.rampGain(0f, 1f, ((prevLead ?: Opts.crossfade.toFloat()) * 1000f).toLong())
        } else {
            fadeJob?.cancel()
            controller?.volume = 1f
        }
        preload()
        queueNextItem()
        if (!SongCache.has(ctx, sg.id) && !Opts.offline) {
            scope.launch {
                try {
                    SongCache.fetch(ctx, sg.id, Engine.streamUrl(sg.url))
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun onEnded() {
        val cur = current ?: return
        val id = cur.id
        if (endedFor == id) return
        endedFor = id
        if (!repeat && earlyId != id && cur.seconds > 30 && position < cur.seconds - 10f) {
            // stopped far too early (broken file or stream): drop the cached copy and continue from a fresh link
            earlyId = id
            endedFor = null
            retriedId = null
            handleError()
            return
        }
        if (repeat) {
            XFade.stop()
            endedFor = null
            fading = false
            fadeJob?.cancel()
            controller?.volume = 1f
            AudioShape.reset()
            controller?.seekTo(0)
            controller?.play()
        } else {
            autoAdvance = true
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
        SongCache.remove(ctx, cur.id)
        Engine.forget(cur.url)
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
                queuedId = null
                queueNextItem()
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

    fun cachedSongs(): List<Song> = SongCache.keys(ctx).mapNotNull { known[it] }

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

    // 30 seconds before a song ends: load the next cover and a few random ones
    private fun warmCovers() {
        val cur = current ?: return
        val next = upcoming().firstOrNull()
        val pool = (queue + history.toList() + liked.toList())
            .filter { it.id != cur.id && it.id != next?.id && it.thumb != null }
            .distinctBy { it.id }
            .shuffled()
            .take(12)
        coverPool = pool
        val q = if (playerStyle == 1) 1200 else 1000
        CoverPreload.warm(ctx, listOfNotNull(next) + pool, q)
    }

    // the next song starts on a second player while the end of this one fades out, so they really overlap
    // the id of the song the main player will really play next
    private fun playerNextId(): String? {
        val c = controller ?: return null
        val i = c.currentMediaItemIndex
        return if (i >= 0 && i + 1 < c.mediaItemCount) c.getMediaItemAt(i + 1).mediaId else null
    }

    private fun startOverlap(leftSec: Float) {
        val nid = playerNextId() ?: return
        val nxt = queue.firstOrNull { it.id == nid } ?: known[nid] ?: return
        scope.launch {
            try {
                val url = DownloadStore.localUri(nxt) ?: SongCache.uri(ctx, nxt.id) ?: Engine.streamUrl(nxt.url)
                if (!fading || current == null) return@launch
                if (playerNextId() != nid) {
                    XFade.stop()
                    return@launch
                }
                XFade.begin(ctx, nxt.id, url, (leftSec * 1000).toLong() - 150L)
            } catch (e: Exception) {
                XFade.stop()
            }
        }
    }

    // a few seconds before the crossfade: load the next song silently on the helper player
    private fun warmOverlap() {
        val nid = playerNextId() ?: return
        if (warmingFor == nid || XFade.preparedId == nid || XFade.active) return
        val nxt = queue.firstOrNull { it.id == nid } ?: known[nid] ?: return
        warmingFor = nid
        scope.launch {
            try {
                val url = DownloadStore.localUri(nxt) ?: SongCache.uri(ctx, nxt.id) ?: Engine.streamUrl(nxt.url)
                if (current == null || playerNextId() != nid) {
                    warmingFor = null
                    return@launch
                }
                XFade.prepare(ctx, nid, url)
            } catch (e: Exception) {
                warmingFor = null
            }
        }
    }

    // the first song has ended: the main player continues the next song at the spot the helper player reached
    private fun takeOverFromOverlap() {
        scope.launch {
            try {
                val c = controller ?: return@launch
                // line the main player up with the helper player while it is still silent, retry until both are in sync
                var tries = 0
                while (tries < 4) {
                    c.seekTo(XFade.position() + 120L)
                    var w = 0
                    while (w < 25 && c.playbackState != Player.STATE_READY) {
                        delay(30)
                        w++
                    }
                    delay(40)
                    val off = XFade.position() - c.currentPosition
                    if (kotlin.math.abs(off) <= 45L) break
                    tries++
                }
                // slow, equal handover so there is no jump when the helper disappears
                AudioShape.rampGain(0f, 1f, 450L)
                XFade.fadeOutAndStop(450L)
            } catch (e: Exception) {
                AudioShape.rampGain(0f, 1f, 200L)
                XFade.stop()
            }
        }
    }

    fun tick() {
        val c = controller
        syncFromPlayer()
        // never leave a helper player running when no fade is going on
        if (XFade.active && !fading) XFade.stop()
        if (c != null && current != null && c.playbackState == Player.STATE_ENDED && c.currentMediaItem?.mediaId == current?.id) onEnded()
        if (c != null && playing && c.playbackState == Player.STATE_READY) {
            position = c.currentPosition / 1000f
            val dur = c.duration
            if (dur > 0) fixDuration((dur / 1000).toInt())
            if (!fading && fadeJob?.isActive != true && c.volume < 0.99f) c.volume = 1f
            if (StudioPrefs.mixOn && dur > 0 && !repeat) {
                val leftM = dur / 1000f - position
                val nM = (if (AutoMix.on) AutoMix.leadFor(current?.id ?: "") else null) ?: StudioPrefs.mixSecs.toFloat()
                if (!fading && dur / 1000f > nM * 3 && leftM >= 0.5f && leftM <= nM) {
                    fading = true
                    MuffleMix.outgoing = true
                    MuffleMix.ramp(0f, StudioPrefs.mixStrength, (leftM * 1000).toLong() - 150)
                    fade(c.volume, 0.3f, (leftM * 1000).toLong() - 150)
                } else if (MuffleMix.outgoing && leftM > nM + 1f) {
                    MuffleMix.clear()
                    fading = false
                    fadeJob?.cancel()
                    c.volume = 1f
                }
            }
            if ((Opts.crossfade > 0 || AutoMix.smart(current?.id)) && dur > 0 && !fading && !repeat) {
                val left = dur / 1000f - position
                val cf = (if (AutoMix.on) AutoMix.leadFor(current?.id ?: "") else null) ?: Opts.crossfade.toFloat()
                if (dur / 1000f > cf * 3 && left > cf && left <= cf + 8f) {
                    warmOverlap()
                } else if (left > cf + 10f && XFade.preparedId != null) {
                    XFade.stop()
                    warmingFor = null
                }
                if (dur / 1000f > cf * 3 && left >= 0.5f && left <= cf) {
                    fading = true
                    AudioShape.rampGain(1f, 0f, (left * 1000).toLong() - 150)
                    startOverlap(left)
                }
            }
            if (dur > 0 && !repeat) {
                val left30 = dur / 1000f - position
                val cid = current?.id
                if (cid != null && cid != warmedFor && left30 > 0f && left30 <= 30f) {
                    warmedFor = cid
                    warmCovers()
                }
            }
            listenSeconds++
            val dk = dayKey(System.currentTimeMillis())
            dayLog[dk] = (dayLog[dk] ?: 0) + 1
            ticks++
            if (ticks % 30 == 0) saveAll()
        }
        if (sleepLeft > 0) {
            sleepLeft--
            if (sleepLeft == 0) c?.pause()
        }
    }
}
