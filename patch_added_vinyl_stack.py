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

# ---------------- Player.kt: the Vinyl style
p = os.path.join(app, "Player.kt")
t = read(p)
if "VinylBody" not in t:
    t = once(t,
        "            if (style == 1) {\n"
        "                ImmersiveBody(\n"
        "                    s, song,\n"
        "                    { menuDialog = true }, { addDialog = true }, { sleepDialog = true }, { queueDialog = true }\n"
        "                )\n"
        "            } else {\n",
        "            if (style == 2) {\n"
        "                VinylBody(\n"
        "                    s, song,\n"
        "                    { menuDialog = true }, { addDialog = true }, { sleepDialog = true }, { queueDialog = true }\n"
        "                )\n"
        "            } else if (style == 1) {\n"
        "                ImmersiveBody(\n"
        "                    s, song,\n"
        "                    { menuDialog = true }, { addDialog = true }, { sleepDialog = true }, { queueDialog = true }\n"
        "                )\n"
        "            } else {\n",
        "Player dispatch")
    anchor = "@Composable\nfun PlayButton(s: AppState, size: Dp, iconSize: Dp) {\n"
    block = read(os.path.join(here, "vinyl_block.txt"))
    t = once(t, anchor, block + anchor, "Player insert")
    t = add_imports(t, [
        "androidx.compose.animation.Crossfade",
        "androidx.compose.animation.core.RepeatMode",
        "androidx.compose.animation.core.infiniteRepeatable",
        "androidx.compose.animation.core.rememberInfiniteTransition",
        "androidx.compose.animation.core.LinearEasing",
        "androidx.compose.animation.core.Animatable",
        "androidx.compose.animation.core.spring",
        "androidx.compose.animation.slideInVertically",
        "androidx.compose.animation.slideOutVertically",
        "androidx.compose.foundation.Canvas",
        "androidx.compose.foundation.interaction.MutableInteractionSource",
        "androidx.compose.material.icons.rounded.Layers",
        "androidx.compose.ui.geometry.Offset",
        "androidx.compose.ui.graphics.drawscope.Stroke",
        "androidx.compose.ui.graphics.toArgb",
        "androidx.compose.ui.platform.LocalContext",
        "kotlin.math.PI",
        "kotlin.math.cos",
        "kotlin.math.sin",
    ])
    write(p, t)
    print("Player.kt patched: Vinyl style")
else:
    print("Player.kt already patched")

# ---------------- Pages.kt: the cover stack page
p = os.path.join(app, "Pages.kt")
t = read(p)
if "CoverStackScreen" not in t:
    lines = t.split("\n")
    out = []
    done = False
    for ln in lines:
        out.append(ln)
        if (not done) and ln.strip() == "\"lyrics\" -> LyricsScreen(s)":
            indent = ln[:len(ln) - len(ln.lstrip())]
            out.append(indent + "\"stack\" -> CoverStackScreen(s)")
            done = True
    if not done:
        fail("Pages.kt: lyrics page line not found")
    write(p, "\n".join(out))
    print("Pages.kt patched: stack page")
else:
    print("Pages.kt already patched")

# ---------------- Settings2.kt: Vinyl in the style list, changelog
p = os.path.join(app, "Settings2.kt")
t = read(p)
if "\"Vinyl\"" not in t:
    t = once(t,
        "listOf(\"Album\", \"Immersive\"), s.playerStyle, a,\n            \"More styles are coming soon\"",
        "listOf(\"Album\", \"Immersive\", \"Vinyl\"), s.playerStyle, a,\n            \"Vinyl: a spinning record with lyrics underneath\"",
        "Settings style list")
key = '    PfChange("Added lyrics upgrade'
if "Added Vinyl player" not in t and t.count(key) == 1:
    entry = (
        '    PfChange("Added Vinyl player and cover stack", a, listOf(\n'
        '        "New Vinyl player style: a spinning record with the cover as the label, a swirling background and the lyric lines underneath",\n'
        '        "Cover stack: a 3D stack of your queue, history or likes. Drag to flip through it",\n'
        '        "Choose Vinyl in Settings, Appearance, Player design style"\n'
        '    ))\n'
    )
    t = t.replace(key, entry + key, 1)
write(p, t)
print("Settings2.kt patched")
print("All done. Now: git add -A && git commit && git push")
