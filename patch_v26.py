import os, sys

ROOT = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
edits = {}

def add(fname, old, new):
    edits.setdefault(fname, []).append((old, new))

# ---------------- Pages.kt: new lyrics page, up/down transition, Discord bridge
add("Pages.kt", '            "lyrics" -> LyricsPage(s)', '            "lyrics" -> LyricsScreen(s)')
add("Pages.kt",
    '    val key = Pages.open\n    var last by remember { mutableStateOf("") }',
    '    DiscordBridge(s)\n    val key = Pages.open\n    var last by remember { mutableStateOf("") }')
add("Pages.kt",
    '''    AnimatedVisibility(
        visible = key != null,
        enter = slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
        exit = slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200))
    ) {''',
    '''    val vert = (key ?: last) == "lyrics"
    AnimatedVisibility(
        visible = key != null,
        enter = if (vert) {
            androidx.compose.animation.slideInVertically(tween(520, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(420))
        } else {
            slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200))
        },
        exit = if (vert) {
            androidx.compose.animation.slideOutVertically(tween(380, easing = FastOutSlowInEasing)) { it / 2 } + fadeOut(tween(300))
        } else {
            slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200))
        }
    ) {''')

# ---------------- Player.kt: the player (cover and text) sinks down while lyrics open
add("Player.kt",
    '''    val dragY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragY.value.roundToInt()) }''',
    '''    val dragY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val lyShift by animateFloatAsState(
        if (Pages.open == "lyrics") 1f else 0f,
        tween(520, easing = FastOutSlowInEasing),
        label = "lyshift"
    )
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = lyShift * size.height * 0.18f
                alpha = 1f - lyShift * 0.9f
            }
            .offset { IntOffset(0, dragY.value.roundToInt()) }''')

# ---------------- PlaybackService.kt: start playing sooner
add("PlaybackService.kt",
    'import androidx.media3.exoplayer.ExoPlayer\n',
    'import androidx.media3.exoplayer.DefaultLoadControl\nimport androidx.media3.exoplayer.ExoPlayer\n')
add("PlaybackService.kt",
    '        val player = ExoPlayer.Builder(this)\n',
    '''        val player = ExoPlayer.Builder(this)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(20000, 60000, 400, 1200)
                    .build()
            )
''')

# ---------------- Engine.kt: remember stream links (about 4 hours) so songs start instantly
add("Engine.kt",
    '    suspend fun streamUrl(videoUrl: String): String = withContext(Dispatchers.IO) {\n        init()\n',
    '''    private val urlCache = HashMap<String, Pair<String, Long>>()

    fun forget(videoUrl: String) {
        synchronized(urlCache) { urlCache.remove(videoUrl) }
    }

    suspend fun streamUrl(videoUrl: String): String = withContext(Dispatchers.IO) {
        val nowMs = System.currentTimeMillis()
        val hit = synchronized(urlCache) { urlCache[videoUrl] }
        if (hit != null && nowMs - hit.second < 4L * 3600 * 1000) return@withContext hit.first
        init()
''')
add("Engine.kt",
    '        best?.content ?: throw IOException("No audio stream found")\n    }',
    '''        val link = best?.content ?: throw IOException("No audio stream found")
        synchronized(urlCache) { urlCache[videoUrl] = Pair(link, nowMs) }
        link
    }''')

# ---------------- State.kt: faster preload, fresh link on retry, warm the link while fading out
add("State.kt",
    '            delay(2500)\n            for (sg in upcoming()) {',
    '            delay(600)\n            for (sg in upcoming()) {')
add("State.kt",
    '        SongCache.remove(ctx, cur.id)\n        val resume',
    '        SongCache.remove(ctx, cur.id)\n        Engine.forget(cur.url)\n        val resume')
add("State.kt",
    '    private fun skipTo(n: Song) {\n        val c = controller\n',
    '''    private fun skipTo(n: Song) {
        val c = controller
        if (!Opts.offline) {
            scope.launch {
                try {
                    Engine.streamUrl(n.url)
                } catch (e: Exception) {
                }
            }
        }
''')

# ---------------- Settings2.kt: Discord option, version, changelog
add("Settings2.kt",
    '"Last.fm, Libre.fm, and ListenBrainz"',
    '"Last.fm, Libre.fm, ListenBrainz and Discord"')
add("Settings2.kt",
    '    Scrobbler.init(ctx.applicationContext)\n',
    '    Scrobbler.init(ctx.applicationContext)\n    DiscordRpc.init(ctx.applicationContext)\n')
add("Settings2.kt",
    '    PfNote("A song is scrobbled after half of it has played (or 4 minutes), and only if it is longer than 30 seconds.")',
    '''    PfLabel("Discord", a)
    PfGroup {
        PfSwitch(
            Icons.Rounded.AutoAwesome, "Discord status",
            if (DiscordRpc.hasToken()) "Shows the song you listen to on your Discord profile"
            else "Sign in to Discord first. Unofficial, so an alt account is safer",
            DiscordRpc.enabled && DiscordRpc.hasToken(), a
        ) { on ->
            if (on && !DiscordRpc.hasToken()) DiscordRpc.loginOpen = true
            else DiscordRpc.setEnabled(ctx, on)
        }
        if (DiscordRpc.hasToken()) {
            PfField("Discord application ID (optional, adds cover art)", DiscordRpc.appId, a, false) {
                DiscordRpc.setAppId(ctx, it)
            }
            PfAction(Icons.Rounded.Lock, "Sign out of Discord", "Forget the Discord login on this phone", a) {
                DiscordRpc.logout(ctx)
            }
        }
        if (DiscordRpc.status.isNotEmpty()) PfNote(DiscordRpc.status)
    }
    if (DiscordRpc.loginOpen) DiscordLogin()
    PfNote("A song is scrobbled after half of it has played (or 4 minutes), and only if it is longer than 30 seconds.")''')
add("Settings2.kt",
    'PfInfo(Icons.Rounded.Info, "Version", "v25", a)',
    'PfInfo(Icons.Rounded.Info, "Version", "v26", a)')
add("Settings2.kt",
    '    PfChange("v25", a, listOf(',
    '''    PfChange("v26", a, listOf(
        "Video and its blurred backdrop start and pause together",
        "Discord status: Integration, Discord",
        "New lyrics page: blurred lines, cover-tinted background, big bold text",
        "Opening lyrics: the player sinks down and the lyrics rise up",
        "Faster playback: song links are remembered, quicker start, quicker preload"
    ))
    PfChange("v25", a, listOf(''')

# ---------------- apply: all or nothing
new_contents = {}
bad = False
for fname, lst in edits.items():
    path = ROOT + fname
    if not os.path.exists(path):
        print("MISSING FILE:", path)
        bad = True
        continue
    text = open(path, encoding="utf-8").read()
    for old, new in lst:
        n = text.count(old)
        if n != 1:
            print("PROBLEM in", fname, "- found", n, "times:", old[:70].replace("\n", " | "))
            bad = True
        else:
            text = text.replace(old, new)
    new_contents[fname] = text

if bad:
    print("\nNothing was changed. Send me the lines above.")
    sys.exit(1)

for fname, text in new_contents.items():
    open(ROOT + fname, "w", encoding="utf-8").write(text)
    print("patched", fname)
print("\nDone. Now: cd ~/PrismWave && git add -A && git commit -m v26 && git push")
