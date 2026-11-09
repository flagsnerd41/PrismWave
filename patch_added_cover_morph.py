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

p = os.path.join(app, "Player.kt")
t = read(p)
if "shareCover()" not in t:
    # the big cover of the Album style tells the lyrics page where it is
    t = once(t,
        "                .fillMaxWidth()\n"
        "                .aspectRatio(1f)\n"
        "                .graphicsLayer {\n"
        "                    scaleX = coverScale\n",
        "                .fillMaxWidth()\n"
        "                .aspectRatio(1f)\n"
        "                .shareCover()\n"
        "                .graphicsLayer {\n"
        "                    scaleX = coverScale\n",
        "Album cover")
    # the player stays where it is and fades while the cover flies away
    t = once(t,
        "                translationY = lyShift * size.height * 0.18f\n"
        "                alpha = 1f - lyShift * 0.9f\n",
        "                translationY = if (SharedCover.rect != null) 0f else lyShift * size.height * 0.18f\n"
        "                alpha = if (SharedCover.rect != null) (1f - LyricsMorph.p * 1.8f).coerceAtLeast(0f) else (1f - lyShift * 0.9f).coerceAtLeast(0f)\n",
        "Player fade")
    # the Vinyl style (if it is installed): the record does the same, and it spins up and winds down
    if "private fun VinylDisc" in t:
        t = once(t,
            "    Box(\n        modifier\n            .aspectRatio(1f)\n            .graphicsLayer {\n                scaleX = sc\n",
            "    Box(\n        modifier\n            .aspectRatio(1f)\n            .shareCover()\n            .graphicsLayer {\n                scaleX = sc\n",
            "Vinyl share")
        t = once(t,
            "    LaunchedEffect(s.playing, s.reduceMotion) {\n"
            "        if (s.playing && !s.reduceMotion) {\n"
            "            while (true) {\n"
            "                rot.animateTo(rot.value + 360f, tween(9000, easing = LinearEasing))\n"
            "                rot.snapTo(rot.value % 360f)\n"
            "            }\n"
            "        }\n"
            "    }\n",
            "    LaunchedEffect(s.playing, s.reduceMotion) {\n"
            "        if (!s.reduceMotion) {\n"
            "            if (s.playing) {\n"
            "                // speeds up gently until it reaches the normal speed of 40 degrees a second\n"
            "                rot.animateTo(rot.value + 30f, tween(1500, easing = Easing { f -> f * f }))\n"
            "                while (true) {\n"
            "                    rot.animateTo(rot.value + 360f, tween(9000, easing = LinearEasing))\n"
            "                    rot.snapTo(rot.value % 360f)\n"
            "                }\n"
            "            } else {\n"
            "                // keeps turning for a moment and slows down like a real record\n"
            "                rot.animateDecay(40f, exponentialDecay(frictionMultiplier = 0.3f))\n"
            "                rot.snapTo(rot.value % 360f)\n"
            "            }\n"
            "        }\n"
            "    }\n",
            "Vinyl spin")
        t = add_imports(t, [
            "androidx.compose.animation.core.Easing",
            "androidx.compose.animation.core.exponentialDecay",
        ])
    write(p, t)
    print("Player.kt patched: cover morph, record spin up and down")
else:
    print("Player.kt already patched")

p = os.path.join(app, "Settings2.kt")
t = read(p)
key = '    PfChange("Added Vinyl player and cover stack"'
if "Added cover morph" not in t and t.count(key) == 1:
    entry = (
        '    PfChange("Added cover morph and record spin", a, listOf(\n'
        '        "Opening lyrics: the big cover shrinks into the top corner and the title slides up, like Apple Music",\n'
        '        "Pause button in the lyrics header",\n'
        '        "The vinyl record speeds up when you play and slowly winds down when you pause"\n'
        '    ))\n'
    )
    t = t.replace(key, entry + key, 1)
    write(p, t)
    print("Settings2.kt: changelog added")
print("All done. Now: git add -A && git commit && git push")
