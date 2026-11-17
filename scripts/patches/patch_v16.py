import os, re, sys

D = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
names = ["Player.kt", "Screens.kt", "Components.kt", "State.kt", "Engine.kt", "Pages.kt", "PlaybackService.kt"]
files = {n: open(D + n, encoding="utf-8").read() for n in names}


def rep(n, old, new):
    if old not in files[n]:
        sys.exit("MISSING in " + n + ": " + old[:90])
    files[n] = files[n].replace(old, new, 1)


def soft(n, old, new):
    if old not in files[n]:
        print("skipped (already applied?) in " + n + ": " + old[:50])
        return
    files[n] = files[n].replace(old, new, 1)


def sub(n, pattern, new, flags=re.S):
    t, k = re.subn(pattern, lambda m: new, files[n], count=1, flags=flags)
    if k != 1:
        sys.exit("MISSING pattern in " + n + ": " + pattern[:90])
    files[n] = t


def cut(n, start, end, new):
    t = files[n]
    i = t.find(start)
    if i < 0:
        sys.exit("MISSING start in " + n + ": " + start[:90])
    j = len(t) if end is None else t.find(end, i + 1)
    if j < 0:
        sys.exit("MISSING end in " + n + ": " + end[:90])
    files[n] = t[:i] + new + t[j:]


def imports(n, text):
    files[n] = files[n].replace("package com.prismwave.app\n", "package com.prismwave.app\n\n" + text, 1)


# =====================================================================  State.kt
rep("State.kt", "    var crossfade by mutableStateOf(0f)\n", "    var crossfade by mutableStateOf(0f)\n    var loading by mutableStateOf(false)\n")
rep("State.kt", "    private var plJob: Job? = null\n",
    "    private var plJob: Job? = null\n    private var endedFor: String? = null\n    private var retriedId: String? = null\n    private var failStreak = 0\n")
sub("State.kt",
    r'override fun onPlaybackStateChanged\(state: Int\) \{\s*if \(state == Player\.STATE_ENDED\) onEnded\(\)\s*\}\s*override fun onPlayerError\(error: PlaybackException\) \{\s*status = "Playback error, tap next"\s*playing = false\s*\}',
    '''override fun onPlaybackStateChanged(state: Int) {
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
                    }''')
rep("State.kt", '        status = "Loading..."\n        playCount++',
    '        status = "Loading..."\n        loading = true\n        endedFor = null\n        playCount++')
rep("State.kt", "val url = DownloadStore.localUri(song) ?: Engine.streamUrl(song.url)",
    'val url = DownloadStore.localUri(song)\n                    ?: (if (AudioCache.isFull(ctx, song.id)) "https://cached.invalid/" + song.id else Engine.streamUrl(song.url))')
rep("State.kt", "MediaItem.Builder().setUri(url).setMediaId(song.id).setMediaMetadata(meta).build()",
    "MediaItem.Builder().setUri(url).setMediaId(song.id).setCustomCacheKey(song.id).setMediaMetadata(meta).build()")
sub("State.kt", r'status = "Can\'t play this one: " \+ \(e\.message \?: "error"\)\.take\(50\)\s*playing = false',
    'status = "Can\'t play this one: " + (e.message ?: "error").take(50)\n                loading = false\n                skipBroken()')
sub("State.kt", r'private fun onEnded\(\) \{.*?\n    \}\n',
    '''private fun onEnded() {
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
''')
rep("State.kt", "    fun tick() {\n        val c = controller\n",
    "    fun tick() {\n        val c = controller\n        if (c != null && current != null && c.playbackState == Player.STATE_ENDED) onEnded()\n")

# =====================================================================  PlaybackService.kt
imports("PlaybackService.kt",
    "import androidx.media3.datasource.DefaultDataSource\n"
    "import androidx.media3.datasource.DefaultHttpDataSource\n"
    "import androidx.media3.datasource.cache.CacheDataSource\n"
    "import androidx.media3.exoplayer.source.DefaultMediaSourceFactory\n")
rep("PlaybackService.kt", "val player = ExoPlayer.Builder(this)\n",
    '''val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(
                    DefaultDataSource.Factory(
                        this,
                        CacheDataSource.Factory()
                            .setCache(AudioCache.get(this))
                            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
                            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                    )
                )
            )
''')

# =====================================================================  Engine.kt
imports("Engine.kt",
    "import org.schabi.newpipe.extractor.InfoItem\n"
    "import org.schabi.newpipe.extractor.channel.ChannelInfoItem\n"
    "import org.schabi.newpipe.extractor.playlist.PlaylistInfo\n"
    "import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem\n")
rep("Engine.kt", "    suspend fun streamUrl(videoUrl: String): String",
    '''    // kind: 0 = All, 1 = Songs, 2 = Videos, 3 = Albums, 4 = Artists, 5 = Playlists
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

    suspend fun collection(url: String): List<Song> = withContext(Dispatchers.IO) {
        init()
        val info = PlaylistInfo.getInfo(ServiceList.YouTube, url)
        info.relatedItems.filterIsInstance<StreamInfoItem>().map { it.toSong() }
    }

    suspend fun streamUrl(videoUrl: String): String''')
files["Engine.kt"] += '''

// kind: 0 = song or video, 1 = album, 2 = playlist, 3 = artist
data class Hit(
    val kind: Int,
    val title: String,
    val sub: String,
    val thumb: String?,
    val url: String,
    val song: Song? = null
)
'''

# =====================================================================  Pages.kt
rep("Pages.kt", "    var showMixes by mutableStateOf(true)\n",
    "    var showMixes by mutableStateOf(true)\n    var blurAmount by mutableStateOf(1f)\n    var blurDark by mutableStateOf(0.35f)\n    var cacheMb by mutableStateOf(512)\n")
rep("Pages.kt", '        showMixes = p.getBoolean("showMixes", true)\n',
    '        showMixes = p.getBoolean("showMixes", true)\n        blurAmount = p.getFloat("blurAmount", 1f)\n        blurDark = p.getFloat("blurDark", 0.35f)\n        cacheMb = p.getInt("cacheMb", 512)\n')
rep("Pages.kt", '            ?.putBoolean("showMixes", showMixes)\n',
    '            ?.putBoolean("showMixes", showMixes)\n            ?.putFloat("blurAmount", blurAmount)\n            ?.putFloat("blurDark", blurDark)\n            ?.putInt("cacheMb", cacheMb)\n')
rep("Pages.kt", '    var nameInitial by mutableStateOf("")\n',
    '    var nameInitial by mutableStateOf("")\n    var collTitle by mutableStateOf("")\n    var collUrl by mutableStateOf("")\n    var collKind by mutableStateOf(1)\n')
rep("Pages.kt", "    fun confirmName(n: String) {",
    '''    fun openCollection(title: String, url: String, kind: Int) {
        collTitle = title
        collUrl = url
        collKind = kind
        song = null
        onClosed = null
        open = "collection"
    }

    fun confirmName(n: String) {''')
rep("Pages.kt", '            "stats" -> StatsPage(s)\n', '            "stats" -> StatsPage(s)\n            "collection" -> CollectionPage(s)\n')
files["Pages.kt"] += '''

// ------------------------------------------------------------ search: albums, playlists, artists

@Composable
fun HitRow(h: Hit, s: AppState) {
    val fake = Song(h.url, h.title, h.sub, 0, h.url, h.thumb)
    Row(
        Modifier
            .fillMaxWidth()
            .softClick { Pages.openCollection(h.title, h.url, h.kind) }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(fake, Modifier.size(60.dp), if (h.kind == 3) 30.dp else 14.dp, 20.sp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(h.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                (if (h.kind == 1) "Album" else if (h.kind == 2) "Playlist" else "Artist") + (if (h.sub.isNotEmpty() && h.kind != 3) "  •  " + h.sub else ""),
                color = TextDim,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CollectionPage(s: AppState) {
    val url = Pages.collUrl
    val title = Pages.collTitle
    val kind = Pages.collKind
    var songs by remember(url) { mutableStateOf<List<Song>>(emptyList()) }
    var busy by remember(url) { mutableStateOf(true) }
    var err by remember(url) { mutableStateOf("") }
    LaunchedEffect(url) {
        busy = true
        err = ""
        try {
            songs = if (kind == 3) Engine.search(title) else Engine.collection(url)
        } catch (e: Exception) {
            err = "Couldn't load: " + (e.message ?: "error").take(60)
        }
        busy = false
    }
    SlidePage(s, title, scroll = false) {
        LazyColumn(Modifier.weight(1f)) {
            if (busy) item { LoadingRow(s) }
            if (err.isNotEmpty()) item { Text(err, color = TextDim, modifier = Modifier.padding(vertical = 12.dp)) }
            if (songs.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PageButton("Play", s.accent, Modifier.weight(1f)) { s.play(songs[0], songs) }
                        PageButton("Shuffle", s.accent, Modifier.weight(1f)) {
                            s.shuffle = true
                            s.play(songs.random(), songs)
                        }
                    }
                }
            }
            items(songs) { SongRow(it, s, songs) }
        }
    }
}
'''

# =====================================================================  Screens.kt
# search chips
rep("Screens.kt", "    var results by remember { mutableStateOf<List<Song>>(emptyList()) }\n",
    "    var hits by remember { mutableStateOf<List<Hit>>(emptyList()) }\n    var kind by remember { mutableStateOf(0) }\n")
rep("Screens.kt", "LaunchedEffect(s.searchQuery) {", "LaunchedEffect(s.searchQuery, kind) {")
rep("Screens.kt", "            results = emptyList()\n", "            hits = emptyList()\n")
rep("Screens.kt", "results = Engine.search(q)", "hits = Engine.searchHits(q, kind)")
rep("Screens.kt", "            if (busy) item { LoadingRow(s) }\n",
    '''            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(listOf("All", "Songs", "Videos", "Albums", "Artists", "Playlists")) { i, label ->
                        Chip(label, kind == i, s.accent) { kind = i }
                    }
                }
            }
            if (busy) item { LoadingRow(s) }
''')
sub("Screens.kt",
    r'if \(!busy\) \{\s*item \{ SectionTitle\("Results"\) \}.*?items\(results\) \{ SongRow\(it, s, results\) \}\s*\}',
    '''if (!busy) {
                if (hits.isEmpty() && err.isEmpty()) {
                    item { Text("Nothing found", color = TextDim, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }
                }
                val songList = hits.mapNotNull { it.song }
                items(hits) { h ->
                    val sg = h.song
                    if (sg != null) SongRow(sg, s, songList) else HitRow(h, s)
                }
            }''')
# cached list
sub("Screens.kt", r'"Offline" -> DownloadStore\.songs\.toList\(\)\s*else -> emptyList\(\)',
    '"Offline" -> DownloadStore.songs.toList()\n        "Cached" -> s.cachedSongs()\n        else -> emptyList()')
rep("Screens.kt", '"History", "My top 50" -> "Play something first"',
    '"History", "My top 50" -> "Play something first"\n                        "Cached" -> "Songs you play are kept here for instant, offline playback."')
# settings: status bar controls
sub("Screens.kt", r'Section\("Look"\) \{\s*ToggleRow\("Blur under status bar".*?\n',
    '''Section("Status bar") {
                        ToggleRow("Blur under status bar", "Blurred strip on every screen", Opts.statusBlur) { Opts.statusBlur = it; Opts.save() }
                        Text("Blur strength " + (Opts.blurAmount * 100).toInt() + "%", color = TextDim)
                        Slider(
                            value = Opts.blurAmount,
                            onValueChange = { Opts.blurAmount = it },
                            valueRange = 0.2f..2f,
                            onValueChangeFinished = { Opts.save() }
                        )
                        Text("Darkness " + (Opts.blurDark * 100).toInt() + "%", color = TextDim)
                        Slider(
                            value = Opts.blurDark,
                            onValueChange = { Opts.blurDark = it },
                            valueRange = 0f..0.9f,
                            onValueChangeFinished = { Opts.save() }
                        )
                    }
                    Section("Look") {
''')
# settings: cache page
rep("Screens.kt", 'SettingsItem("performance", "Performance", "Blur, motion, and battery saver", Icons.Rounded.Speed)',
    'SettingsItem("performance", "Performance", "Blur, motion, and battery saver", Icons.Rounded.Speed),\n'
    '                    SettingsItem("cache", "Storage and cache", "Cached songs and size limit", Icons.Rounded.Download)')
rep("Screens.kt", '"performance" to "Performance",', '"performance" to "Performance",\n        "cache" to "Storage and cache",')
rep("Screens.kt", '                "performance" -> {',
    '''                "cache" -> {
                    val ctx = LocalContext.current
                    var tick by remember { mutableStateOf(0) }
                    Section("Cached songs") {
                        Text("Using " + AudioCache.usedMb(ctx, tick) + " MB of " + Opts.cacheMb + " MB", color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("Songs you play are saved here for instant and offline playback. Find them in Library, Cached.", color = TextDim, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(256, 512, 1024, 2048).forEach { mb ->
                                Chip(if (mb >= 1024) (mb / 1024).toString() + " GB" else mb.toString() + " MB", Opts.cacheMb == mb, s.accent) {
                                    Opts.cacheMb = mb
                                    Opts.save()
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("The size limit applies after you restart the app.", color = TextDim, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        ActionButton("Clear cache", s.accent) {
                            AudioCache.clear(ctx)
                            tick++
                        }
                    }
                }
                "performance" -> {''')
# changelog + version
rep("Screens.kt", 'Section("v15") { ChangeList(listOf("What\'s new page in Settings")) }',
    '''Section("v16") {
                        ChangeList(listOf(
                            "Search filters: All, Songs, Videos, Albums, Artists, Playlists",
                            "Cached songs: replay fast and offline, list in Library, size in Settings",
                            "Status bar blur strength and darkness controls",
                            "Fix: song ending no longer gets stuck, retries then skips",
                            "Loading ring on the play button",
                            "Animated player icons, mini player slides up with a glow",
                            "Swipe song rows and the mini player, shimmer loading"
                        ))
                    }
                    Section("v15") { ChangeList(listOf("What's new page in Settings")) }''')
rep("Screens.kt", 'Text("Version v15"', 'Text("Version v16"')
# v14 items (skipped automatically when already applied)
imports("Screens.kt", "import androidx.compose.animation.core.animateFloat\n")
t = files["Screens.kt"]
if "CircularProgressIndicator(color = s.accent, modifier = Modifier.size(28.dp))" in t:
    i = t.find("@Composable\nfun LoadingRow(")
    j = t.find("@Composable\nfun QuickCard(")
    files["Screens.kt"] = t[:i] + '''@Composable
fun LoadingRow(s: AppState) {
    val tr = androidx.compose.animation.core.rememberInfiniteTransition(label = "sk")
    val a by tr.animateFloat(
        0.05f, 0.16f,
        androidx.compose.animation.core.infiniteRepeatable(tween(850), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "ska"
    )
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = a)))
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.width(180.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(Color.White.copy(alpha = a)))
                    Box(Modifier.width(110.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = a)))
                }
            }
        }
    }
}

''' + t[j:]
else:
    print("shimmer already applied")

# =====================================================================  Components.kt (swipe rows, from v14)
if "detectHorizontalDragGestures" not in files["Components.kt"]:
    imports("Components.kt",
        "import androidx.compose.foundation.gestures.detectHorizontalDragGestures\nimport android.widget.Toast\n")
    soft("Components.kt", "    val isCurrent = s.current?.id == song.id",
         "    val isCurrent = s.current?.id == song.id\n    val ctx = LocalContext.current")
    soft("Components.kt", "            .softClick { s.play(song, from) }\n            .padding(horizontal = 20.dp, vertical = 8.dp),",
         "            .softClick { s.play(song, from) }\n"
         "            .pointerInput(song.id) {\n"
         "                var dx = 0f\n"
         "                detectHorizontalDragGestures(\n"
         "                    onDragStart = { dx = 0f },\n"
         "                    onDragEnd = {\n"
         "                        if (dx > 140f) {\n"
         "                            s.toggleLike(song)\n"
         "                            Toast.makeText(ctx, \"Like updated\", Toast.LENGTH_SHORT).show()\n"
         "                        } else if (dx < -140f) {\n"
         "                            s.queue = s.queue + song\n"
         "                            Toast.makeText(ctx, \"Added to queue\", Toast.LENGTH_SHORT).show()\n"
         "                        }\n"
         "                    }\n"
         "                ) { _, d -> dx += d }\n"
         "            }\n"
         "            .padding(horizontal = 20.dp, vertical = 8.dp),")

# =====================================================================  Player.kt
imports("Player.kt", "import androidx.compose.material3.CircularProgressIndicator\n")
soft("Player.kt", ".shadow(22.dp, coverShape)", ".shadow(22.dp, coverShape, ambientColor = art, spotColor = art)")
# top blur controls
rep("Player.kt", "BlurEffect(backdrop.radius, backdrop.radius, TileMode.Clamp)",
    "BlurEffect(backdrop.radius * Opts.blurAmount, backdrop.radius * Opts.blurAmount, TileMode.Clamp)")
rep("Player.kt", "drawRect(Night.copy(alpha = if (useBlur) 0.35f else 0.85f))",
    "drawRect(Night.copy(alpha = if (useBlur) Opts.blurDark else 0.85f))")
# mini player: slide up with glow + swipe + spinner (via PlayButton)
cut("Player.kt", "@Composable\nfun MiniPlayer(", "@Composable\nfun rememberArtColor(",
'''@Composable
fun MiniPlayer(s: AppState) {
    val song = s.current ?: return
    val art = rememberArtColor(song)
    var shown by remember { mutableStateOf(false) }
    val glow = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        shown = true
        delay(350)
        glow.animateTo(0f, tween(1500))
    }
    AnimatedVisibility(
        visible = shown,
        enter = slideInVertically(spring(dampingRatio = 0.75f, stiffness = 260f)) { it } + fadeIn(tween(250))
    ) {
        Glass(
            s.glassAlpha(),
            Modifier
                .fillMaxWidth()
                .shadow((30f * glow.value).dp, RoundedCornerShape(32.dp), false, art, art)
                .softClick { s.showPlayer = true }
                .pointerInput(Unit) {
                    var dx = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dx = 0f },
                        onDragEnd = { if (dx > 120f) s.prev() else if (dx < -120f) s.next() }
                    ) { _, d -> dx += d }
                },
            RoundedCornerShape(32.dp)
        ) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                    val ring = s.accent
                    val total = song.seconds.coerceAtLeast(1)
                    Canvas(Modifier.fillMaxSize()) {
                        val progress = (s.position / total).coerceIn(0f, 1f)
                        drawArc(
                            color = ring,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Cover(song, Modifier.size(46.dp).clip(CircleShape), 23.dp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (s.status.isNotEmpty()) s.status else song.artist,
                        color = TextDim,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                RoundBtn(Icons.Rounded.SkipPrevious, 0.9f, { s.prev() }, 42.dp)
                Spacer(Modifier.width(6.dp))
                PlayButton(s, 52.dp, 30.dp)
                Spacer(Modifier.width(6.dp))
                RoundBtn(Icons.Rounded.SkipNext, 0.9f, { s.next() }, 42.dp)
            }
        }
    }
}

''')
# alive icons: toggles pop and fade color
cut("Player.kt", "@Composable\nprivate fun PlayerIcon(", "@Composable\nprivate fun VolumeRow(",
'''@Composable
private fun PlayerIcon(icon: ImageVector, tint: Color, size: Dp = 26.dp, box: Dp = 48.dp, onClick: () -> Unit) {
    val c by animateColorAsState(tint, tween(250), label = "pit")
    val pop = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(tint) {
        if (first) {
            first = false
        } else {
            pop.animateTo(1.3f, tween(100))
            pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 350f))
        }
    }
    Box(Modifier.bounceClick(onClick).size(box), contentAlignment = Alignment.Center) {
        Icon(
            icon,
            contentDescription = null,
            tint = c,
            modifier = Modifier.size(size).graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
        )
    }
}

''')
# heart pop
rep("Player.kt", "        val liked = s.isLiked(song)\n        val bgMod",
    "        val liked = s.isLiked(song)\n"
    "        val likePop = remember { Animatable(1f) }\n"
    "        LaunchedEffect(liked) {\n"
    "            if (liked) {\n"
    "                likePop.animateTo(1.4f, tween(110))\n"
    "                likePop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f))\n"
    "            }\n"
    "        }\n"
    "        val bgMod")
rep("Player.kt", "Modifier.bounceClick { s.toggleLike(song) }.size(40.dp).then(bgMod),",
    "Modifier.bounceClick { s.toggleLike(song) }.size(40.dp).graphicsLayer {\n"
    "                scaleX = likePop.value\n"
    "                scaleY = likePop.value\n"
    "            }.then(bgMod),")
# transport row: skip nudge + loading ring
cut("Player.kt", "@Composable\nprivate fun TransportRow(", "@Composable\nprivate fun AlbumBody(",
'''@Composable
private fun NudgeIcon(icon: ImageVector, dir: Float, size: Dp, box: Dp, onClick: () -> Unit) {
    val nudge = remember { Animatable(0f) }
    val sc = rememberCoroutineScope()
    Box(
        Modifier
            .bounceClick {
                sc.launch {
                    nudge.animateTo(dir * 18f, tween(90))
                    nudge.animateTo(0f, spring(dampingRatio = 0.35f, stiffness = 400f))
                }
                onClick()
            }
            .size(box)
            .graphicsLayer { translationX = nudge.value },
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size)) }
}

@Composable
private fun TransportRow(s: AppState, side: Dp, big: Dp, skipIcons: Boolean = false) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NudgeIcon(if (skipIcons) Icons.Rounded.SkipPrevious else Icons.Rounded.FastRewind, -1f, side, side + 28.dp) { s.prev() }
        Box(Modifier.bounceClick { s.toggle() }.size(big + 18.dp), contentAlignment = Alignment.Center) {
            if (s.loading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 4.dp, modifier = Modifier.size(big))
            } else {
                AnimatedContent(
                    targetState = s.playing,
                    transitionSpec = {
                        (fadeIn(tween(160)) + scaleIn(initialScale = 0.5f)) togetherWith
                            (fadeOut(tween(100)) + scaleOut(targetScale = 0.5f))
                    },
                    label = "bigplay"
                ) { p ->
                    Icon(
                        if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(big)
                    )
                }
            }
        }
        NudgeIcon(if (skipIcons) Icons.Rounded.SkipNext else Icons.Rounded.FastForward, 1f, side, side + 28.dp) { s.next() }
    }
}

''')
# small play button: loading ring
cut("Player.kt", "@Composable\nfun PlayButton(", "// ------------------------------------------------------------ blurred strip under the status bar",
'''@Composable
fun PlayButton(s: AppState, size: Dp, iconSize: Dp) {
    val corner by animateDpAsState(
        if (s.playing) size / 3 else size / 2,
        spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "morph"
    )
    Box(
        Modifier
            .bounceClick { s.toggle() }
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(s.accent),
        contentAlignment = Alignment.Center
    ) {
        if (s.loading) {
            CircularProgressIndicator(color = Color.Black, strokeWidth = 3.dp, modifier = Modifier.size(iconSize))
        } else {
            AnimatedContent(
                targetState = s.playing,
                transitionSpec = {
                    (fadeIn(tween(160)) + scaleIn(initialScale = 0.5f)) togetherWith
                        (fadeOut(tween(100)) + scaleOut(targetScale = 0.5f))
                },
                label = "playicon"
            ) { playing ->
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}


''')

for n in names:
    open(D + n, "w", encoding="utf-8").write(files[n])
print("v16 patch applied")
