import os, re, sys

base = os.path.join(os.path.dirname(os.path.abspath(__file__)), "app/src/main/java/com/prismwave/app")
bad = 0

def edit(name, pairs):
    global bad
    path = os.path.join(base, name)
    src = open(path, encoding="utf-8").read()
    for pat, rep, rx in pairs:
        n = len(re.findall(pat, src)) if rx else src.count(pat)
        if n != 1:
            print("PROBLEM in " + name + ": found " + str(n) + " times: " + pat[:60])
            bad += 1
            continue
        src = re.sub(pat, lambda m: rep, src) if rx else src.replace(pat, rep)
    open(path, "w", encoding="utf-8").write(src)
    print("done " + name)

INS = "import androidx.compose.foundation.layout.displayCutout\nimport androidx.compose.foundation.layout.statusBarsIgnoringVisibility\nimport androidx.compose.foundation.layout.statusBars\n"

# mini player cover reports where it is
edit("Player.kt", [
    ("Cover(song, Modifier.size(50.dp), 12.dp, 20.sp)",
     "Cover(song, Modifier.size(50.dp).onGloballyPositioned { MiniCover.rect = it.boundsInRoot() }, 12.dp, 20.sp)", False),
])

# covers in playlist rows
edit("Screens.kt", [
    (r'Text\(\s*\(i \+ 1\)\.toString\(\),\s*color = if \(cur\) s\.accent else Color\.White\.copy\(alpha = 0\.6f\),\s*fontSize = 15\.sp,\s*modifier = Modifier\.width\(30\.dp\)\s*\)',
     "Cover(song, Modifier.size(50.dp), 10.dp, 18.sp)\n                        Spacer(Modifier.width(14.dp))", True),
    ("Box(Modifier.padding(start = 50.dp).fillMaxWidth().height(1.dp)",
     "Box(Modifier.padding(start = 84.dp).fillMaxWidth().height(1.dp)", False),
])

# flight animation + theme mode
edit("MainActivity.kt", [
    ("StudioPrefs.init(appCtx) }", "StudioPrefs.init(appCtx); AppColors.init(appCtx) }", False),
    ("LaunchedEffect(artC) { if (cur != null && Opts.livingBg) s.accent = lerp(artC, Color.White, 0.35f) }",
     "LaunchedEffect(artC, AppColors.ver, AppColors.palette) {\n        if (Opts.livingBg) {\n            if (cur != null) s.accent = lerp(artC, Color.White, 0.35f)\n        } else {\n            s.accent = AppColors.palette\n        }\n    }", False),
    ("enter = slideInVertically(tween(520, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(380)),",
     "enter = if (MiniCover.rect != null && !s.reduceMotion) fadeIn(tween(380)) else slideInVertically(tween(520, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(380)),", False),
    (r'\)\s*\{\s*FullPlayer\(s\)\s*\}\s*PagesHost\(s\)',
     ") { FullPlayer(s) }\n            CoverFlight(s)\n            PagesHost(s)", True),
])

# app colors choice
edit("Settings2.kt", [
    ("Gold, Color(0xFF4DB6AC), Color(0xFFE57A8A), Color(0xFF8AB4F8), Color(0xFFB39DDB), Color(0xFFFFA45C)",
     "Gold, Color(0xFF4DB6AC), Color(0xFFE57A8A), Color(0xFF8AB4F8), Color(0xFFB39DDB), Color(0xFFFFA45C), Color(0xFF81C784), Color(0xFFEF5350)", False),
    (r's\.accent = c\s*s\.saveSettings\(\)',
     "AppColors.pick(c)\n                                s.accent = c\n                                s.saveSettings()", True),
    (r'PfAccent\(s\)\s*\}\s*PfLabel\("Player", a\)',
     'PfChoice(\n            Icons.Rounded.AutoAwesome, "App colors", listOf("Color palette", "Follow music theme"), if (Opts.livingBg) 1 else 0, a,\n            "Palette: the colors you pick. Follow music: the app takes the colors of the playing cover"\n        ) {\n            AppColors.setFollow(it == 1)\n            if (it == 0) s.accent = AppColors.palette\n            s.saveSettings()\n        }\n        PfAccent(s)\n    }\n    PfLabel("Player", a)', True),
])

# space for the punch hole above the full lyrics
edit("LyricsStage.kt", [
    ("package com.prismwave.app\n", "package com.prismwave.app\n\n" + INS, False),
    ("val smallTop = WindowInsets.statusBars.getTop(density).toFloat() + 14f * dp",
     "val smallTop = maxOf(WindowInsets.statusBarsIgnoringVisibility.getTop(density), WindowInsets.displayCutout.getTop(density), WindowInsets.statusBars.getTop(density)).toFloat() + 26f * dp", False),
])
edit("LyricsScreen.kt", [
    ("package com.prismwave.app\n", "package com.prismwave.app\n\nimport androidx.compose.foundation.layout.WindowInsets\n" + INS, False),
    (r'Box\(Modifier\.fillMaxSize\(\)\.pointerInput\(Unit\) \{ detectTapGestures \{ \} \}\) \{',
     "val lyTop = with(density) { maxOf(WindowInsets.statusBarsIgnoringVisibility.getTop(this), WindowInsets.displayCutout.getTop(this), WindowInsets.statusBars.getTop(this)).toDp() } + 24.dp\n\n    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }) {", True),
    ("Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {",
     "Column(Modifier.fillMaxSize().padding(top = lyTop).navigationBarsPadding()) {", False),
])

print("ALL GOOD" if bad == 0 else "Some edits failed: send me this screen")
