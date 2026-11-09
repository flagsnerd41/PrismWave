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

# ---------------- Player.kt: popup menu, bigger vinyl cover
p = os.path.join(app, "Player.kt")
t = read(p)
if "PopItem" not in t:
    # 1. the menu becomes a popup card that grows out of the top right corner
    a = t.index("    val maxH = (LocalConfiguration.current.screenHeightDp * 0.92f).dp\n    Box(Modifier.fillMaxSize()) {\n")
    b = t.index("    if (tempoOpen) TempoPitchDialog(s) { tempoOpen = false }\n")
    if b < a:
        fail("Player.kt: menu markers are in the wrong order")
    t = t[:a] + read(os.path.join(here, "popup_region.txt")) + t[b:]
    anchor = "@Composable\nprivate fun PlayerMenuSheet(\n"
    t = once(t, anchor, read(os.path.join(here, "popup_block.txt")) + anchor, "Player menu anchor")
    t = add_imports(t, [
        "androidx.compose.ui.graphics.TransformOrigin",
        "androidx.compose.foundation.layout.requiredWidth",
    ])
    # 2. the vinyl: bigger record and a much bigger cover label
    if "private fun VinylDisc" in t:
        t = once(t, "                .fillMaxSize(0.30f)\n", "                .fillMaxSize(0.44f)\n", "Vinyl label size")
        t = once(t, "radius = r * (0.36f + i * 0.027f),", "radius = r * (0.50f + i * 0.0225f),", "Vinyl grooves")
        t = once(t,
            "drawCircle(color = Color.Black.copy(alpha = 0.7f), radius = r * 0.33f, center = ctr)",
            "drawCircle(color = Color.Black.copy(alpha = 0.7f), radius = r * 0.47f, center = ctr)",
            "Vinyl label ring")
        t = once(t,
            "                Modifier\n"
            "                    .fillMaxWidth()\n"
            "                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {\n"
            "                        Pages.show(\"lyrics\")\n",
            "                Modifier\n"
            "                    .requiredWidth(\n"
            "                        minOf(\n"
            "                            LocalConfiguration.current.screenWidthDp - 12,\n"
            "                            (LocalConfiguration.current.screenHeightDp * 0.43f).toInt()\n"
            "                        ).dp\n"
            "                    )\n"
            "                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {\n"
            "                        Pages.show(\"lyrics\")\n",
            "Vinyl size")
    write(p, t)
    print("Player.kt patched: popup menu, bigger vinyl cover")
else:
    print("Player.kt already patched")

# ---------------- Pages.kt: the lyrics page does its own animation (the cover flies), so no slide and fade on top of it
p = os.path.join(app, "Pages.kt")
t = read(p)
if "EnterTransition.None" not in t:
    t = once(t,
        "        enter = if (vert) {\n            androidx.compose.animation.slideInVertically(",
        "        enter = if (vert && SharedCover.rect != null) {\n"
        "            androidx.compose.animation.EnterTransition.None\n"
        "        } else if (vert) {\n"
        "            androidx.compose.animation.slideInVertically(",
        "Pages enter")
    t = once(t,
        "        exit = if (vert) {\n            androidx.compose.animation.slideOutVertically(",
        "        exit = if (vert && SharedCover.rect != null) {\n"
        "            androidx.compose.animation.ExitTransition.None\n"
        "        } else if (vert) {\n"
        "            androidx.compose.animation.slideOutVertically(",
        "Pages exit")
    write(p, t)
    print("Pages.kt patched: cover animation can show now")
else:
    print("Pages.kt already patched")

# ---------------- Settings2.kt: changelog
p = os.path.join(app, "Settings2.kt")
t = read(p)
key = '    PfChange("Added cover morph and record spin"'
if "Added popup menu" not in t and t.count(key) == 1:
    entry = (
        '    PfChange("Added popup menu, fixed cover animation, bigger vinyl cover", a, listOf(\n'
        '        "The three dots menu is now a popup that grows out of the corner, with parts that rise and sharpen one after another",\n'
        '        "Fixed the cover animation: the page no longer slides and fades over the flying cover",\n'
        '        "Vinyl: bigger record and a much bigger cover in the middle"\n'
        '    ))\n'
    )
    t = t.replace(key, entry + key, 1)
    write(p, t)
    print("Settings2.kt: changelog added")
print("All done. Now: git add -A && git commit && git push")
