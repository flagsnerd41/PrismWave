import os, sys

here = os.path.dirname(os.path.abspath(__file__))
app = os.path.join(here, "app", "src", "main", "java", "com", "prismwave", "app")

def fail(msg):
    print("PATCH FAILED:", msg)
    sys.exit(1)

def once(t, old, new, label):
    n = t.count(old)
    if n != 1:
        fail(label + ": found " + str(n) + " times (apply the archive look zip first): " + old.strip()[:60])
    return t.replace(old, new)

p = os.path.join(app, "LyricsStage.kt")
if not os.path.exists(p):
    fail("LyricsStage.kt is missing")
t = open(p, encoding="utf-8").read()
if "BounceText" in t:
    print("LyricsStage.kt already patched")
    sys.exit(0)

# 1. which line plays, which line the list waits on
t = once(t,
    "    val active by remember(lines, synced, off) {\n"
    "        derivedStateOf {\n"
    "            if (synced) lines.indexOfLast { it.ms <= smoothMs + off } else -1\n"
    "        }\n"
    "    }\n",
    "    val playing by remember(lines, synced, off) {\n"
    "        derivedStateOf { if (synced) lines.indexOfLast { it.ms <= smoothMs + off } else -1 }\n"
    "    }\n"
    "    // a line is lit only while it is being sung. After that the list moves on to the next line,\n"
    "    // which waits there dim until the song reaches it, like ArchiveTune\n"
    "    val ended by remember(lines, synced, off) {\n"
    "        derivedStateOf {\n"
    "            val pi = playing\n"
    "            synced && pi >= 0 && smoothMs + off >= lines[pi].ms + sungMs(lines, pi)\n"
    "        }\n"
    "    }\n"
    "    val active by remember(lines, synced, off) {\n"
    "        derivedStateOf {\n"
    "            if (!synced) -1 else if (playing < 0) 0 else if (ended && playing + 1 < lines.size) playing + 1 else playing\n"
    "        }\n"
    "    }\n"
    "    val litIdx by remember(lines, synced, off) {\n"
    "        derivedStateOf { if (synced && playing >= 0 && !ended) playing else -1 }\n"
    "    }\n",
    "active line")

# 2. lighting and fading follow the lit line, the position follows the waiting line
t = once(t, "                                val on = !synced || i == active\n", "                                val on = !synced || i == litIdx\n", "lit")
t = once(t,
    "                                    if (on) 1f\n                                    else if (dist == 1) 0.52f\n",
    "                                    if (on) 1f\n                                    else if (dist == 0) 0.50f\n                                    else if (dist == 1) 0.52f\n",
    "alpha")
t = once(t, "                                    if (on || !blurOn || !synced) 0.dp\n", "                                    if (dist == 0 || !blurOn || !synced) 0.dp\n", "blur")
t = once(t, "                                    if (on && synced) 1f else if (synced) 0.86f else 1f,\n", "                                    if (!synced || dist == 0) 1f else 0.95f,\n", "scale")

# 3. the words bounce one after another when the line is lit
t = once(t,
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
    "                                        )\n",
    "                                        BounceText(ln.text, on && synced, synced, fontSize, lineHeight, center)\n",
    "text")

t = t.rstrip() + '''

// how long a line is sung: a guess from its length, never longer than the gap to the next line
private fun sungMs(lines: List<LyricLine>, i: Int): Long {
    val gap = if (i + 1 < lines.size) lines[i + 1].ms - lines[i].ms else 6000L
    return (lines[i].text.length * 75L).coerceIn(1400L, 6500L).coerceAtMost((gap - 200L).coerceAtLeast(500L))
}

// the line lights up when it is sung and its words bounce up one after another (like ArchiveTune)
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BounceText(
    text: String,
    lit: Boolean,
    animate: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    center: Boolean
) {
    val words = remember(text) { text.split(" ") }
    val scales = remember(words.size) { List(words.size) { Animatable(1f) } }
    val floats = remember(words.size) { List(words.size) { Animatable(0f) } }
    LaunchedEffect(lit) {
        if (lit && animate) {
            words.indices.forEach { i ->
                launch {
                    delay(i * 40L)
                    scales[i].animateTo(1.045f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
                    scales[i].animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                }
                launch {
                    delay(i * 40L)
                    floats[i].animateTo(-5f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
                    floats[i].animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                }
            }
        }
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (center) Arrangement.Center else Arrangement.Start
    ) {
        words.forEachIndexed { i, w ->
            Text(
                if (i < words.lastIndex) w + " " else w,
                color = Color.White,
                fontFamily = PrismFont,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    scaleX = scales[i].value
                    scaleY = scales[i].value
                    translationY = floats[i].value * this.density
                }
            )
        }
    }
}
'''
imports = [
    "androidx.compose.animation.core.Animatable",
    "androidx.compose.animation.core.Spring",
    "androidx.compose.foundation.layout.ExperimentalLayoutApi",
    "androidx.compose.foundation.layout.FlowRow",
    "kotlinx.coroutines.delay",
    "kotlinx.coroutines.launch",
]
add = ""
for imp in imports:
    if ("import " + imp + "\n") not in t:
        add += "import " + imp + "\n"
if add:
    t = once(t, "package com.prismwave.app\n", "package com.prismwave.app\n\n" + add, "imports")
open(p, "w", encoding="utf-8").write(t)
print("LyricsStage.kt patched: the next line waits dim until it is sung, then its words bounce")
print("All done. Now: git add -A && git commit && git push")
