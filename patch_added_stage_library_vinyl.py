import os, sys

here = os.path.dirname(os.path.abspath(__file__))
app = os.path.join(here, "app", "src", "main", "java", "com", "prismwave", "app")

def fail(msg):
    print("PATCH FAILED:", msg)
    sys.exit(1)

def read(p):
    return open(p, encoding="utf-8").read()

def write(p, t):
    open(p, "w", encoding="utf-8").write(t)

def once(t, old, new, label):
    n = t.count(old)
    if n != 1:
        fail(label + ": found " + str(n) + " times: " + old.strip()[:70])
    return t.replace(old, new)

def add_imports(t, imports):
    add = ""
    for imp in imports:
        if ("import " + imp + "\n") not in t:
            add += "import " + imp + "\n"
    if add:
        t = once(t, "package com.prismwave.app\n", "package com.prismwave.app\n\n" + add, "imports")
    return t

# the old half view file is replaced by LyricsStage.kt
old_half = os.path.join(app, "HalfView.kt")
if os.path.exists(old_half):
    os.remove(old_half)
    print("HalfView.kt removed (LyricsStage.kt replaces it)")

# ---------------- Player.kt
p = os.path.join(app, "Player.kt")
t = read(p)
if "LyricsStage(s, song)" not in t:
    # steps are driven by PlayerStage.stage, 0 to 2
    t = once(t,
        "    LaunchedEffect(PlayerStage.half) {\n"
        "        halfP.animateTo(if (PlayerStage.half) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = 260f))\n"
        "    }\n"
        "    DisposableEffect(Unit) { onDispose { PlayerStage.half = false } }\n"
        "    BackHandler(enabled = PlayerStage.half) { PlayerStage.half = false }\n",
        "    LaunchedEffect(PlayerStage.stage) {\n"
        "        halfP.animateTo(PlayerStage.stage.toFloat(), spring(dampingRatio = 0.85f, stiffness = 260f))\n"
        "    }\n"
        "    DisposableEffect(Unit) {\n"
        "        PlayerStage.open = true\n"
        "        onDispose {\n"
        "            PlayerStage.open = false\n"
        "            PlayerStage.stage = 0\n"
        "        }\n"
        "    }\n"
        "    BackHandler(enabled = PlayerStage.stage > 0) { PlayerStage.back() }\n",
        "stage effects")
    t = once(t,
        "                        if (halfP.value > 0f || (dragY.value <= 0f && d < 0f)) {\n"
        "                            // dragging up opens the half view, dragging down closes it again\n"
        "                            val before = halfP.value\n"
        "                            halfP.snapTo((halfP.value - d / 650f).coerceIn(0f, 1f))\n"
        "                            if (before >= 1f && d < 0f) upExtra += -d\n"
        "                        } else {\n",
        "                        if (halfP.value > 0f || (dragY.value <= 0f && d < 0f)) {\n"
        "                            // dragging up goes up a step: player, half view, full lyrics. Dragging down goes back.\n"
        "                            halfP.snapTo((halfP.value - d / 650f).coerceIn(0f, 2f))\n"
        "                        } else {\n",
        "drag move")
    t = once(t,
        "                    if (halfP.value > 0f) {\n"
        "                        if (halfP.value >= 0.999f && (upExtra > 160f || vel < -1100f)) {\n"
        "                            upExtra = 0f\n"
        "                            Pages.show(\"lyrics\")\n"
        "                        } else {\n"
        "                            upExtra = 0f\n"
        "                            val goHalf = if (vel < -700f) true else if (vel > 700f) false else halfP.value > 0.45f\n"
        "                            PlayerStage.half = goHalf\n"
        "                            halfP.animateTo(if (goHalf) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = 260f))\n"
        "                        }\n"
        "                    } else if (dragY.value > 300f) {\n",
        "                    if (halfP.value > 0f) {\n"
        "                        val v = halfP.value\n"
        "                        val target = (\n"
        "                            if (vel < -700f) kotlin.math.floor(v) + 1f\n"
        "                            else if (vel > 700f) kotlin.math.ceil(v) - 1f\n"
        "                            else v.roundToInt().toFloat()\n"
        "                        ).coerceIn(0f, 2f).toInt()\n"
        "                        PlayerStage.stage = target\n"
        "                        halfP.animateTo(target.toFloat(), spring(dampingRatio = 0.85f, stiffness = 260f))\n"
        "                    } else if (dragY.value > 300f) {\n",
        "drag end")
    # the player fades out while the stage comes in
    t = once(t,
        "        AnimatedContent(\n"
        "            targetState = s.playerStyle,\n",
        "        AnimatedContent(\n"
        "            modifier = Modifier.graphicsLayer { alpha = (1f - halfP.value * 3f).coerceAtLeast(0f) },\n"
        "            targetState = s.playerStyle,\n",
        "style fade")
    t = once(t, "HalfLyricsView(s, song) { halfP.value }", "LyricsStage(s, song) { halfP.value }", "stage call")

    # Vinyl: the new layout like the reference
    a = t.index("@Composable\nprivate fun VinylBody(")
    b = t.index("@Composable\nfun PlayButton(")
    if b < a:
        fail("Player.kt: VinylBody markers are in the wrong order")
    t = t[:a] + read(os.path.join(here, "vinylbody.txt")) + t[b:]
    t = once(t, "            .aspectRatio(1f)\n            .shareCover()\n            .graphicsLayer {\n                scaleX = sc\n",
             "            .aspectRatio(1f)\n            .graphicsLayer {\n                scaleX = sc\n", "Vinyl outer share")
    t = once(t, "                .fillMaxSize(0.44f)\n                .align(Alignment.Center)\n                .graphicsLayer { rotationZ = rot.value }\n",
             "                .fillMaxSize(0.34f)\n                .align(Alignment.Center)\n                .shareCover(0, true)\n                .graphicsLayer { rotationZ = rot.value }\n", "Vinyl label")
    t = once(t, "radius = r * (0.50f + i * 0.0225f),", "radius = r * (0.44f + i * 0.0245f),", "Vinyl grooves")
    t = once(t, "radius = r * 0.47f, center = ctr)", "radius = r * 0.40f, center = ctr)", "Vinyl ring")
    t = once(t, "lerp(art, Color.Black, 0.60f), tween(700), label = \"swirlBase\"", "lerp(art, Color.Black, 0.30f), tween(700), label = \"swirlBase\"", "Swirl brighter")
    t = add_imports(t, [
        "androidx.compose.foundation.layout.BoxWithConstraints",
        "androidx.compose.foundation.layout.requiredSize",
        "androidx.compose.foundation.gestures.detectHorizontalDragGestures",
        "androidx.compose.foundation.gestures.detectTapGestures",
        "androidx.compose.material.icons.rounded.SkipPrevious",
        "androidx.compose.material.icons.rounded.SkipNext",
        "androidx.compose.material.icons.rounded.Pause",
        "androidx.compose.material.icons.rounded.PlayArrow",
        "androidx.compose.foundation.layout.offset",
        "kotlin.math.roundToInt",
    ])
    write(p, t)
    print("Player.kt patched: steps on one screen, new Vinyl layout")
else:
    print("Player.kt already patched")

# ---------------- Pages.kt: lyrics never open as a page while the player is open
p = os.path.join(app, "Pages.kt")
t = read(p)
if "PlayerStage.open" not in t:
    t = once(t,
        "    fun show(key: String, song: Song? = null, playlistId: String? = null) {\n        this.song = song\n",
        "    fun show(key: String, song: Song? = null, playlistId: String? = null) {\n"
        "        if (key == \"lyrics\" && PlayerStage.open) {\n"
        "            // the lyrics happen on the player itself: the cover moves up and the lyrics come in\n"
        "            PlayerStage.stage = 2\n"
        "            return\n"
        "        }\n"
        "        this.song = song\n",
        "Pages.show")
    write(p, t)
    print("Pages.kt patched: no separate lyrics page")
else:
    print("Pages.kt already patched")

# ---------------- LyricsScreen.kt: the stage reuses its pieces
p = os.path.join(app, "LyricsScreen.kt")
t = read(p)
changed = False
for name in ("LyricText", "PauseDots", "TuneBtn"):
    old = "private fun " + name + "("
    if old in t:
        t = t.replace(old, "fun " + name + "(", 1)
        changed = True
if changed:
    write(p, t)
    print("LyricsScreen.kt: pieces shared")

# ---------------- Muffled ending: a smooth sweep over the whole length, and a longer range
p = os.path.join(app, "AudioShape.kt")
t = read(p)
if "Math.pow(0.02" not in t:
    t = once(t,
        "        val k = 1f - m\n        val fc = 350f + 16000f * k * k * k\n",
        "        // the sound gets duller smoothly over the whole length, from 20000 Hz down to about 400 Hz\n"
        "        val fc = (20000.0 * Math.pow(0.02, m.toDouble())).toFloat().coerceIn(300f, 20000f)\n",
        "muffle curve")
    write(p, t)
    print("AudioShape.kt patched: the muffle builds up over the whole length")

p = os.path.join(app, "Studio.kt")
t = read(p)
if "getInt(\"mixSecs\", 12)" not in t:
    t = once(t, "    var mixSecs by mutableStateOf(10)\n", "    var mixSecs by mutableStateOf(12)\n", "mixSecs default")
    t = once(t, "mixSecs = sp.getInt(\"mixSecs\", 10).coerceIn(5, 15)", "mixSecs = sp.getInt(\"mixSecs\", 12).coerceIn(5, 20)", "mixSecs range")
    write(p, t)
    print("Studio.kt patched: longer muffle")

p = os.path.join(app, "Settings2.kt")
t = read(p)
old = "StudioPrefs.mixSecs.toFloat(), 5f..15f, a,"
if old in t:
    t = t.replace(old, "StudioPrefs.mixSecs.toFloat(), 5f..20f, a,")
key = '    PfChange("Added swipe steps and half lyrics view"'
if "Added stage" not in t and t.count(key) == 1:
    entry = (
        '    PfChange("Added stage lyrics, Library redesign, vinyl layout, longer muffle", a, listOf(\n'
        '        "Lyrics no longer open as another page: the cover moves up and shrinks into the corner on the same screen",\n'
        '        "Half view: the bottom of the cover melts into a blur that continues behind the lyrics",\n'
        '        "Library redesigned: sliding tabs, big colorful tiles, playlist grid with cover collages, artist grid",\n'
        '        "Vinyl: a huge record pushed to the right like the reference, the lines of the song at the bottom, swipe to change song",\n'
        '        "The muffled ending now builds up over the whole length (up to 20 seconds) and the length setting works"\n'
        '    ))\n'
    )
    t = t.replace(key, entry + key, 1)
write(p, t)
print("Settings2.kt patched")

# ---------------- Screens.kt: the Library
p = os.path.join(app, "Screens.kt")
t = read(p)
if "LibraryTabs" not in t:
    a = t.index("@Composable\nprivate fun LibraryMain(s: AppState) {")
    b = t.index("@Composable\nfun SongListScreen(")
    if b < a:
        fail("Screens.kt: Library markers are in the wrong order")
    t = t[:a] + read(os.path.join(here, "library_block.txt")) + t[b:]
    t = add_imports(t, [
        "androidx.compose.animation.core.animateDpAsState",
        "androidx.compose.animation.core.spring",
        "androidx.compose.foundation.layout.BoxWithConstraints",
        "androidx.compose.foundation.layout.offset",
        "androidx.compose.foundation.layout.fillMaxHeight",
        "androidx.compose.foundation.shape.CircleShape",
        "androidx.compose.foundation.interaction.MutableInteractionSource",
    ])
    write(p, t)
    print("Screens.kt patched: new Library")
else:
    print("Screens.kt already patched")
print("All done. Now: git add -A && git commit && git push")
