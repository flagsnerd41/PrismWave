import os, re, sys

D = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
names = ["Player.kt", "Screens.kt", "State.kt", "Pages.kt", "Cache.kt"]
files = {n: open(D + n, encoding="utf-8").read() for n in names}


def rep(n, old, new):
    if old not in files[n]:
        sys.exit("MISSING in " + n + ": " + old[:90])
    files[n] = files[n].replace(old, new, 1)


def sub(n, pattern, new):
    t, k = re.subn(pattern, lambda m: new, files[n], count=1, flags=re.S)
    if k != 1:
        sys.exit("MISSING pattern in " + n + ": " + pattern[:90])
    files[n] = t


def imports(n, text):
    files[n] = files[n].replace("package com.prismwave.app\n", "package com.prismwave.app\n\n" + text, 1)


# ===================================================== Pages.kt: options, solid dialog, swipeable card
rep("Pages.kt", "    var cacheMb by mutableStateOf(512)\n",
    "    var cacheMb by mutableStateOf(512)\n    var preload by mutableStateOf(true)\n    var fade by mutableStateOf(true)\n    var offline by mutableStateOf(false)\n    var headphones by mutableStateOf(true)\n")
rep("Pages.kt", '        cacheMb = p.getInt("cacheMb", 512)\n',
    '        cacheMb = p.getInt("cacheMb", 512)\n        preload = p.getBoolean("preload", true)\n        fade = p.getBoolean("fade", true)\n        offline = p.getBoolean("offline", false)\n        headphones = p.getBoolean("headphones", true)\n')
rep("Pages.kt", '            ?.putInt("cacheMb", cacheMb)\n',
    '            ?.putInt("cacheMb", cacheMb)\n            ?.putBoolean("preload", preload)\n            ?.putBoolean("fade", fade)\n            ?.putBoolean("offline", offline)\n            ?.putBoolean("headphones", headphones)\n')
sub("Pages.kt", r'containerColor = CardBg2,(\s*)shape = RoundedCornerShape\(32\.dp\)',
    'containerColor = Color(0xFF241F1F),\n        shape = RoundedCornerShape(32.dp)')
sub("Pages.kt", r'fun FeaturedCard\(song: Song, from: List<Song>, s: AppState\) \{',
    'fun FeaturedCard(song: Song, from: List<Song>, s: AppState, modifier: Modifier = Modifier) {')
sub("Pages.kt", r'Box\(\s*Modifier\s*\.padding\(horizontal = 20\.dp\)\s*\.fillMaxWidth\(\)\s*\.height\(330\.dp\)',
    'Box(\n        modifier\n            .height(330.dp)')

# ===================================================== Cache.kt: warm the next songs
imports("Cache.kt",
    "import androidx.media3.datasource.DataSpec\n"
    "import androidx.media3.datasource.DefaultHttpDataSource\n"
    "import androidx.media3.datasource.cache.CacheDataSource\n"
    "import androidx.media3.datasource.cache.CacheWriter\n"
    "import kotlinx.coroutines.Dispatchers\n"
    "import kotlinx.coroutines.withContext\n")
rep("Cache.kt", "    fun usedMb(",
    '''    suspend fun warm(ctx: Context, key: String, url: String) = withContext(Dispatchers.IO) {
        val factory = CacheDataSource.Factory()
            .setCache(get(ctx))
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
        val spec = DataSpec.Builder().setUri(url).setKey(key).build()
        CacheWriter(factory.createDataSource(), spec, null, null).cache()
    }

    fun usedMb(''')

# ===================================================== State.kt
imports("State.kt",
    "import android.media.AudioDeviceCallback\nimport android.media.AudioDeviceInfo\nimport android.media.AudioManager\n")
# 1) the multi-skip bug: only react when the player holds the current song
rep("State.kt", "if (c != null && current != null && c.playbackState == Player.STATE_ENDED) onEnded()",
    "if (c != null && current != null && c.playbackState == Player.STATE_ENDED && c.currentMediaItem?.mediaId == current?.id) onEnded()")
rep("State.kt", "    private var failStreak = 0\n",
    "    private var failStreak = 0\n    private var fadeJob: Job? = null\n    private var preloadJob: Job? = null\n    private val planned = ArrayList<Song>()\n    private var pausedByUnplug = false\n    private var pausedAt = 0L\n")
rep("State.kt", "        connect()\n        if (signedIn", "        connect()\n        watchHeadphones()\n        if (signedIn")
rep("State.kt", "        current = song\n        queue = from\n        position = 0f\n",
    "        if (from !== queue) planned.clear()\n        pausedByUnplug = false\n        current = song\n        queue = from\n        position = 0f\n")
rep("State.kt", '                c.prepare()\n                c.play()\n                status = ""\n',
    '''                c.prepare()
                if (Opts.fade) c.volume = 0f
                c.play()
                if (Opts.fade) fade(0f, 1f, 450) else c.volume = 1f
                status = ""
                preload()
''')
sub("State.kt", r'fun toggle\(\) \{.*?\n    \}\n',
    '''fun toggle() {
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
''')
sub("State.kt", r'fun next\(\) \{.*?\n    \}\n',
    '''fun next() {
        val cur = current ?: return
        if (queue.isEmpty()) return
        val i = queue.indexOfFirst { it.id == cur.id }
        val n = if (shuffle) (planned.firstOrNull { it.id != cur.id } ?: queue.random()) else queue[(i + 1) % queue.size]
        planned.remove(n)
        skipTo(n)
    }
''')
rep("State.kt", "        play(queue[(i - 1 + queue.size) % queue.size], queue)",
    "        skipTo(queue[(i - 1 + queue.size) % queue.size])")
rep("State.kt", "    private fun onEnded() {\n",
    '''    private fun fade(from: Float, to: Float, ms: Long, done: () -> Unit = {}) {
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
''')

# ===================================================== Player.kt: mini player glow redo
imports("Player.kt",
    "import androidx.compose.ui.geometry.Offset\nimport androidx.compose.ui.geometry.CornerRadius\nimport androidx.compose.animation.core.animateFloat\n")
rep("Player.kt", ".shadow((30f * glow.value).dp, RoundedCornerShape(32.dp), false, art, art)",
    '''.drawBehind {
                    val a = maxOf(glow.value * 0.6f, if (s.playing) pulseA else 0f)
                    if (a > 0.01f) {
                        val padX = 36.dp.toPx()
                        val padY = 26.dp.toPx()
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                listOf(art.copy(alpha = a), Color.Transparent),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width * 0.62f
                            ),
                            topLeft = Offset(-padX, -padY),
                            size = Size(size.width + padX * 2, size.height + padY * 2),
                            cornerRadius = CornerRadius(60.dp.toPx())
                        )
                    }
                }''')
rep("Player.kt", "    val glow = remember { Animatable(1f) }\n",
    '''    val glow = remember { Animatable(1f) }
    val pulseT = androidx.compose.animation.core.rememberInfiniteTransition(label = "mpp")
    val pulseA by pulseT.animateFloat(
        0.10f, 0.28f,
        androidx.compose.animation.core.infiniteRepeatable(tween(1600), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "mppa"
    )
''')

# ===================================================== Screens.kt
# offline mode + no network calls while offline
sub("Screens.kt",
    r'LaunchedEffect\(Unit\) \{\s*s\.loadFeed\("quick", "new music hits"\)\s*s\.loadFeed\("chill", "chill songs"\)\s*s\.loadFeed\("workout", "workout songs"\)',
    '''LaunchedEffect(Opts.offline) {
        if (!Opts.offline) {
            s.loadFeed("quick", "new music hits")
            s.loadFeed("chill", "chill songs")
            s.loadFeed("workout", "workout songs")
        }''')
rep("Screens.kt", '    val quick = s.feeds["quick"]\n', '    val quick = if (Opts.offline) s.offlineSongs() else s.feeds["quick"]\n')
rep("Screens.kt", '    val chill = s.feeds["chill"] ?: emptyList()', '    val chill = if (Opts.offline) emptyList<Song>() else (s.feeds["chill"] ?: emptyList())')
rep("Screens.kt", '    val workout = s.feeds["workout"] ?: emptyList()', '    val workout = if (Opts.offline) emptyList<Song>() else (s.feeds["workout"] ?: emptyList())')
sub("Screens.kt", r'items\(chips\) \{ c ->',
    '''item {
                        Chip("Offline", Opts.offline, s.accent) {
                            Opts.offline = !Opts.offline
                            Opts.save()
                        }
                    }
                    items(chips) { c ->''')
rep("Screens.kt", "hits = Engine.searchHits(q, kind)",
    '''hits = if (Opts.offline) {
                s.offlineSongs()
                    .filter { it.title.contains(q, true) || it.artist.contains(q, true) }
                    .map { Hit(0, it.title, it.artist, it.thumb, it.url, it) }
            } else {
                Engine.searchHits(q, kind)
            }''')
# keep listening: 3 swipeable cards at the top
sub("Screens.kt",
    r'if \(history\.isNotEmpty\(\) && Opts\.showKeep\) \{\s*item \{ Spacer\(Modifier\.height\(18\.dp\)\) \}\s*item \{ FeaturedCard\(history\.first\(\), history, s\) \}\s*if \(history\.size > 1\) \{\s*item \{ Spacer\(Modifier\.height\(14\.dp\)\) \}\s*item \{ SongShelf\(history\.drop\(1\), s\) \}\s*\}\s*\}',
    '''if (history.size > 3 && Opts.showKeep) {
                item { Spacer(Modifier.height(18.dp)) }
                item { SectionTitle("Recently played") }
                item { SongShelf(history.drop(3), s) }
            }''')
rep("Screens.kt", '            item { SectionTitle("Quick picks") }',
    '''            if (history.isNotEmpty() && Opts.showKeep) {
                item { Spacer(Modifier.height(14.dp)) }
                item {
                    val cardW = (LocalConfiguration.current.screenWidthDp * 0.86f).dp
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) { items(history.take(3)) { sg -> FeaturedCard(sg, history, s, Modifier.width(cardW)) } }
                }
            }
            item { SectionTitle("Quick picks") }''')
# settings: playback toggles
rep("Screens.kt", 'Section("Status bar") {',
    '''Section("Playback") {
                        ToggleRow("Preload next songs", "Loads the next 2 songs while one plays (uses data)", Opts.preload) { Opts.preload = it; Opts.save() }
                        ToggleRow("Fade in and out", "Soft volume fade on play, pause and skip", Opts.fade) { Opts.fade = it; Opts.save() }
                        ToggleRow("Headphone smarts", "Pause on disconnect, resume when they reconnect", Opts.headphones) { Opts.headphones = it; Opts.save() }
                        ToggleRow("Offline mode", "Only show downloaded and cached songs", Opts.offline) { Opts.offline = it; Opts.save() }
                    }
                    Section("Status bar") {''')
rep("Screens.kt", 'Section("v16") {',
    '''Section("v17") {
                        ChangeList(listOf(
                            "Fix: song ending no longer skips several songs",
                            "Keep listening at the top of Home: 3 swipeable cards",
                            "Preload the next 2 songs while one plays",
                            "Offline mode, fade in and out, headphone smarts",
                            "Solid Tempo and Pitch dialog, new mini player glow"
                        ))
                    }
                    Section("v16") {''')
rep("Screens.kt", 'Text("Version v16"', 'Text("Version v17"')

for n in names:
    open(D + n, "w", encoding="utf-8").write(files[n])
print("v17 patch applied")
