import os, re, sys

D = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
names = ["Theme.kt", "MainActivity.kt", "Pages.kt", "Player.kt", "Screens.kt", "Components.kt"]
files = {n: open(D + n, encoding="utf-8").read() for n in names}


def rep(n, old, new):
    if old not in files[n]:
        sys.exit("MISSING in " + n + ": " + old[:90])
    files[n] = files[n].replace(old, new, 1)


def sub_grp(n, pattern, fn):
    t, k = re.subn(pattern, fn, files[n], count=1, flags=re.S)
    if k != 1:
        sys.exit("MISSING pattern in " + n + ": " + pattern[:90])
    files[n] = t


def imports(n, text):
    files[n] = files[n].replace("package com.prismwave.app\n", "package com.prismwave.app\n\n" + text, 1)


# ===================================================== Theme.kt: four themes
rep("Theme.kt", "val Night = Color(0xFF16120C)\nval Surface1 = Color(0xFF241F15)\nval Surface2 = Color(0xFF2F2819)\nval TextDim = Color(0xFFB8AE98)\n",
    '''class Palette(val night: Color, val s1: Color, val s2: Color, val dim: Color)

private val palettes = listOf(
    Palette(Color(0xFF16120C), Color(0xFF241F15), Color(0xFF2F2819), Color(0xFFB8AE98)), // Warm
    Palette(Color(0xFF121212), Color(0xFF1E1E1E), Color(0xFF2A2A2A), Color(0xFFB0B0B0)), // Dark
    Palette(Color(0xFF000000), Color(0xFF0D0D0D), Color(0xFF1A1A1A), Color(0xFFA8A8A8)), // AMOLED
    Palette(Color(0xFF0B1220), Color(0xFF141C2E), Color(0xFF1C2740), Color(0xFFA9B4CC))  // Midnight
)

private fun pal(): Palette = palettes[Opts.theme.coerceIn(0, palettes.size - 1)]

val Night: Color get() = pal().night
val Surface1: Color get() = pal().s1
val Surface2: Color get() = pal().s2
val TextDim: Color get() = pal().dim
''')

# ===================================================== Pages.kt
rep("Pages.kt", "    var bassBoost by mutableStateOf(0)\n",
    "    var bassBoost by mutableStateOf(0)\n    var theme by mutableStateOf(0)\n    var lite by mutableStateOf(false)\n")
rep("Pages.kt", '        bassBoost = p.getInt("bassBoost", 0)\n',
    '        bassBoost = p.getInt("bassBoost", 0)\n        theme = p.getInt("theme", 0)\n        lite = p.getBoolean("lite", false)\n')
rep("Pages.kt", '            ?.putInt("bassBoost", bassBoost)\n',
    '            ?.putInt("bassBoost", bassBoost)\n            ?.putInt("theme", theme)\n            ?.putBoolean("lite", lite)\n')
imports("Pages.kt",
    "import androidx.compose.animation.core.Animatable\n"
    "import androidx.compose.foundation.layout.Arrangement\n"
    "import androidx.compose.material.icons.rounded.Home\n"
    "import androidx.compose.material.icons.rounded.LibraryMusic\n"
    "import androidx.compose.material.icons.rounded.Search\n"
    "import androidx.compose.material.icons.rounded.Settings\n"
    "import androidx.compose.ui.draw.drawBehind\n"
    "import androidx.compose.ui.geometry.CornerRadius\n"
    "import androidx.compose.ui.geometry.Offset\n"
    "import androidx.compose.ui.graphics.Shadow\n"
    "import androidx.compose.ui.text.TextStyle\n"
    "import kotlinx.coroutines.delay\n"
    "import kotlinx.coroutines.launch\n")
# lite mode: no heavy blurred cover behind pages
rep("Pages.kt", "Cover(cur, Modifier.fillMaxSize().blur(60.dp), 0.dp, 120.sp, quality = 400)",
    "Cover(cur, if (Opts.lite) Modifier.fillMaxSize() else Modifier.fillMaxSize().blur(60.dp), 0.dp, 120.sp, quality = if (Opts.lite) 200 else 400)")
rep("Pages.kt", "Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)",
    "Text(title, color = Color.White, fontFamily = PrismFont, fontSize = 26.sp, fontWeight = FontWeight.Bold)")
# gradient cards (no blur, cheap)
rep("Pages.kt", ".background(Color.White.copy(alpha = 0.09f))",
    ".background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f))))\n            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))")
files["Pages.kt"] += '''

// ------------------------------------------------------------ floating bottom bar

@Composable
fun FloatingNavBar(s: AppState) {
    val shape = RoundedCornerShape(36.dp)
    val tabs = listOf(
        Triple(Screen.Home, Icons.Rounded.Home, "Home"),
        Triple(Screen.Search, Icons.Rounded.Search, "Search"),
        Triple(Screen.Library, Icons.Rounded.LibraryMusic, "Library"),
        Triple(Screen.Settings, Icons.Rounded.Settings, "Settings")
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Surface1.copy(alpha = 0.97f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { (scr, icon, label) ->
            val sel = s.screen == scr
            val a by animateFloatAsState(if (sel) 1f else 0f, tween(260), label = "navsel")
            val accent = s.accent
            Row(
                Modifier
                    .weight(if (sel) 1.7f else 1f)
                    .height(52.dp)
                    .drawBehind {
                        if (a > 0.01f) {
                            if (!Opts.lite) {
                                drawRoundRect(
                                    brush = Brush.radialGradient(
                                        listOf(accent.copy(alpha = 0.30f * a), Color.Transparent),
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        radius = size.width * 0.75f
                                    ),
                                    topLeft = Offset(-12.dp.toPx(), -10.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(size.width + 24.dp.toPx(), size.height + 20.dp.toPx()),
                                    cornerRadius = CornerRadius(40.dp.toPx())
                                )
                            }
                            drawRoundRect(accent.copy(alpha = 0.18f * a), cornerRadius = CornerRadius(size.height / 2f))
                        }
                    }
                    .bounceClick {
                        s.openPlaylist = null
                        s.libraryOpen = null
                        s.screen = scr
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = lerp(TextDim, accent, a), modifier = Modifier.size(26.dp))
                if (sel) {
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

// ------------------------------------------------------------ welcome: "made by flagsnerd", word by word

@Composable
fun GlowWords(accent: Color) {
    val words = listOf("made", "by", "flagsnerd")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        words.forEachIndexed { i, w ->
            val a = remember { Animatable(0f) }
            val g = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                delay(900L + i * 330L)
                launch { a.animateTo(1f, tween(420)) }
                g.animateTo(1f, tween(280))
                g.animateTo(if (i == 2) 0.85f else 0.3f, tween(700))
            }
            val c = if (i == 2) accent else Color.White
            Text(
                w,
                color = c,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                style = TextStyle(shadow = Shadow(color = c, offset = Offset.Zero, blurRadius = 4f + 22f * g.value)),
                modifier = Modifier.graphicsLayer {
                    alpha = a.value
                    translationY = (1f - a.value) * 24f
                }
            )
        }
    }
}
'''

# ===================================================== MainActivity.kt
imports("MainActivity.kt",
    "import androidx.compose.foundation.border\n"
    "import androidx.compose.runtime.derivedStateOf\n"
    "import androidx.compose.ui.input.nestedscroll.nestedScroll\n")
rep("MainActivity.kt", "delay(1250)", "delay(2300)")
sub_grp("MainActivity.kt", r'(translationY = \(1f - titleIn\.value\) \* 140f\s*\}\s*\))',
    lambda m: m.group(1) + "\n                                Spacer(Modifier.height(10.dp))\n                                GlowWords(s.accent)")
rep("MainActivity.kt", "                NavBar(s)", "                FloatingNavBar(s)")
rep("MainActivity.kt", "val blurActive = s.blurOn && !s.batterySaver && Build.VERSION.SDK_INT >= 31",
    "val blurActive = s.blurOn && !s.batterySaver && !Opts.lite && Build.VERSION.SDK_INT >= 31")
rep("MainActivity.kt", "val strength = if (cur != null && Opts.livingBg) 1f else 0.5f",
    "val strength = (if (cur != null && Opts.livingBg) 1f else 0.5f) * (if (Opts.theme == 2) 0.35f else 1f)")
# collapsing title bar driven by scrolling anywhere
rep("MainActivity.kt", "    CompositionLocalProvider(LocalBackdrop provides backdrop, LocalReduceMotion provides s.reduceMotion) {",
    '''    var scrolled by remember { mutableStateOf(0f) }
    LaunchedEffect(s.screen, s.openPlaylist, s.libraryOpen) { scrolled = 0f }
    val showBar by remember { derivedStateOf { scrolled > 6f } }
    val scrollConn = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                scrolled = (scrolled - consumed.y).coerceAtLeast(0f)
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
    CompositionLocalProvider(LocalBackdrop provides backdrop, LocalReduceMotion provides s.reduceMotion) {''')
sub_grp("MainActivity.kt",
    r'(CompositionLocalProvider\(LocalBackdrop provides backdrop, LocalReduceMotion provides s\.reduceMotion\) \{\s*Box\(Modifier\.fillMaxSize\(\)\.background\(Night\))\)',
    lambda m: m.group(1) + ".nestedScroll(scrollConn))")
rep("MainActivity.kt", ") { TopBlur(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 18.dp) }\n",
    ''') { TopBlur(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 18.dp) }
            if (showBar && s.openPlaylist == null) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .graphicsLayer { alpha = (scrolled / 240f).coerceIn(0f, 1f) }
                        .background(Night.copy(alpha = 0.96f))
                        .statusBarsPadding()
                        .height(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when (s.screen) {
                            Screen.Home -> "PrismWave"
                            Screen.Search -> "Search"
                            Screen.Library -> s.libraryOpen ?: "Library"
                            else -> "Settings"
                        },
                        color = Color.White,
                        fontFamily = PrismFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
''')

# ===================================================== Player.kt (lite mode)
rep("Player.kt", "if (!Opts.statusBlur) return", "if (!Opts.statusBlur || Opts.lite) return")
rep("Player.kt", "val a = maxOf(glow.value * 0.6f, if (s.playing) pulseA else 0f)",
    "val a = if (Opts.lite) 0f else maxOf(glow.value * 0.6f, if (s.playing) pulseA else 0f)")

# ===================================================== Components.kt (typography)
rep("Components.kt", "Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))",
    "Text(text, color = Color.White, fontFamily = PrismFont, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))")

# ===================================================== Screens.kt
rep("Screens.kt", 'Text(titles[pg] ?: "Settings", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)',
    'Text(titles[pg] ?: "Settings", color = Color.White, fontFamily = PrismFont, fontSize = 26.sp, fontWeight = FontWeight.Bold)')
# gradient settings cards
sub_grp("Screens.kt",
    r'(private fun Section\(title: String, content: @Composable ColumnScope\.\(\) -> Unit\) \{\s*Column\(\s*Modifier\s*\.padding\(horizontal = 20\.dp, vertical = 8\.dp\)\s*\.fillMaxWidth\(\)\s*\.clip\(RoundedCornerShape\(24\.dp\)\)\s*)\.background\(CardBg\)',
    lambda m: m.group(1) + ".background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))))\n            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))")
# theme + lite mode controls
rep("Screens.kt", '                    Section("Status bar") {',
    '''                    Section("Style") {
                        Text("Theme", color = TextDim)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Warm", "Dark", "AMOLED", "Midnight").forEachIndexed { i, n ->
                                Chip(n, Opts.theme == i, s.accent) {
                                    Opts.theme = i
                                    Opts.save()
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        ToggleRow("Lite mode", "Turns off glow, blur and heavy effects for low-end phones", Opts.lite) { Opts.lite = it; Opts.save() }
                    }
                    Section("Status bar") {''')
rep("Screens.kt", '                    Section("v18") {',
    '''                    Section("v19") {
                        ChangeList(listOf(
                            "Themes: Warm, Dark, AMOLED, Midnight",
                            "Floating bottom bar with a glowing selected tab",
                            "Title bar that fades in as you scroll",
                            "Welcome animation: made by flagsnerd, word by word",
                            "Gradient cards and bolder titles",
                            "Lite mode for low-end phones"
                        ))
                    }
                    Section("v18") {''')
rep("Screens.kt", 'Text("Version v18"', 'Text("Version v19"')

for n in names:
    open(D + n, "w", encoding="utf-8").write(files[n])
print("v19 patch applied")
