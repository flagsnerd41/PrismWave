import os, sys

ROOT = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
bad = False
out = {}

def load(name):
    p = ROOT + name
    if not os.path.exists(p):
        print("MISSING FILE:", p)
        return None
    return open(p, encoding="utf-8").read()

def cut(text, start_marker, end_marker, new, fname, include_end=False):
    a = text.find(start_marker)
    if a < 0 or text.count(start_marker) != 1:
        print("PROBLEM in", fname, "- start marker found", text.count(start_marker), "times:", start_marker[:60].replace("\n", " | "))
        return None
    if end_marker is None:
        return text[:a] + new
    b = text.find(end_marker, a)
    if b < 0:
        print("PROBLEM in", fname, "- end marker not found:", end_marker[:60].replace("\n", " | "))
        return None
    if include_end:
        b += len(end_marker)
    return text[:a] + new + text[b:]

# ================= Discord.kt: cover as the big picture, P logo as the small one =================
t = load("Discord.kt")
if t is not None:
    new_block = r'''    private const val DEFAULT_APP_ID = "1565273917797564516"
    private const val LOGO_URL = "https://raw.githubusercontent.com/flagsnerd41/PrismWave/main/discord_icon.png"

    private fun effectiveId(): String = appId.trim().ifEmpty { DEFAULT_APP_ID }

    private fun buildActivity(sg: Song): JSONObject {
        val start = System.currentTimeMillis() - (lastPos * 1000).toLong()
        val a = JSONObject()
            .put("name", "PrismWave").put("type", 2)
            .put("details", sg.title).put("state", sg.artist)
            .put("application_id", effectiveId())
        val ts = JSONObject().put("start", start)
        if (sg.seconds > 1) ts.put("end", start + sg.seconds * 1000L)
        a.put("timestamps", ts)
        val assets = JSONObject()
        val cover = sg.thumb?.let { resolveImage(artUrl(it, 512)) }
        val logo = resolveImage(LOGO_URL)
        if (cover != null) {
            assets.put("large_image", cover).put("large_text", sg.title)
            if (logo != null) assets.put("small_image", logo).put("small_text", "PrismWave")
        } else if (logo != null) {
            assets.put("large_image", logo).put("large_text", "PrismWave")
        }
        if (assets.length() > 0) a.put("assets", assets)
        return a
    }

    // Discord fetches the picture itself and gives back a short key we can use in the status
    private fun resolveImage(url: String): String? {
        assetCache[url]?.let { return it }
        return try {
            val body = JSONObject().put("urls", JSONArray().put(url)).toString()
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("https://discord.com/api/v9/applications/" + effectiveId() + "/external-assets")
                .header("Authorization", token)
                .post(body)
                .build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) {
                    status = "Picture failed (error " + r.code + ")"
                    null
                } else {
                    val arr = JSONArray(r.body?.string() ?: "[]")
                    val p = arr.optJSONObject(0)?.optString("external_asset_path", "")
                    if (p.isNullOrEmpty()) null else ("mp:" + p).also { assetCache[url] = it }
                }
            }
        } catch (e: Exception) {
            null
        }
    }'''
    t2 = cut(t, "    private fun buildActivity(sg: Song): JSONObject {", "\n}\n\nprivate const val DISCORD_JS", new_block, "Discord.kt")
    if t2 is None:
        bad = True
    else:
        out["Discord.kt"] = t2

# ================= Player.kt: new bottom navigation and mini player =================
t = load("Player.kt")
if t is not None:
    nav = r'''@Composable
fun NavBar(s: AppState) {
    val tabs = listOf(
        Triple(Screen.Home, Icons.Rounded.Home, "Home"),
        Triple(Screen.Library, Icons.Rounded.LibraryMusic, "Library")
    )
    val searchSel = s.screen == Screen.Search
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiquidGlass(s.glassAlpha(), Modifier.weight(1f).height(72.dp), 36.dp) {
            Row(
                Modifier.fillMaxSize().padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { (screen, icon, label) ->
                    val sel = s.screen == screen
                    val bg by animateColorAsState(
                        if (sel) s.accent.copy(alpha = 0.24f) else Color.Transparent, tween(250), label = "navbg"
                    )
                    val fg by animateColorAsState(
                        if (sel) s.accent else Color.White.copy(alpha = 0.85f), tween(250), label = "navfg"
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(31.dp))
                            .background(bg)
                            .softClick {
                                s.screen = screen
                                s.libraryOpen = null
                                s.openPlaylist = null
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(28.dp))
                        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        LiquidGlass(s.glassAlpha(), Modifier.size(72.dp), 36.dp) {
            val sbg by animateColorAsState(
                if (searchSel) s.accent.copy(alpha = 0.24f) else Color.Transparent, tween(250), label = "snavbg"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(sbg)
                    .softClick {
                        s.screen = Screen.Search
                        s.libraryOpen = null
                        s.openPlaylist = null
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = if (searchSel) s.accent else Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

'''
    t = cut(t, "@Composable\nfun NavBar(s: AppState) {", "@Composable\nfun MiniPlayer(s: AppState) {", nav, "Player.kt")
    if t is None:
        bad = True
    else:
        mini = r'''            Box(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Cover(song, Modifier.size(50.dp), 12.dp, 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (s.status.isNotEmpty()) s.status else song.artist,
                            color = TextDim,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(Modifier.bounceClick { s.toggle() }.size(48.dp), contentAlignment = Alignment.Center) {
                        if (s.loading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
                        } else {
                            AnimatedContent(
                                targetState = s.playing,
                                transitionSpec = {
                                    (fadeIn(tween(160)) + scaleIn(initialScale = 0.5f)) togetherWith
                                        (fadeOut(tween(100)) + scaleOut(targetScale = 0.5f))
                                },
                                label = "miniplay"
                            ) { p ->
                                Icon(
                                    if (p) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    Box(Modifier.bounceClick { s.next() }.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.FastForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                val frac = (s.position / song.seconds.coerceAtLeast(1)).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 22.dp, bottom = 3.dp)
                        .fillMaxWidth(frac)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(s.accent.copy(alpha = 0.9f))
                )
            }
'''
        t = cut(
            t,
            "            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {\n                Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {",
            "                RoundBtn(Icons.Rounded.SkipNext, 0.9f, { s.next() }, 42.dp)\n            }\n",
            mini, "Player.kt", include_end=True
        )
        if t is None:
            bad = True
        else:
            imp = "import androidx.compose.foundation.layout.fillMaxSize\n"
            if t.count(imp) != 1:
                print("PROBLEM in Player.kt - import anchor found", t.count(imp), "times")
                bad = True
            else:
                t = t.replace(imp, imp + "import androidx.compose.foundation.layout.fillMaxHeight\n")
                out["Player.kt"] = t

# ================= Screens.kt: profile picture opens Settings =================
t = load("Screens.kt")
if t is not None:
    avatar = r'''@Composable
private fun ProfileAvatar(s: AppState) {
    val ctx = LocalContext.current
    LaunchedEffect(s.cookie) {
        if (s.signedIn) {
            if (ProfilePic.url == null) ProfilePic.url = YtAccount.profilePhoto(s.cookie)
        } else {
            ProfilePic.url = null
        }
    }
    val url = ProfilePic.url
    if (s.signedIn && url != null) {
        val req = remember(url) { ImageRequest.Builder(ctx).data(artUrl(url, 200)).crossfade(300).build() }
        AsyncImage(
            model = req,
            contentDescription = "Settings",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .bounceClick { s.screen = Screen.Settings }
                .size(56.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
        )
    } else {
        Box(
            Modifier
                .bounceClick { s.screen = Screen.Settings }
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AccountCircle, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(34.dp))
        }
    }
}
'''
    t = cut(t, "@Composable\nprivate fun ProfileAvatar(s: AppState) {", None, avatar, "Screens.kt")
    if t is None:
        bad = True
    else:
        out["Screens.kt"] = t

if bad:
    print("\nNothing was changed. Send me the lines above.")
    sys.exit(1)

for name, text in out.items():
    open(ROOT + name, "w", encoding="utf-8").write(text)
    print("patched", name)
print("\nDone. Now: cd ~/PrismWave && git add -A && git commit -m v28 && git push")
