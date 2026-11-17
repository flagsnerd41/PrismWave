import os, re, sys

D = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
names = ["Player.kt", "Screens.kt", "Components.kt", "MainActivity.kt"]
files = {n: open(D + n, encoding="utf-8").read() for n in names}


def rep(n, old, new):
    if old not in files[n]:
        sys.exit("MISSING in " + n + ": " + old[:80])
    files[n] = files[n].replace(old, new)


def cut(n, start, end, new):
    t = files[n]
    i = t.find(start)
    if i < 0:
        sys.exit("MISSING start in " + n + ": " + start[:80])
    j = len(t) if end is None else t.find(end, i)
    if j < 0:
        sys.exit("MISSING end in " + n + ": " + end[:80])
    files[n] = t[:i] + new + t[j:]


def imports(n, text):
    files[n] = files[n].replace("package com.prismwave.app\n", "package com.prismwave.app\n\n" + text, 1)


# ---------------- Components.kt: pop-ups become pages, haptics/bounce options
rep("Components.kt", "targetValue = if (pressed) scaleTo else 1f,", "targetValue = if (pressed && Opts.bounce) scaleTo else 1f,")
rep("Components.kt", "haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)",
    "if (Opts.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)")
cut("Components.kt", "@Composable\nfun NamePlaylistDialog(", "@Composable\nfun AddToPlaylistDialog(",
    "@Composable\nfun NamePlaylistDialog(\n    title: String,\n    initial: String,\n    onConfirm: (String) -> Unit,\n    onDismiss: () -> Unit\n) {\n    LaunchedEffect(Unit) { Pages.askName(title, initial, onConfirm, onDismiss) }\n}\n\n")
cut("Components.kt", "@Composable\nfun AddToPlaylistDialog(", "@Composable\nfun SongMenu(",
    "@Composable\nfun AddToPlaylistDialog(song: Song, s: AppState, onDone: () -> Unit) {\n    LaunchedEffect(Unit) {\n        Pages.show(\"add\", song)\n        onDone()\n    }\n}\n\n")
cut("Components.kt", "@Composable\nfun SongMenu(", "@Composable\nfun SongRow(",
    "@Composable\nfun SongMenu(song: Song, s: AppState, playlistId: String?, onDismiss: () -> Unit) {\n    LaunchedEffect(Unit) {\n        Pages.show(\"songmenu\", song, playlistId)\n        onDismiss()\n    }\n}\n\n")

# ---------------- Player.kt
imports("Player.kt",
    "import androidx.compose.animation.slideInHorizontally\n"
    "import androidx.compose.animation.slideOutHorizontally\n"
    "import androidx.compose.animation.core.FastOutSlowInEasing\n"
    "import androidx.compose.material.icons.rounded.ArrowBack\n")
rep("Player.kt", "fun TopBlur(height: Dp) {\n    val backdrop = LocalBackdrop.current",
    "fun TopBlur(height: Dp) {\n    if (!Opts.statusBlur) return\n    val backdrop = LocalBackdrop.current")
# queue + sleep dialogs -> pages
cut("Player.kt", "    if (queueDialog) {\n        AlertDialog(",
    "@OptIn(ExperimentalFoundationApi::class)\n@Composable\nprivate fun TitleRow(",
    "    if (queueDialog) {\n        LaunchedEffect(Unit) {\n            Pages.show(\"queue\")\n            queueDialog = false\n        }\n    }\n"
    "    if (sleepDialog) {\n        LaunchedEffect(Unit) {\n            Pages.show(\"sleep\")\n            sleepDialog = false\n        }\n    }\n}\n\n")
# options bottom sheet -> full slide-in page with blurred cover
NEW_A = '''        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200))
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Night)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
            ) {
                Cover(song, Modifier.fillMaxSize().blur(60.dp), 0.dp, 120.sp, quality = 400)
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)))
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundBtn(Icons.Rounded.ArrowBack, 0.9f, { close() })
                    Spacer(Modifier.width(14.dp))
                    Text("Options", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
'''
cut("Player.kt",
    "        AnimatedVisibility(visible = visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {\n            Box(",
    "                Row(\n                    Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(CardBg).padding(16.dp),",
    NEW_A)
rep("Player.kt",
    ") { tempoOpen = true }\n                }\n            }\n        }\n    }\n    if (tempoOpen) TempoPitchDialog(s) { tempoOpen = false }\n",
    ") { Pages.show(\"tempo\") }\n                }\n            }\n            }\n        }\n    }\n")
# details dialog -> page, old tempo dialog removed (end of file)
cut("Player.kt", "    if (details) {\n        AlertDialog(", None,
    "    if (details) {\n        LaunchedEffect(Unit) {\n            Pages.show(\"details\", song)\n            details = false\n        }\n    }\n}\n")

# ---------------- MainActivity.kt
rep("MainActivity.kt", "val s = remember { AppState(appCtx) }",
    "remember { Opts.init(appCtx) }\n            val s = remember { AppState(appCtx) }")
rep("MainActivity.kt", "visible = s.screen == Screen.Home,", "visible = true,")
files["MainActivity.kt"], k = re.subn(
    r"Modifier\.fillMaxSize\(\)\.then\(\s*if \(scr == Screen\.Library && s\.openPlaylist != null\) Modifier else Modifier\.statusBarsPadding\(\)\s*\)",
    "Modifier.fillMaxSize()", files["MainActivity.kt"])
if k != 1:
    sys.exit("MISSING in MainActivity.kt: statusBarsPadding block")
rep("MainActivity.kt", "val artC = if (cur != null) rememberArtColor(cur) else s.accent",
    "val artC = if (cur != null && Opts.livingBg) rememberArtColor(cur) else s.accent")
rep("MainActivity.kt", "val strength = if (cur != null) 1f else 0.5f",
    "val strength = if (cur != null && Opts.livingBg) 1f else 0.5f")
rep("MainActivity.kt", "            if (s.showLogin) LoginScreen(s)", "            PagesHost(s)\n            if (s.showLogin) LoginScreen(s)")

# ---------------- Screens.kt
imports("Screens.kt",
    "import androidx.compose.material.icons.rounded.TouchApp\n"
    "import androidx.compose.material.icons.rounded.Home\n"
    "import androidx.compose.material.icons.rounded.Info\n")
# stats dialog -> page
cut("Screens.kt", "    if (stats) {\n        AlertDialog(", "@Composable\nfun LoadingRow",
    "    if (stats) {\n        LaunchedEffect(Unit) {\n            Pages.show(\"stats\")\n            stats = false\n        }\n    }\n}\n\n")
# playlist menu dialog -> page
cut("Screens.kt", "    if (menu) {\n        AlertDialog(", "    if (renaming) {\n        NamePlaylistDialog(\"Rename playlist\"",
    "    if (menu) {\n        LaunchedEffect(Unit) {\n            Pages.show(\"plmenu\", null, pl.id)\n            menu = false\n        }\n    }\n")
# content scrolls under the blurred status bar on every screen
OLD_LC = "LazyColumn(contentPadding = PaddingValues(bottom = 220.dp)) {"
if OLD_LC not in files["Screens.kt"]:
    sys.exit("MISSING LazyColumn padding")
files["Screens.kt"] = files["Screens.kt"].replace(OLD_LC, "LazyColumn(contentPadding = PaddingValues(top = statusTop(), bottom = 220.dp)) {")
rep("Screens.kt", "Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 220.dp)) {",
    "Column(Modifier.verticalScroll(rememberScrollState()).padding(top = statusTop(), bottom = 220.dp)) {")
# home sections can be switched off
rep("Screens.kt", "if (history.isNotEmpty()) {", "if (history.isNotEmpty() && Opts.showKeep) {")
rep("Screens.kt", "if (speedDial.size >= 3) {", "if (speedDial.size >= 3 && Opts.showSpeed) {")
rep("Screens.kt", "if (forgotten.size >= 3) {", "if (forgotten.size >= 3 && Opts.showForgotten) {")
rep("Screens.kt",
    "            item { SongRowList(\"Chill\", chill, s) }\n            item { SongRowList(\"Workout\", workout, s) }",
    "            if (Opts.showMixes) {\n                item { SongRowList(\"Chill\", chill, s) }\n                item { SongRowList(\"Workout\", workout, s) }\n            }")
# settings: more accent colors, new pages
rep("Screens.kt", "listOf(Gold, Color(0xFF4DB6AC), Color(0xFFE57A8A), Color(0xFF8AB4F8)).forEach { c ->",
    "listOf(Gold, Color(0xFF4DB6AC), Color(0xFFE57A8A), Color(0xFF8AB4F8), Color(0xFFB39DDB), Color(0xFFFFA45C)).forEach { c ->")
rep("Screens.kt", "SettingsItem(\"backup\", \"Backup & restore\", \"Save or bring back your library\", Icons.Rounded.Folder)",
    "SettingsItem(\"behavior\", \"Behavior\", \"Haptics, taps, and look extras\", Icons.Rounded.TouchApp),\n"
    "                    SettingsItem(\"content\", \"Content\", \"Choose what shows on Home\", Icons.Rounded.Home),\n"
    "                    SettingsItem(\"backup\", \"Backup & restore\", \"Save or bring back your library\", Icons.Rounded.Folder),\n"
    "                    SettingsItem(\"about\", \"About\", \"Version and build info\", Icons.Rounded.Info)")
rep("Screens.kt", "\"backup\" to \"Backup & restore\"",
    "\"backup\" to \"Backup & restore\",\n        \"behavior\" to \"Behavior\",\n        \"content\" to \"Content\",\n        \"about\" to \"About\"")
rep("Screens.kt", "@Composable\nfun SettingsScreen(s: AppState) {",
    '''@Composable
private fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White)
            Text(sub, color = TextDim, fontSize = 13.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsScreen(s: AppState) {''')
NEW_PAGES = '''                "behavior" -> {
                    Section("Feel") {
                        ToggleRow("Haptic feedback", "A small tick when you tap", Opts.haptics) { Opts.haptics = it; Opts.save() }
                        ToggleRow("Bounce on tap", "Buttons squish when pressed", Opts.bounce) { Opts.bounce = it; Opts.save() }
                    }
                    Section("Look") {
                        ToggleRow("Blur under status bar", "Blurred strip on every screen", Opts.statusBlur) { Opts.statusBlur = it; Opts.save() }
                        ToggleRow("Living background", "Tint the app with the song's cover color", Opts.livingBg) { Opts.livingBg = it; Opts.save() }
                    }
                }
                "content" -> {
                    Section("Home sections") {
                        ToggleRow("Keep listening", "Your recent songs", Opts.showKeep) { Opts.showKeep = it; Opts.save() }
                        ToggleRow("Speed dial", "Your most played", Opts.showSpeed) { Opts.showSpeed = it; Opts.save() }
                        ToggleRow("Forgotten favorites", "Liked songs you haven't played lately", Opts.showForgotten) { Opts.showForgotten = it; Opts.save() }
                        ToggleRow("Chill and Workout", "Mix shelves at the bottom", Opts.showMixes) { Opts.showMixes = it; Opts.save() }
                    }
                }
                "about" -> {
                    Section("PrismWave") {
                        Text("Version v12", color = Color.White)
                        Text("Private build, made from scratch", color = TextDim, fontSize = 13.sp)
                    }
                }
                "backup" -> {'''
rep("Screens.kt", "\"backup\" -> {", NEW_PAGES.lstrip())

for n in names:
    open(D + n, "w", encoding="utf-8").write(files[n])
print("v12 patch applied")
