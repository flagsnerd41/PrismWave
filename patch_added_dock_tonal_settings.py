#!/usr/bin/env python3
# added new dock (glow ring mini player, one-bar menu), tonal switch rows, dotted sliders and bottom sheet pickers
# run from anywhere:  python3 ~/PrismWave/patch_added_dock_tonal_settings.py
import os
import re
import sys

ROOT = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app")


def load(name):
    with open(os.path.join(ROOT, name), encoding="utf-8") as f:
        return f.read()


def must_replace(text, old, new, label):
    n = text.count(old)
    if n != 1:
        print("FAILED: " + label + " (found " + str(n) + " times, needed 1)")
        sys.exit(1)
    return text.replace(old, new, 1)


def swap_between(text, start, end, new, label):
    i0 = text.find(start)
    i1 = text.find(end)
    if i0 < 0 or i1 < 0 or i1 < i0 or text.count(start) != 1:
        print("FAILED: " + label + " markers (" + str(i0) + ", " + str(i1) + ")")
        sys.exit(1)
    return text[:i0] + new + text[i1:]


files = {}

# ---------------------------------------------------------------- MainActivity.kt: the new menu bar
t = load("MainActivity.kt")
t = must_replace(
    t,
    "                NavCatScene(s)\n                MiniPlayer(s)\n                NavBar(s)",
    "                NavCatScene(s)\n                MiniPlayer(s)\n                NavBar2(s)",
    "NavBar2",
)
files["MainActivity.kt"] = t

# ---------------------------------------------------------------- Player.kt: the mini player
t = load("Player.kt")

# bigger cover
t = must_replace(
    t,
    "Cover(song, Modifier.size(50.dp).onGloballyPositioned { MiniCover.rect = it.boundsInRoot() }, 12.dp, 20.sp)",
    "Cover(song, Modifier.size(54.dp).onGloballyPositioned { MiniCover.rect = it.boundsInRoot() }, 15.dp, 20.sp)",
    "Mini cover",
)

# the play button is a white circle
t = must_replace(
    t,
    "                    Box(Modifier.bounceClick { s.toggle() }.size(48.dp), contentAlignment = Alignment.Center) {\n"
    "                        if (s.loading) {\n"
    "                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))\n"
    "                        } else {",
    "                    Box(\n"
    "                        Modifier.bounceClick { s.toggle() }.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.95f)),\n"
    "                        contentAlignment = Alignment.Center\n"
    "                    ) {\n"
    "                        if (s.loading) {\n"
    "                            CircularProgressIndicator(color = Night, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))\n"
    "                        } else {",
    "Mini play button",
)
t = must_replace(
    t,
    "                                    if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,\n"
    "                                    contentDescription = null,\n"
    "                                    tint = Color.White,\n"
    "                                    modifier = Modifier.size(32.dp)",
    "                                    if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,\n"
    "                                    contentDescription = null,\n"
    "                                    tint = Night,\n"
    "                                    modifier = Modifier.size(28.dp)",
    "Mini play icon",
)

# moving glow inside the bar
t = must_replace(
    t,
    "            Box(Modifier.fillMaxWidth()) {\n"
    "                Row(\n"
    "                    Modifier.padding(horizontal = 10.dp, vertical = 9.dp),",
    "            Box(Modifier.fillMaxWidth()) {\n"
    "                Box(Modifier.matchParentSize().graphicsLayer { alpha = 0.5f }.glowBehind(art, s.playing, s.batterySaver, 0f))\n"
    "                Row(\n"
    "                    Modifier.padding(horizontal = 10.dp, vertical = 9.dp),",
    "Mini glow",
)

# a brighter progress line
t = must_replace(
    t,
    "                        .padding(start = 22.dp, bottom = 3.dp)\n"
    "                        .fillMaxWidth(frac)\n"
    "                        .height(2.dp)\n"
    "                        .clip(RoundedCornerShape(1.dp))\n"
    "                        .background(s.accent.copy(alpha = 0.9f))",
    "                        .padding(start = 22.dp, bottom = 4.dp)\n"
    "                        .fillMaxWidth(frac)\n"
    "                        .height(3.dp)\n"
    "                        .clip(RoundedCornerShape(2.dp))\n"
    "                        .background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White)))",
    "Mini progress",
)

# the running ring of light
t = must_replace(
    t,
    "        Box(\n"
    "            modifier\n"
    "                .clip(RoundedCornerShape(radius))\n"
    "                .background(Brush.horizontalGradient(listOf(lerp(Night, c, 0.62f), lerp(Night, c, 0.30f)))),\n"
    "            content = content\n"
    "        )",
    "        Box(\n"
    "            modifier\n"
    "                .glowRing(c, radius)\n"
    "                .clip(RoundedCornerShape(radius))\n"
    "                .background(Brush.horizontalGradient(listOf(lerp(Night, c, 0.62f), lerp(Night, c, 0.30f)))),\n"
    "            content = content\n"
    "        )",
    "Mini ring",
)
files["Player.kt"] = t

# ---------------------------------------------------------------- Settings2.kt
t = load("Settings2.kt")

# rows with a switch that is on fill with the accent color
t = must_replace(
    t,
    "    below: @Composable () -> Unit = {}\n"
    ") {\n"
    "    Column(\n"
    "        Modifier\n"
    "            .fillMaxWidth()\n"
    "            .background(CardBg)\n",
    "    below: @Composable () -> Unit = {},\n"
    "    tint: Boolean = false\n"
    ") {\n"
    "    val rowBg by androidx.compose.animation.animateColorAsState(\n"
    "        if (tint) androidx.compose.ui.graphics.lerp(CardBg, accent, 0.30f) else CardBg,\n"
    "        tween(300),\n"
    "        label = \"pfrowbg\"\n"
    "    )\n"
    "    Column(\n"
    "        Modifier\n"
    "            .fillMaxWidth()\n"
    "            .background(rowBg)\n",
    "PfRow tint",
)
t = must_replace(
    t,
    "    PfRow(\n"
    "        icon, title, sub, accent, { onChange(!checked) },\n"
    "        trailing = {",
    "    PfRow(\n"
    "        icon, title, sub, accent, { onChange(!checked) },\n"
    "        tint = checked,\n"
    "        trailing = {",
    "PfSwitch tint",
)

slider = r'''@Composable
private fun PfSlider(
    icon: ImageVector?,
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onChange: (Float) -> Unit,
    onFinish: () -> Unit = {}
) {
    PfRow(
        icon, title, valueText, accent, null,
        below = { PfDotSlider(value, range, accent, onChange, onFinish) }
    )
}

// a fat dotted track with a tall bar as the thumb
@Composable
private fun PfDotSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    accent: Color,
    onChange: (Float) -> Unit,
    onFinish: () -> Unit
) {
    val span = (range.endInclusive - range.start).coerceAtLeast(0.0001f)
    val frac = ((value - range.start) / span).coerceIn(0f, 1f)
    var dragging by remember { mutableStateOf(false) }
    val change by rememberUpdatedState(onChange)
    val finish by rememberUpdatedState(onFinish)
    val thumbW by androidx.compose.animation.core.animateDpAsState(if (dragging) 3.dp else 5.dp, tween(150), label = "dsThumb")
    Canvas(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .height(44.dp)
            .pointerInput(range) {
                detectTapGestures { o ->
                    change(range.start + (o.x / size.width).coerceIn(0f, 1f) * span)
                    finish()
                }
            }
            .pointerInput(range) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        finish()
                    },
                    onDragCancel = {
                        dragging = false
                        finish()
                    }
                ) { c, _ ->
                    c.consume()
                    change(range.start + (c.position.x / size.width).coerceIn(0f, 1f) * span)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val trackH = 16.dp.toPx()
        val top = (h - trackH) / 2f
        val gap = 7.dp.toPx()
        val tx = (w * frac).coerceIn(trackH / 2f, w - trackH / 2f)
        val round = CornerRadius(trackH / 2f)
        val leftW = (tx - gap).coerceAtLeast(0f)
        if (leftW > 0.5f) drawRoundRect(accent, Offset(0f, top), Size(leftW, trackH), round)
        val rx = tx + gap
        if (w - rx > 0.5f) drawRoundRect(accent.copy(alpha = 0.26f), Offset(rx, top), Size(w - rx, trackH), round)
        val dot = 1.6f.dp.toPx()
        val step = 10.dp.toPx()
        var x = 9.dp.toPx()
        while (x < leftW - 6.dp.toPx()) {
            drawCircle(Night.copy(alpha = 0.45f), dot, Offset(x, h / 2f))
            x += step
        }
        x = rx + 9.dp.toPx()
        while (x < w - 9.dp.toPx()) {
            drawCircle(accent.copy(alpha = 0.85f), dot, Offset(x, h / 2f))
            x += step
        }
        val tw = thumbW.toPx()
        drawRoundRect(accent, Offset(tx - tw / 2f, h / 2f - 18.dp.toPx()), Size(tw, 36.dp.toPx()), CornerRadius(tw / 2f))
    }
}

'''
t = swap_between(t, "@Composable\nprivate fun PfSlider(", "@Composable\nprivate fun PfChoice(", slider, "PfSlider")

choice = r'''@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PfChoice(
    icon: ImageVector?,
    title: String,
    options: List<String>,
    selected: Int,
    accent: Color,
    sub: String? = null,
    onSelect: (Int) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    PfRow(
        icon, title, sub, accent, { open = true },
        trailing = { Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
        below = {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accent.copy(alpha = 0.16f))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(options.getOrElse(selected) { "" }, color = accent, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    )
    if (open) {
        // a bottom sheet: big title, one pill per option, the chosen one filled with the accent color
        val sheet = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val sheetScope = rememberCoroutineScope()
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { open = false },
            sheetState = sheet,
            containerColor = Night,
            shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )
                options.forEachIndexed { i, o ->
                    val chosen = i == selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (chosen) accent else CardBg2)
                            .softClick {
                                onSelect(i)
                                sheetScope.launch {
                                    sheet.hide()
                                    open = false
                                }
                            }
                            .padding(horizontal = 28.dp, vertical = 22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(o, color = if (chosen) Night else Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
                        if (chosen) Icon(Icons.Rounded.Check, contentDescription = null, tint = Night)
                    }
                }
            }
        }
    }
}

'''
t = swap_between(t, "@Composable\nprivate fun PfChoice(", "@Composable\nprivate fun PfAction(", choice, "PfChoice")

marker = "private fun PfChangelogPage(s: AppState) {\n    val a = s.accent\n"
t = must_replace(
    t,
    marker,
    marker +
    '    PfChange("Added new dock, tonal settings and bottom sheet pickers", a, listOf(\n'
    '        "Menu bar: one glass bar with Home, Search and Library. The chosen tab grows, gets a bubble and shows its name",\n'
    '        "Mini player: a moving glow inside, a ring of light running around the edge, bigger cover and a round play button",\n'
    '        "Settings: a switch that is on fills its whole row with the accent color",\n'
    '        "Settings: dotted sliders with a tall thumb, and choices open as a bottom sheet with big pills"\n'
    '    ))\n',
    "Changelog",
)
files["Settings2.kt"] = t

# ---------------------------------------------------------------- Extras.kt (red dot on What's new)
t = load("Extras.kt")
t, n = re.subn(r'const val ID = "[^"]*"', 'const val ID = "added-dock-tonal-settings"', t, count=1)
if n != 1:
    print("FAILED: WhatsNew id")
    sys.exit(1)
files["Extras.kt"] = t

for name, text in files.items():
    with open(os.path.join(ROOT, name), "w", encoding="utf-8") as f:
        f.write(text)
    print("patched " + name)
print("done")
