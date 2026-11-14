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

edit("Player.kt", [
    # the mini title and the full player title report where they are
    ("modifier = Modifier.clip(RoundedCornerShape(2.dp)),",
     "modifier = Modifier.clip(RoundedCornerShape(2.dp)).onGloballyPositioned { MiniTitle.rect = it.boundsInRoot() },", False),
    (r'modifier = Modifier\.basicMarquee\(\)(?=\s*\)\s*Text\(\s*if \(s\.status)',
     "modifier = Modifier\n                    .graphicsLayer { alpha = if (FlightState.running) 0f else 1f }\n                    .onGloballyPositioned {\n                        PlayerTitle.rect = it.boundsInRoot()\n                        PlayerTitle.sizeSp = titleSize.value\n                    }\n                    .basicMarquee()", True),
    (r'modifier = Modifier\.basicMarquee\(\)(?=\s*\)\s*\}\s*val liked = s\.isLiked\(song\))',
     "modifier = Modifier.graphicsLayer { alpha = if (FlightState.running) 0f else 1f }.basicMarquee()", True),
    # the pills no longer push the cover down after the player opens
    (r'if \(VideoSync\.has\[song\.id\] == true\) \{\s*ModePills\(\)\s*Spacer\(Modifier\.height\(14\.dp\)\)\s*\}',
     "if (VideoSync.has[song.id] == true) {\n            ModePills()\n            Spacer(Modifier.height(14.dp))\n        } else {\n            Spacer(Modifier.height(58.dp))\n        }", True),
])

# the home title shrinks to fit instead of wrapping
edit("Screens.kt", [
    ("    var stats by remember { mutableStateOf(false) }\n    val hdrA",
     "    var stats by remember { mutableStateOf(false) }\n    var titleFs by remember { mutableStateOf(26f) }\n    val hdrA", False),
    (r'fontFamily = RetroBlendy,\s*fontSize = 28\.sp,',
     "fontFamily = RetroBlendy,\n            fontSize = titleFs.sp,\n            maxLines = 1,\n            softWrap = false,\n            onTextLayout = { if (it.hasVisualOverflow && titleFs > 12f) titleFs -= 1f },", True),
])

# the player fades in a bit faster when the cover flies
edit("MainActivity.kt", [
    ("TapCover.rect != null) && !s.reduceMotion) fadeIn(tween(380))",
     "TapCover.rect != null) && !s.reduceMotion) fadeIn(tween(240))", False),
])
print("ALL GOOD" if bad == 0 else "Some edits failed: send me this screen")
