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

# the spot of the three dots button is remembered here
p = os.path.join(app, "SharedCover.kt")
t = read(p)
if "object MenuAnchor" not in t:
    t = t.rstrip() + "\n\n// where the three dots button is, so the popup menu can grow out of it\nobject MenuAnchor {\n    var rect: Rect? = null\n}\n"
    write(p, t)
    print("SharedCover.kt: MenuAnchor added")

p = os.path.join(app, "Player.kt")
t = read(p)
if "MenuAnchor" not in t:
    # every three dots button tells where it is
    t = once(t,
        "    Box(Modifier.bounceClick(onClick).size(box), contentAlignment = Alignment.Center) {\n",
        "    Box(\n"
        "        Modifier\n"
        "            .bounceClick(onClick)\n"
        "            .size(box)\n"
        "            .onGloballyPositioned { if (icon == Icons.Rounded.MoreHoriz) MenuAnchor.rect = it.boundsInRoot() },\n"
        "        contentAlignment = Alignment.Center\n"
        "    ) {\n",
        "PlayerIcon")
    t = once(t,
        "            Modifier.bounceClick(onMenu).size(40.dp).then(bgMod),\n",
        "            Modifier\n"
        "                .bounceClick(onMenu)\n"
        "                .size(40.dp)\n"
        "                .onGloballyPositioned { MenuAnchor.rect = it.boundsInRoot() }\n"
        "                .then(bgMod),\n",
        "TitleRow dots")
    # the popup card grows out of the dots
    a = t.index("        // a card that grows out of the top right corner, near the three dots\n")
    b = t.index("                    transformOrigin = TransformOrigin(1f, 0f)\n", a) + len("                    transformOrigin = TransformOrigin(1f, 0f)\n")
    new_head = (
        "        // a card that grows out of the three dots button: it opens down from it, or up when the button is low on the screen\n"
        "        val anchor = MenuAnchor.rect\n"
        "        val dens = LocalDensity.current\n"
        "        val screenW = with(dens) { cfg.screenWidthDp.dp.toPx() }\n"
        "        val screenH = with(dens) { cfg.screenHeightDp.dp.toPx() }\n"
        "        val popWpx = with(dens) { popW.toPx() }\n"
        "        val gap = with(dens) { 8.dp.toPx() }\n"
        "        val edge = with(dens) { 12.dp.toPx() }\n"
        "        val down = anchor == null || anchor.center.y < screenH * 0.5f\n"
        "        var cardH by remember { mutableStateOf(0) }\n"
        "        val cardLeft = if (anchor != null) {\n"
        "            (anchor.right + gap).coerceAtMost(screenW - edge) - popWpx\n"
        "        } else {\n"
        "            screenW - edge - popWpx\n"
        "        }\n"
        "        val cardTop = if (anchor == null) {\n"
        "            with(dens) { 62.dp.toPx() }\n"
        "        } else if (down) {\n"
        "            anchor.bottom + gap\n"
        "        } else {\n"
        "            (anchor.top - gap - cardH).coerceAtLeast(edge)\n"
        "        }\n"
        "        val originX = (((anchor?.center?.x ?: screenW) - cardLeft) / popWpx).coerceIn(0f, 1f)\n"
        "        val originY = if (down) 0f else 1f\n"
        "        val roomDp = if (anchor == null || down) maxH else minOf(maxH, with(dens) { ((anchor.top - gap - edge) / density).dp })\n"
        "        Column(\n"
        "            Modifier\n"
        "                .offset { IntOffset(cardLeft.roundToInt().coerceAtLeast(0), cardTop.roundToInt().coerceAtLeast(0)) }\n"
        "                .width(popW)\n"
        "                .heightIn(max = roomDp)\n"
        "                .onSizeChanged { cardH = it.height }\n"
        "                .graphicsLayer {\n"
        "                    val sc = 0.30f + 0.70f * o\n"
        "                    scaleX = sc\n"
        "                    scaleY = sc\n"
        "                    alpha = (o * 1.6f).coerceIn(0f, 1f)\n"
        "                    transformOrigin = TransformOrigin(originX, originY)\n"
    )
    t = t[:a] + new_head + t[b:]
    t = add_imports(t, [
        "androidx.compose.ui.layout.onGloballyPositioned",
        "androidx.compose.ui.layout.boundsInRoot",
        "androidx.compose.ui.layout.onSizeChanged",
        "androidx.compose.ui.platform.LocalDensity",
        "androidx.compose.ui.unit.IntOffset",
        "androidx.compose.foundation.layout.offset",
        "kotlin.math.roundToInt",
    ])
    write(p, t)
    print("Player.kt patched: popup grows out of the three dots")
else:
    print("Player.kt already patched")
print("All done. Now: git add -A && git commit && git push")
