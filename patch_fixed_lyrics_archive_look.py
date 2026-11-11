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

p = os.path.join(app, "LyricsStage.kt")
if not os.path.exists(p):
    fail("LyricsStage.kt is missing, apply the stage zip first")
t = read(p)
if "gradTop" not in t:
    # 1. a line lights up exactly when it is sung, not before
    t = once(t, "lines.indexOfLast { it.ms <= smoothMs + off + 250 }", "lines.indexOfLast { it.ms <= smoothMs + off }", "active line")
    t = once(t, "lines.indexOfLast { it.ms <= posMs + off + 250 }", "lines.indexOfLast { it.ms <= posMs + off }", "half line")

    # 2. the playing line sits at 35% from the top, like ArchiveTune
    t = once(t,
        "    LaunchedEffect(active, lines, fullVisible) {\n"
        "        if (fullVisible && active >= 0) {\n"
        "            val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == active }\n"
        "            if (info != null) {\n"
        "                listState.animateScrollBy(info.offset.toFloat(), tween(650, easing = FastOutSlowInEasing))\n"
        "            } else {\n"
        "                listState.animateScrollToItem(active)\n"
        "            }\n"
        "        }\n"
        "    }\n",
        "    LaunchedEffect(active, lines, fullVisible) {\n"
        "        if (fullVisible && active >= 0) {\n"
        "            val target = (listState.layoutInfo.viewportSize.height * 0.35f).toInt()\n"
        "            if (abs(active + 1 - listState.firstVisibleItemIndex) > 15) {\n"
        "                listState.scrollToItem((active - 1).coerceAtLeast(0), 0)\n"
        "            }\n"
        "            listState.animateScrollToItem(active + 1, -target)\n"
        "        }\n"
        "    }\n",
        "scroll anchor")

    # 3. gradient colors from the cover
    t = once(t,
        "    val base = lerp(art, Color.Black, 0.55f)\n",
        "    val base = lerp(art, Color.Black, 0.55f)\n"
        "    val gradTop = remember(art) { tone(art, 180f, 0.45f, 0.30f) }\n"
        "    val gradBottom = remember(art) { tone(art, 0f, 0.90f, 0.55f) }\n",
        "gradient colors")
    t = once(t,
        "        SwirlBackground(art, s.reduceMotion || Opts.lite)\n        if (!Opts.lite) {\n",
        "        Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - (st() - 1f).coerceIn(0f, 1f) }) {\n"
        "            SwirlBackground(art, s.reduceMotion || Opts.lite)\n"
        "        }\n"
        "        // the full lyrics get a smooth gradient of the cover colors, like ArchiveTune\n"
        "        Box(\n"
        "            Modifier\n"
        "                .fillMaxSize()\n"
        "                .graphicsLayer { alpha = (st() - 1f).coerceIn(0f, 1f) }\n"
        "                .background(Brush.verticalGradient(0f to gradTop, 0.5f to lerp(gradTop, gradBottom, 0.45f), 1f to gradBottom))\n"
        "        )\n"
        "        if (!Opts.lite) {\n",
        "gradient layer")

    # 4. the whole line, no word by word
    t = once(t,
        "                                    } else {\n"
        "                                        LyricText(ln.text, ln.ms, endMs, on && synced, { smoothMs + off }, fontSize, lineHeight, center)\n"
        "                                    }\n",
        "                                    } else {\n"
        "                                        // the whole line lights up at once, only when it is sung\n"
        "                                        Text(\n"
        "                                            ln.text,\n"
        "                                            color = Color.White,\n"
        "                                            fontFamily = PrismFont,\n"
        "                                            fontSize = fontSize,\n"
        "                                            lineHeight = lineHeight,\n"
        "                                            fontWeight = FontWeight.Bold,\n"
        "                                            textAlign = if (center) TextAlign.Center else TextAlign.Start,\n"
        "                                            modifier = Modifier.fillMaxWidth()\n"
        "                                        )\n"
        "                                    }\n",
        "whole line")

    # 5. fade and blur steps like ArchiveTune
    t = once(t,
        "                                val a by animateFloatAsState(\n"
        "                                    if (on) 1f else (0.5f - 0.07f * dist).coerceAtLeast(0.16f),\n"
        "                                    tween(380),\n"
        "                                    label = \"sa\"\n"
        "                                )\n",
        "                                val a by animateFloatAsState(\n"
        "                                    if (on) 1f\n"
        "                                    else if (dist == 1) 0.52f\n"
        "                                    else if (dist == 2) 0.30f\n"
        "                                    else if (dist == 3) 0.18f\n"
        "                                    else 0.10f,\n"
        "                                    tween(if (on) 330 else 500, easing = FastOutSlowInEasing),\n"
        "                                    label = \"sa\"\n"
        "                                )\n",
        "line alpha")
    t = once(t,
        "                                    if (on || !blurOn) 0.dp else (1.5f + 1.1f * dist).coerceAtMost(7f).dp,\n"
        "                                    tween(380),\n",
        "                                    if (on || !blurOn || !synced) 0.dp\n"
        "                                    else (if (dist == 1) 2f else if (dist == 2) 5f else 12f).dp,\n"
        "                                    tween(300, easing = FastOutSlowInEasing),\n",
        "line blur")

    # 6. head space, so the playing line can sit in the middle, and soft top and bottom edges
    t = once(t,
        "                        LazyColumn(\n"
        "                            Modifier.fillMaxSize(),\n"
        "                            state = listState,\n"
        "                            contentPadding = PaddingValues(top = 20.dp, bottom = 240.dp)\n"
        "                        ) {\n"
        "                            itemsIndexed(lines) { i, ln ->\n",
        "                        LazyColumn(\n"
        "                            Modifier\n"
        "                                .fillMaxSize()\n"
        "                                .graphicsLayer(alpha = 0.99f)\n"
        "                                .drawWithContent {\n"
        "                                    drawContent()\n"
        "                                    val px = 80.dp.toPx()\n"
        "                                    drawRect(\n"
        "                                        brush = Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, startY = 0f, endY = px),\n"
        "                                        blendMode = BlendMode.DstIn\n"
        "                                    )\n"
        "                                    drawRect(\n"
        "                                        brush = Brush.verticalGradient(0f to Color.Black, 1f to Color.Transparent, startY = size.height - px, endY = size.height),\n"
        "                                        blendMode = BlendMode.DstIn\n"
        "                                    )\n"
        "                                },\n"
        "                            state = listState,\n"
        "                            contentPadding = PaddingValues(top = 0.dp, bottom = 240.dp)\n"
        "                        ) {\n"
        "                            item { Spacer(Modifier.height(120.dp)) }\n"
        "                            itemsIndexed(lines) { i, ln ->\n",
        "list")

    # helper and imports
    t = t.rstrip() + (
        "\n\n// a color from the cover with its hue turned, a maximum saturation and a fixed brightness\n"
        "private fun tone(c: Color, turn: Float, maxSat: Float, bright: Float): Color {\n"
        "    val hsv = FloatArray(3)\n"
        "    android.graphics.Color.colorToHSV(c.toArgb(), hsv)\n"
        "    hsv[0] = (hsv[0] + turn + 360f) % 360f\n"
        "    hsv[1] = (hsv[1] + 0.15f).coerceAtMost(maxSat)\n"
        "    hsv[2] = bright\n"
        "    return Color(android.graphics.Color.HSVToColor(hsv))\n"
        "}\n"
    )
    imports = ["androidx.compose.ui.graphics.toArgb", "androidx.compose.ui.text.style.TextAlign"]
    add = ""
    for imp in imports:
        if ("import " + imp + "\n") not in t:
            add += "import " + imp + "\n"
    if add:
        t = once(t, "package com.prismwave.app\n", "package com.prismwave.app\n\n" + add, "imports")
    write(p, t)
    print("LyricsStage.kt patched: gradient, whole lines, lyrics in the middle, exact timing")
else:
    print("LyricsStage.kt already patched")

p = os.path.join(app, "Settings2.kt")
t = read(p)
key = '    PfChange("Added stage lyrics'
if "Added ArchiveTune lyrics look" not in t and t.count(key) == 1:
    entry = (
        '    PfChange("Added ArchiveTune lyrics look", a, listOf(\n'
        '        "Gradient background from the cover colors, the playing line sits in the middle",\n'
        '        "The whole line lights up at once, only when it is sung, no word by word",\n'
        '        "Lines fade and blur the further they are from the playing one"\n'
        '    ))\n'
    )
    t = t.replace(key, entry + key, 1)
    write(p, t)
print("All done. Now: git add -A && git commit && git push")
