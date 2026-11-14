import os, re
base = os.path.join(os.path.dirname(os.path.abspath(__file__)), "app/src/main/java/com/prismwave/app")
bad = 0

def edit(name, pairs):
    global bad
    path = os.path.join(base, name)
    src = open(path, encoding="utf-8").read()
    for pat, rep, rx in pairs:
        n = len(re.findall(pat, src)) if rx else src.count(pat)
        if n != 1:
            print("PROBLEM in " + name + ": found " + str(n) + " times: " + pat[:55])
            bad += 1
            continue
        src = re.sub(pat, lambda m: rep, src) if rx else src.replace(pat, rep)
    open(path, "w", encoding="utf-8").write(src)
    print("done " + name)

# covers that fail to load (the big letter) fall back to the normal picture
edit("Components.kt", [
    (r'val req = remember\(thumb, quality\) \{\s*ImageRequest\.Builder\(ctx\)\.data\(artUrl\(thumb, quality\)\)\.crossfade\(350\)\.build\(\)\s*\}\s*AsyncImage\(\s*model = req,',
     "var failed by remember(thumb, quality) { mutableStateOf(false) }\n            val req = remember(thumb, quality, failed) {\n                ImageRequest.Builder(ctx).data(if (failed) thumb else artUrl(thumb, quality)).crossfade(350).build()\n            }\n            AsyncImage(\n                model = req,\n                onError = { failed = true },", True),
])

# mini player title slides up when the song changes
edit("Player.kt", [
    (r'Text\(\s*song\.title,\s*color = Color\.White,\s*fontWeight = FontWeight\.Bold,\s*fontSize = 17\.sp,\s*maxLines = 1,\s*overflow = TextOverflow\.Ellipsis\s*\)',
     'AnimatedContent(\n                            targetState = song.title,\n                            modifier = Modifier.clip(RoundedCornerShape(2.dp)),\n                            transitionSpec = {\n                                (slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(300))) togetherWith\n                                    (slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(160)))\n                            },\n                            label = "minititle"\n                        ) { t ->\n                            Text(\n                                t,\n                                color = Color.White,\n                                fontWeight = FontWeight.Bold,\n                                fontSize = 17.sp,\n                                maxLines = 1,\n                                overflow = TextOverflow.Ellipsis\n                            )\n                        }', True),
    (r'Text\(\s*if \(s\.status\.isNotEmpty\(\)\) s\.status else song\.artist,\s*color = TextDim,\s*fontSize = 14\.sp,\s*maxLines = 1,\s*overflow = TextOverflow\.Ellipsis\s*\)',
     'AnimatedContent(\n                            targetState = if (s.status.isNotEmpty()) s.status else song.artist,\n                            transitionSpec = { fadeIn(tween(320, delay = 120)) togetherWith fadeOut(tween(140)) },\n                            label = "miniartist"\n                        ) { t ->\n                            Text(t, color = TextDim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)\n                        }', True),
])

# home: Retro Blendy title, tapping a song grows the player out of its cover
edit("Screens.kt", [
    (r'fontFamily = PrismFont,\s*fontSize = 27\.sp,',
     "fontFamily = RetroBlendy,\n            fontSize = 28.sp,", True),
    ("val cur = s.current?.id == sg.id",
     "val cur = s.current?.id == sg.id\n            val cref = remember { CoverRef() }", False),
    (".clickable { s.play(sg, songs) }", ".clickable { openFrom(s, sg, songs, cref, 12f) }", False),
    ("Cover(sg, Modifier.size(54.dp), 12.dp, 18.sp)", "Cover(sg, Modifier.size(54.dp).trackCover(cref), 12.dp, 18.sp)", False),
    (r'Column\(modifier\.softClick \{ s\.play\(song, from\) \}\.width\(150\.dp\)\) \{\s*Cover\(song, Modifier\.size\(150\.dp\), 20\.dp, 44\.sp\)',
     "val cref = remember { CoverRef() }\n    Column(modifier.softClick { openFrom(s, song, from, cref, 20f) }.width(150.dp)) {\n        Cover(song, Modifier.size(150.dp).trackCover(cref), 20.dp, 44.sp)", True),
    (r'Column\(modifier\.softClick \{ s\.play\(song, from\) \}\.width\(140\.dp\)\) \{\s*Box \{\s*Cover\(song, Modifier\.size\(140\.dp\), 24\.dp, 44\.sp\)',
     "val cref = remember { CoverRef() }\n    Column(modifier.softClick { openFrom(s, song, from, cref, 24f) }.width(140.dp)) {\n        Box {\n            Cover(song, Modifier.size(140.dp).trackCover(cref), 24.dp, 44.sp)", True),
    (r'val shape = RoundedCornerShape\(32\.dp\)',
     "val shape = RoundedCornerShape(32.dp)\n    val cref = remember { CoverRef() }", True),
    (r'\.softClick \{ s\.play\(song, from\) \}\s*\.clip\(shape\)\s*\.border\(1\.dp, Color\.White\.copy\(alpha = 0\.1f\), shape\)',
     ".softClick { openFrom(s, song, from, cref, 32f) }\n            .clip(shape)\n            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)", True),
    (r'Cover\(song, Modifier\.fillMaxSize\(\), 0\.dp, 90\.sp, quality = 800\)(?=\s*Box\(\s*Modifier\.fillMaxSize\(\)\.background\(\s*Brush\.verticalGradient\(\s*0\.45f)',
     "Cover(song, Modifier.fillMaxSize().trackCover(cref), 0.dp, 90.sp, quality = 800)", True),
])

# the player fades in (instead of sliding) when a flight is coming
edit("MainActivity.kt", [
    ("if (MiniCover.rect != null && !s.reduceMotion) fadeIn(tween(380))",
     "if ((MiniCover.rect != null || TapCover.rect != null) && !s.reduceMotion) fadeIn(tween(380))", False),
])
print("ALL GOOD" if bad == 0 else "Some edits failed: send me this screen")
