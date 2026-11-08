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

# ---------------- Video.kt: smoother sync (the stutter fix, part 1)
p = os.path.join(app, "Video.kt")
t = read(p)
if "val sp = when" not in t:
    t = once(t,
        "        val ps = listOfNotNull(main, back)\n        return ps.isNotEmpty() && ps.all { it.playbackState == Player.STATE_READY }\n",
        "        val m = main\n        return m != null && m.playbackState == Player.STATE_READY\n",
        "Video gate")
    t = once(t,
        "                if (state == Player.STATE_BUFFERING) {\n"
        "                    VideoSync.main?.playWhenReady = false\n"
        "                    VideoSync.back?.playWhenReady = false\n"
        "                }\n",
        "                if (state == Player.STATE_BUFFERING && !low) {\n"
        "                    VideoSync.back?.playWhenReady = false\n"
        "                }\n",
        "Video buffering")
    t = once(t,
        "            if (player.playbackState != Player.STATE_IDLE && now - lastSeek > 1500) {\n",
        "            if (player.playbackState != Player.STATE_IDLE && now - lastSeek > 4000) {\n",
        "Video cooldown")
    t = once(t,
        "                val limit = if (linked) 0.2f else 0.8f\n"
        "                val drift = player.currentPosition / 1000f - target\n"
        "                if (abs(drift) > limit) {\n"
        "                    player.seekTo((target * 1000).toLong())\n"
        "                    lastSeek = now\n"
        "                }\n",
        "                val limit = if (linked) 0.9f else 1.5f\n"
        "                val drift = player.currentPosition / 1000f - target\n"
        "                if (abs(drift) > limit) {\n"
        "                    player.seekTo((target * 1000).toLong())\n"
        "                    player.setPlaybackSpeed(1f)\n"
        "                    lastSeek = now\n"
        "                } else {\n"
        "                    // small differences are fixed by playing a little faster or slower, no jump\n"
        "                    val sp = when {\n"
        "                        drift < -0.25f -> 1.06f\n"
        "                        drift > 0.25f -> 0.94f\n"
        "                        else -> 1f\n"
        "                    }\n"
        "                    if (player.playbackParameters.speed != sp) player.setPlaybackSpeed(sp)\n"
        "                }\n",
        "Video drift")
    write(p, t)
    print("Video.kt patched")
else:
    print("Video.kt already patched")

# ---------------- Player.kt: no blur and no mask on the video (the stutter fix, part 2)
p = os.path.join(app, "Player.kt")
t = read(p)
if "VideoPrefs.liveBackdrop" not in t:
    t = once(t,
        "        if (VideoSync.showVideo(song)) {\n            VideoStage(s, song, Modifier.fillMaxSize().blur(50.dp), low = true)\n",
        "        if (VideoSync.showVideo(song) && VideoPrefs.liveBackdrop) {\n            VideoStage(s, song, Modifier.fillMaxSize().blur(50.dp), low = true)\n",
        "Player backdrop")
    t = once(t,
        "                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }\n"
        "                .drawWithContent {\n"
        "                    drawContent()\n"
        "                    drawRect(brush = stageFade, blendMode = BlendMode.DstIn)\n"
        "                }\n"
        "        ) {\n"
        "            if (VideoSync.showVideo(song)) {\n"
        "                VideoStage(s, song, Modifier.fillMaxSize())\n",
        "                .then(\n"
        "                    if (VideoSync.showVideo(song)) Modifier else Modifier\n"
        "                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }\n"
        "                        .drawWithContent {\n"
        "                            drawContent()\n"
        "                            drawRect(brush = stageFade, blendMode = BlendMode.DstIn)\n"
        "                        }\n"
        "                )\n"
        "        ) {\n"
        "            if (VideoSync.showVideo(song)) {\n"
        "                VideoStage(s, song, Modifier.fillMaxSize())\n"
        "                // a plain gradient on top instead of cutting the video with a mask every frame\n"
        "                Box(\n"
        "                    Modifier.fillMaxSize().background(\n"
        "                        Brush.verticalGradient(\n"
        "                            0.44f to Color.Transparent,\n"
        "                            0.60f to bottom.copy(alpha = 0.30f),\n"
        "                            0.80f to bottom.copy(alpha = 0.80f),\n"
        "                            1f to bottom\n"
        "                        )\n"
        "                    )\n"
        "                )\n",
        "Player stage")
    write(p, t)
    print("Player.kt patched")
else:
    print("Player.kt already patched")

# ---------------- VideoBoost.kt: hide the status bar while the full player is open
p = os.path.join(app, "VideoBoost.kt")
t = read(p)
if "StatusBarHider" not in t:
    t = once(t,
        "    val open = s.showPlayer\n",
        "    val open = s.showPlayer\n    VideoPrefs.init(ctx)\n    StatusBarHider(open)\n",
        "VideoBoost")
    write(p, t)
    print("VideoBoost.kt patched: status bar hides in the full player")
else:
    print("VideoBoost.kt already patched")

# ---------------- Screens.kt: header button, playlist collage, search extras
p = os.path.join(app, "Screens.kt")
t = read(p)
if "WhatsNewButton" not in t:
    t = once(t,
        "        RoundBtn(Icons.Rounded.BarChart, 0.9f, { stats = true }, 46.dp)\n        Spacer(Modifier.width(8.dp))\n",
        "        RoundBtn(Icons.Rounded.BarChart, 0.9f, { stats = true }, 46.dp)\n        Spacer(Modifier.width(8.dp))\n"
        "        WhatsNewButton(s)\n        Spacer(Modifier.width(8.dp))\n",
        "Header button")
    t = once(t,
        "Cover(first, Modifier.size(144.dp), 20.dp, 44.sp)",
        "PlaylistArt(p, first, 144.dp, 20.dp)",
        "Playlist card art")
    t = once(t,
        "Cover(first, Modifier.size(60.dp), 14.dp)",
        "PlaylistArt(p, first, 60.dp, 14.dp)",
        "Playlist row art")
    t = once(t,
        "@Composable\nfun SearchScreen(s: AppState) {\n",
        "@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)\n@Composable\nfun SearchScreen(s: AppState) {\n",
        "Search optin")
    t = once(t,
        "    var tab by remember { mutableStateOf(0) }\n",
        "    var tab by remember { mutableStateOf(0) }\n"
        "    val searchCtx = LocalContext.current\n"
        "    SearchHistory.init(searchCtx)\n"
        "    var sugg by remember { mutableStateOf<List<String>>(emptyList()) }\n"
        "    LaunchedEffect(s.searchQuery) {\n"
        "        val sq = s.searchQuery.trim()\n"
        "        if (sq.length < 2 || Opts.offline) {\n"
        "            sugg = emptyList()\n"
        "            return@LaunchedEffect\n"
        "        }\n"
        "        delay(250)\n"
        "        sugg = Suggest.fetch(sq)\n"
        "        delay(1300)\n"
        "        SearchHistory.add(sq)\n"
        "    }\n",
        "Search state")
    t = once(t,
        "        if (!searching) {\n            item {\n                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {\n",
        "        if (!searching) {\n"
        "            if (SearchHistory.items.isNotEmpty() && s.searchQuery.isBlank()) {\n"
        "                item { SectionTitle(\"Recent searches\", \"Clear\") { SearchHistory.clear() } }\n"
        "                items(SearchHistory.items.take(6)) { rq ->\n"
        "                    Row(\n"
        "                        Modifier\n"
        "                            .fillMaxWidth()\n"
        "                            .clickable { s.searchQuery = rq }\n"
        "                            .padding(horizontal = 20.dp, vertical = 12.dp),\n"
        "                        verticalAlignment = Alignment.CenterVertically\n"
        "                    ) {\n"
        "                        Icon(Icons.Rounded.History, contentDescription = null, tint = TextDim, modifier = Modifier.size(22.dp))\n"
        "                        Spacer(Modifier.width(14.dp))\n"
        "                        Text(rq, color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))\n"
        "                        Icon(\n"
        "                            Icons.Rounded.Close, contentDescription = \"Remove\", tint = TextDim,\n"
        "                            modifier = Modifier.size(22.dp).clickable { SearchHistory.remove(rq) }\n"
        "                        )\n"
        "                    }\n"
        "                }\n"
        "            }\n"
        "            item {\n                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {\n",
        "Search recents")
    chips_old = (
        "            item {\n"
        "                LazyRow(\n"
        "                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),\n"
        "                    horizontalArrangement = Arrangement.spacedBy(10.dp)\n"
        "                ) {\n"
        "                    itemsIndexed(listOf(\"All\", \"Songs\", \"Videos\", \"Albums\", \"Artists\", \"Playlists\")) { i, label ->\n"
        "                        Chip(label, kind == i, s.accent) { kind = i }\n"
        "                    }\n"
        "                }\n"
        "            }\n"
    )
    chips_new = (
        "            if (sugg.isNotEmpty()) {\n"
        "                item {\n"
        "                    LazyRow(\n"
        "                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),\n"
        "                        horizontalArrangement = Arrangement.spacedBy(8.dp)\n"
        "                    ) {\n"
        "                        items(sugg.take(8)) { sq ->\n"
        "                            Row(\n"
        "                                Modifier\n"
        "                                    .clip(RoundedCornerShape(50))\n"
        "                                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))\n"
        "                                    .clickable { s.searchQuery = sq }\n"
        "                                    .padding(horizontal = 14.dp, vertical = 8.dp),\n"
        "                                verticalAlignment = Alignment.CenterVertically\n"
        "                            ) {\n"
        "                                Icon(Icons.Rounded.Search, contentDescription = null, tint = TextDim, modifier = Modifier.size(16.dp))\n"
        "                                Spacer(Modifier.width(6.dp))\n"
        "                                Text(sq, color = Color.White, fontSize = 14.sp, maxLines = 1)\n"
        "                            }\n"
        "                        }\n"
        "                    }\n"
        "                }\n"
        "            }\n"
        "            stickyHeader {\n"
        "                Box(Modifier.fillMaxWidth().background(Night.copy(alpha = 0.94f))) {\n"
        "                    LazyRow(\n"
        "                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),\n"
        "                        horizontalArrangement = Arrangement.spacedBy(10.dp)\n"
        "                    ) {\n"
        "                        itemsIndexed(listOf(\"All\", \"Songs\", \"Videos\", \"Albums\", \"Artists\", \"Playlists\")) { i, label ->\n"
        "                            Chip(label, kind == i, s.accent) { kind = i }\n"
        "                        }\n"
        "                    }\n"
        "                }\n"
        "            }\n"
    )
    t = once(t, chips_old, chips_new, "Search chips")
    t = add_imports(t, [
        "androidx.compose.foundation.lazy.stickyHeader",
        "androidx.compose.material.icons.rounded.Close",
    ])
    write(p, t)
    print("Screens.kt patched: header button, collage, search extras")
else:
    print("Screens.kt already patched")

# ---------------- Settings2.kt: open on a page, Transitions page, changelog
p = os.path.join(app, "Settings2.kt")
t = read(p)
if "SettingsNav.take()" not in t:
    t = once(t,
        "    var page by remember { mutableStateOf<String?>(null) }\n",
        "    var page by remember { mutableStateOf<String?>(SettingsNav.take()) }\n",
        "Settings start page")
page = read(os.path.join(here, "transitions_page.txt"))
i = t.find("@Composable\nprivate fun PfTransitionsPage(")
if i < 0:
    fail("Settings2.kt: Transitions page not found")
t = t[:i].rstrip() + "\n\n" + page
key = '    PfChange("Added Home layout'
if "Added lyrics upgrade" not in t and t.count(key) == 1:
    entry = (
        '    PfChange("Added lyrics upgrade, fixed video stutter, added UI extras", a, listOf(\n'
        '        "Lyrics: the playing line fills word by word, pause dots, moving background, sync fix, text size and alignment",\n'
        '        "Lyrics: better song matching, and lyrics are saved on the phone",\n'
        '        "Fixed video stutter: no more blur and mask on the video every frame",\n'
        '        "The status bar hides in the full player",\n'
        '        "What\'s new button in the Home header",\n'
        '        "Playlist covers show a 2 by 2 collage",\n'
        '        "Search: recent searches, live suggestions and chips that stay at the top"\n'
        '    ))\n'
    )
    t = t.replace(key, entry + key, 1)
write(p, t)
print("Settings2.kt patched")
print("All done. Now: git add -A && git commit && git push")
