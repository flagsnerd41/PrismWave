#!/usr/bin/env python3
# added About PrismWave page, pinned header with fading blur, new playlist design
# run from anywhere:  python3 ~/PrismWave/patch_added_about_header_playlist.py
import os
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


files = {}

# ---------------------------------------------------------------- Screens.kt
t = load("Screens.kt")

# Home: the header is pinned now, so the list only keeps an empty space where it was
t = must_replace(
    t,
    "            item { Header(s) }\n"
    "            item {\n"
    "                LazyRow(\n"
    "                    contentPadding = PaddingValues(horizontal = 20.dp),\n"
    "                    horizontalArrangement = Arrangement.spacedBy(10.dp)\n"
    "                ) {\n"
    "                    items(moods) { c ->",
    "            item { Spacer(Modifier.height(PinnedHeaderHeight)) }\n"
    "            item {\n"
    "                LazyRow(\n"
    "                    contentPadding = PaddingValues(horizontal = 20.dp),\n"
    "                    horizontalArrangement = Arrangement.spacedBy(10.dp)\n"
    "                ) {\n"
    "                    items(moods) { c ->",
    "Home header",
)

# Search: space moves into the list padding so the sticky filter chips stop under the header
t = must_replace(
    t,
    "    LazyColumn(contentPadding = PaddingValues(top = statusTop(), bottom = 220.dp)) {\n"
    "        item { Header(s) }\n"
    "        item {\n"
    "            OutlinedTextField(\n"
    "                value = s.searchQuery,",
    "    LazyColumn(contentPadding = PaddingValues(top = statusTop() + PinnedHeaderHeight, bottom = 220.dp)) {\n"
    "        item {\n"
    "            OutlinedTextField(\n"
    "                value = s.searchQuery,",
    "Search header",
)

# Library main page
t = must_replace(
    t,
    "        item { Header(s) }\n"
    "        item {\n"
    "            Column(Modifier.padding(horizontal = 24.dp).padding(top = 4.dp)) {",
    "        item { Spacer(Modifier.height(PinnedHeaderHeight)) }\n"
    "        item {\n"
    "            Column(Modifier.padding(horizontal = 24.dp).padding(top = 4.dp)) {",
    "Library header",
)

# new playlist page
t = must_replace(
    t,
    'p.startsWith("pl:") -> PlaylistScreen(s, p.removePrefix("pl:"))',
    'p.startsWith("pl:") -> PlaylistScreen2(s, p.removePrefix("pl:"))',
    "Playlist call",
)
files["Screens.kt"] = t

# ---------------------------------------------------------------- MainActivity.kt
t = load("MainActivity.kt")
anchor = (
    "            Column(\n"
    "                Modifier\n"
    "                    .align(Alignment.BottomCenter)\n"
    "                    .navigationBarsPadding()"
)
t = must_replace(
    t,
    anchor,
    "            if (headerShown(s)) {\n"
    "                Box(Modifier.align(Alignment.TopCenter)) { PinnedHeader(s) }\n"
    "            }\n" + anchor,
    "Main pinned header",
)
files["MainActivity.kt"] = t

# ---------------------------------------------------------------- Settings2.kt
t = load("Settings2.kt")

t = must_replace(
    t,
    '        "about" to "About"\n    )',
    '        "about" to "About PrismWave"\n    )',
    "About title",
)
t = must_replace(
    t,
    'PfItem("about", "About", "Version and build info", Icons.Rounded.Info)',
    'PfItem("about", "About PrismWave", "Version, updates and credits", Icons.Rounded.Info)',
    "About list item",
)

new_about = '''@Composable
private fun PfAboutPage(s: AppState) {
    val a = s.accent
    val uctx = LocalContext.current
    val uscope = rememberCoroutineScope()
    val build = (Updater.currentCode(uctx) - 100).coerceAtLeast(0)
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(28.dp))
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.about_cat),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    0.35f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.85f)
                )
            )
        )
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppLogo(80.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                "PrismWave",
                color = Color.White,
                fontFamily = RetroBlendy,
                fontSize = 40.sp,
                letterSpacing = 0.6.sp
            )
            Text("made by flagsnerd", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp)
        }
    }
    PfLabel("Version", a)
    PfGroup {
        PfInfo(Icons.Rounded.Info, "Current version", "1.5", a)
        PfAction(
            Icons.Rounded.Sync, "Check for updates",
            if (Updater.status.isNotEmpty()) Updater.status else "Look for a newer version", a,
            Updater.status != "Checking..."
        ) { uscope.launch { Updater.checkNow(uctx) } }
        PfNote("Private build, made from scratch. Build " + build)
    }
}

'''
i0 = t.find("@Composable\nprivate fun PfAboutPage(s: AppState) {")
i1 = t.find("@Composable\nprivate fun PfChangelogPage(s: AppState) {")
if i0 < 0 or i1 < 0 or i1 < i0:
    print("FAILED: About page markers (" + str(i0) + ", " + str(i1) + ")")
    sys.exit(1)
t = t[:i0] + new_about + t[i1:]

t = must_replace(
    t,
    '    val a = s.accent\n    PfChange("Added ArchiveTune lyrics look", a, listOf(',
    '    val a = s.accent\n'
    '    PfChange("Added About page, pinned header and new playlist design", a, listOf(\n'
    '        "About PrismWave in Settings: cat picture, logo, Retro Blendy name, version 1.5 and Check for updates",\n'
    '        "The Home, Search and Library header stays on top while you scroll, with a fading blur under it",\n'
    '        "Playlist page like ArchiveTune: full cover on top, big title, round buttons with a Play pill"\n'
    '    ))\n'
    '    PfChange("Added ArchiveTune lyrics look", a, listOf(',
    "Changelog",
)
files["Settings2.kt"] = t

# ---------------------------------------------------------------- Extras.kt (red dot on What's new)
t = load("Extras.kt")
t = must_replace(
    t,
    'const val ID = "added-ui-lyrics-video"',
    'const val ID = "added-about-header-playlist"',
    "WhatsNew id",
)
files["Extras.kt"] = t

# everything matched, now write
for name, text in files.items():
    with open(os.path.join(ROOT, name), "w", encoding="utf-8") as f:
        f.write(text)
    print("patched " + name)
print("done")
