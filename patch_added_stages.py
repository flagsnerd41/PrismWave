import os, subprocess, sys

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

player = os.path.join(app, "Player.kt")

# step 0: the popup menu (push 1) has to be in first
if "PopItem" not in read(player):
    r = subprocess.run([sys.executable, os.path.join(here, "patch_added_popup_menu.py")])
    if r.returncode != 0:
        fail("the popup menu patch did not apply")

t = read(player)
if "swipeConn" not in t:
    # 1. the popup gets a handle, and swiping up on it opens the half view
    a = t.index("    val cfg = LocalConfiguration.current\n    val maxH = (cfg.screenHeightDp * 0.80f).dp\n")
    b = t.index("    if (tempoOpen) TempoPitchDialog(s) { tempoOpen = false }\n")
    if b < a:
        fail("Player.kt: popup markers are in the wrong order")
    t = t[:a] + read(os.path.join(here, "popup_region_v2.txt")) + t[b:]

    # 2. the full player: swipe up for the half view, swipe up again for the full lyrics
    t = once(t,
        "    val dragY = remember { Animatable(0f) }\n    val scope = rememberCoroutineScope()\n",
        "    val dragY = remember { Animatable(0f) }\n    val scope = rememberCoroutineScope()\n"
        "    // steps: the player, the half lyrics view (cover on top, lyrics underneath), the full lyrics page\n"
        "    val halfP = remember { Animatable(0f) }\n"
        "    var upExtra by remember { mutableStateOf(0f) }\n"
        "    val halfVisible by remember { derivedStateOf { halfP.value > 0.001f } }\n"
        "    LaunchedEffect(PlayerStage.half) {\n"
        "        halfP.animateTo(if (PlayerStage.half) 1f else 0f, spring(dampingRatio = 0.85f, stiffness = 260f))\n"
        "    }\n"
        "    DisposableEffect(Unit) { onDispose { PlayerStage.half = false } }\n"
        "    BackHandler(enabled = PlayerStage.half) { PlayerStage.half = false }\n",
        "FullPlayer state")
    t = once(t,
        "                state = rememberDraggableState { d ->\n"
        "                    scope.launch { dragY.snapTo((dragY.value + d).coerceAtLeast(0f)) }\n"
        "                },\n"
        "                orientation = Orientation.Vertical,\n"
        "                onDragStopped = {\n"
        "                    if (dragY.value > 300f) s.showPlayer = false\n"
        "                    else dragY.animateTo(0f, spring(dampingRatio = 0.7f))\n"
        "                }\n",
        "                state = rememberDraggableState { d ->\n"
        "                    scope.launch {\n"
        "                        if (halfP.value > 0f || (dragY.value <= 0f && d < 0f)) {\n"
        "                            // dragging up opens the half view, dragging down closes it again\n"
        "                            val before = halfP.value\n"
        "                            halfP.snapTo((halfP.value - d / 650f).coerceIn(0f, 1f))\n"
        "                            if (before >= 1f && d < 0f) upExtra += -d\n"
        "                        } else {\n"
        "                            dragY.snapTo((dragY.value + d).coerceAtLeast(0f))\n"
        "                        }\n"
        "                    }\n"
        "                },\n"
        "                orientation = Orientation.Vertical,\n"
        "                onDragStopped = { vel ->\n"
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
        "                    } else if (dragY.value > 300f) {\n"
        "                        s.showPlayer = false\n"
        "                    } else {\n"
        "                        dragY.animateTo(0f, spring(dampingRatio = 0.7f))\n"
        "                    }\n"
        "                }\n",
        "FullPlayer drag")
    t = once(t,
        "        }\n    }\n    if (menuDialog) {\n        PlayerMenuSheet(\n",
        "        }\n"
        "        if (halfVisible) {\n"
        "            HalfLyricsView(s, song) { halfP.value }\n"
        "        }\n"
        "    }\n    if (menuDialog) {\n        PlayerMenuSheet(\n",
        "FullPlayer half view")
    t = add_imports(t, [
        "androidx.compose.runtime.DisposableEffect",
        "androidx.compose.runtime.rememberUpdatedState",
        "androidx.compose.runtime.derivedStateOf",
        "androidx.compose.ui.input.nestedscroll.NestedScrollConnection",
        "androidx.compose.ui.input.nestedscroll.NestedScrollSource",
        "androidx.compose.ui.input.nestedscroll.nestedScroll",
        "androidx.compose.ui.unit.Velocity",
        "androidx.compose.foundation.gestures.detectVerticalDragGestures",
        "androidx.compose.ui.input.pointer.pointerInput",
        "androidx.compose.ui.geometry.Offset",
    ])
    write(player, t)
    print("Player.kt patched: swipe up steps")
else:
    print("Player.kt already patched")

# ---------------- LyricsScreen.kt: swipe down on the header closes the lyrics
p = os.path.join(app, "LyricsScreen.kt")
t = read(p)
if "detectVerticalDragGestures" not in t:
    if "val close: () -> Unit" in t:
        t = once(t,
            "            Row(\n"
            "                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),\n"
            "                verticalAlignment = Alignment.CenterVertically\n"
            "            ) {\n"
            "                Box(Modifier.size(46.dp).onGloballyPositioned",
            "            Row(\n"
            "                Modifier\n"
            "                    .fillMaxWidth()\n"
            "                    .pointerInput(Unit) {\n"
            "                        var acc = 0f\n"
            "                        detectVerticalDragGestures(\n"
            "                            onDragStart = { acc = 0f },\n"
            "                            onDragEnd = { if (acc > 80f) close() },\n"
            "                            onVerticalDrag = { _, dy -> acc += dy }\n"
            "                        )\n"
            "                    }\n"
            "                    .padding(horizontal = 16.dp, vertical = 10.dp),\n"
            "                verticalAlignment = Alignment.CenterVertically\n"
            "            ) {\n"
            "                Box(Modifier.size(46.dp).onGloballyPositioned",
            "Lyrics header")
        t = add_imports(t, ["androidx.compose.foundation.gestures.detectVerticalDragGestures"])
        write(p, t)
        print("LyricsScreen.kt patched: swipe down on the header closes")
    else:
        print("LyricsScreen.kt: close animation not installed, skipped")
else:
    print("LyricsScreen.kt already patched")

# ---------------- Settings2.kt: changelog
p = os.path.join(app, "Settings2.kt")
t = read(p)
key = '    PfChange("Added popup menu'
if "Added swipe steps" not in t and t.count(key) == 1:
    entry = (
        '    PfChange("Added swipe steps and half lyrics view", a, listOf(\n'
        '        "Swipe up on the player: half view with the cover on top, a blurred bottom and the lyrics underneath",\n'
        '        "Swipe up again: the full lyrics page. Swipe down goes back one step",\n'
        '        "Swipe up on the three dots menu opens the half view too",\n'
        '        "Swipe down on the lyrics header closes the lyrics"\n'
        '    ))\n'
    )
    t = t.replace(key, entry + key, 1)
    write(p, t)
    print("Settings2.kt: changelog added")
print("All done. Now: git add -A && git commit && git push")
