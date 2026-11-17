import os, shutil, sys

BASE = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")

def read(n):
    with open(BASE + n, encoding="utf-8") as f:
        return f.read()

def write(n, t):
    shutil.copyfile(BASE + n, BASE + n + ".bak22")
    with open(BASE + n, "w", encoding="utf-8") as f:
        f.write(t)

def swap(text, old, new, label):
    c = text.count(old)
    if c != 1:
        print("STOP: could not find the spot for '" + label + "' (found " + str(c) + "). Nothing was changed for this file.")
        sys.exit(1)
    return text.replace(old, new)

NEW_HOME = r'''

// ------------------------------------------------------------ home v22 (new design)

private fun hourNow(): Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

private fun greetingText(): String {
    val h = hourNow()
    return when {
        h < 5 -> "Good Night"
        h < 12 -> "Good Morning"
        h < 18 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

@Composable
private fun HomeHeader2(s: AppState) {
    val day = hourNow() in 5..17
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Home", color = Color.White, fontFamily = PrismFont, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(greetingText(), color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Icon(
                    if (day) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
                    contentDescription = null,
                    tint = s.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Box(
            Modifier
                .bounceClick { s.screen = Screen.Settings }
                .size(56.dp)
                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                .clip(CircleShape)
        ) { AppLogo(56.dp) }
    }
}

@Composable
private fun FeaturedHero(song: Song, from: List<Song>, s: AppState) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(190.dp)
            .softClick { s.play(song, from) }
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
    ) {
        Cover(song, Modifier.fillMaxSize(), 0.dp, 90.sp, quality = 800)
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.72f),
                    0.55f to Color.Black.copy(alpha = 0.25f),
                    1f to Color.Transparent
                )
            )
        )
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.62f).padding(start = 22.dp)) {
            Text("Featured", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                song.title,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(song.artist, color = Color.White.copy(alpha = 0.75f), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.22f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text("Play", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun HomeSection(title: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).clickable(enabled = onClick != null) { onClick?.invoke() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontFamily = PrismFont, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            if (onClick != null) {
                Icon(
                    Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeCard(song: Song, from: List<Song>, s: AppState, modifier: Modifier = Modifier) {
    val cur = s.current?.id == song.id
    Column(modifier.softClick { s.play(song, from) }.width(150.dp)) {
        Cover(song, Modifier.size(150.dp), 20.dp, 44.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            song.title,
            color = if (cur) s.accent else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(song.artist, color = TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HomeShelf(songs: List<Song>, s: AppState) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) { itemsIndexed(songs.take(15)) { i, sg -> HomeCard(sg, songs, s, Modifier.appear(i)) } }
}

@Composable
fun HomeScreen(s: AppState) {
    LaunchedEffect(Opts.offline) {
        if (!Opts.offline) {
            s.loadFeed("quick", "new music hits")
            s.loadFeed("chill", "chill songs")
            s.loadFeed("workout", "workout songs")
        }
    }
    val quick = if (Opts.offline) s.offlineSongs() else s.feeds["quick"]
    val chill = if (Opts.offline) emptyList<Song>() else (s.feeds["chill"] ?: emptyList())
    val workout = if (Opts.offline) emptyList<Song>() else (s.feeds["workout"] ?: emptyList())
    val chips = listOf("Relax", "Workout", "Energize", "Party", "Commute", "Focus", "Sleep")
    val history = s.history.toList()
    val recentIds = history.map { it.id }.toSet()
    val forgotten = s.liked.filter { it.id !in recentIds }
    val speedDial = s.topPlayed()
    val ytPlaylists = s.playlists.filter { it.id.startsWith("yt_") }
    val hero = quick?.firstOrNull() ?: speedDial.firstOrNull() ?: history.firstOrNull()
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(430.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0f to s.accent.copy(alpha = 0.30f),
                        0.42f to s.accent.copy(alpha = 0.12f),
                        1f to Color.Transparent
                    )
                )
        )
        LazyColumn(
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = 220.dp
            )
        ) {
            item { HomeHeader2(s) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Chip("Offline", Opts.offline, s.accent) {
                            Opts.offline = !Opts.offline
                            Opts.save()
                        }
                    }
                    items(chips) { c ->
                        Chip(c, false, s.accent) {
                            s.searchQuery = c + " music"
                            s.screen = Screen.Search
                        }
                    }
                }
            }
            if (hero != null) {
                item { Spacer(Modifier.height(16.dp)) }
                item { FeaturedHero(hero, quick ?: listOf(hero), s) }
            }
            if (history.isNotEmpty() && Opts.showKeep) {
                item {
                    HomeSection("Recently Played") {
                        s.openPlaylist = null
                        s.libraryOpen = "History"
                        s.screen = Screen.Library
                    }
                }
                item { HomeShelf(history, s) }
            }
            if (speedDial.size >= 3 && Opts.showSpeed) {
                item {
                    HomeSection("Speed Dial") {
                        s.openPlaylist = null
                        s.libraryOpen = "My top 50"
                        s.screen = Screen.Library
                    }
                }
                item { HomeShelf(speedDial, s) }
            }
            item { HomeSection("Quick picks") }
            item {
                if (quick == null) LoadingRow(s) else QuickPicksGrid(quick, s)
            }
            if (ytPlaylists.isNotEmpty()) {
                item { HomeSection("Your playlists") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) { items(ytPlaylists.take(12)) { p -> PlaylistCard(p, s) } }
                }
            }
            if (forgotten.size >= 3 && Opts.showForgotten) {
                item { HomeSection("Forgotten favorites") }
                item { HomeShelf(forgotten, s) }
            }
            if (Opts.showMixes) {
                item { SongRowList("Chill", chill, s) }
                item { SongRowList("Workout", workout, s) }
            }
        }
    }
}
'''

EXTRA_IMPORTS = (
    "import androidx.compose.material.icons.rounded.WbSunny\n"
    "import androidx.compose.material.icons.rounded.NightsStay\n"
    "import androidx.compose.material.icons.rounded.KeyboardArrowRight\n"
)

# ---------------- Screens.kt
sc = read("Screens.kt")
sc = swap(sc, "package com.prismwave.app\n", "package com.prismwave.app\n\n" + EXTRA_IMPORTS, "imports")
sc = swap(sc, "fun HomeScreen(s: AppState) {", "private fun HomeScreenOld(s: AppState) {", "old home")
sc = swap(sc, 'Section("v21") {', '''Section("v22") {
                        ChangeList(listOf(
                            "New Home design: greeting header, Featured card, Recently Played and Speed Dial shelves",
                            "Lighter glass on the bars: the blur runs on a quarter-size copy, same look, much less work",
                            "Softer highlight on the glass edge"
                        ))
                    }
                    Section("v21") {''', "changelog")
sc = swap(sc, 'Text("Version v21", color = Color.White)', 'Text("Version v22", color = Color.White)', "version")
sc = sc.rstrip("\n") + "\n" + NEW_HOME

# ---------------- Components.kt (glass)
co = read("Components.kt")
co = swap(co, "import androidx.compose.ui.graphics.drawscope.translate\n",
          "import androidx.compose.ui.graphics.drawscope.translate\nimport androidx.compose.ui.graphics.drawscope.scale\n", "scale import")
co = swap(co, """                    blurLayer.renderEffect = BlurEffect(backdrop.radius, backdrop.radius, TileMode.Clamp)
                    blurLayer.clip = true
                    blurLayer.record(this, layoutDirection, IntSize(size.width.toInt(), size.height.toInt())) {
                        translate(-pos.x, -pos.y) { drawLayer(backdrop.layer) }
                    }
                    drawLayer(blurLayer)
""", """                    // blur a quarter-size copy and scale it back up: about 16x less work for the GPU
                    val k = 0.25f
                    blurLayer.renderEffect = BlurEffect(backdrop.radius * k, backdrop.radius * k, TileMode.Clamp)
                    blurLayer.clip = true
                    blurLayer.record(
                        this, layoutDirection,
                        IntSize((size.width * k).toInt().coerceAtLeast(1), (size.height * k).toInt().coerceAtLeast(1))
                    ) {
                        scale(k, k, Offset.Zero) { translate(-pos.x, -pos.y) { drawLayer(backdrop.layer) } }
                    }
                    scale(1f / k, 1f / k, Offset.Zero) { drawLayer(blurLayer) }
""", "glass blur")
co = swap(co, ".border(1.dp, Color.White.copy(alpha = 0.08f), shape),\n        content = content",
          ".border(\n                1.dp,\n                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.05f))),\n                shape\n            ),\n        content = content",
          "glass border")

write("Screens.kt", sc)
write("Components.kt", co)
print("v22 patch applied: new Home + lighter glass. Now: cd ~/PrismWave && git add -A && git commit -m v22 && git push")
