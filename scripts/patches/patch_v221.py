import os, shutil, sys

BASE = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
BAK = os.path.expanduser("~/pw_bak221/")
os.makedirs(BAK, exist_ok=True)

def read(n):
    with open(BASE + n, encoding="utf-8") as f:
        return f.read()

def write(n, t):
    shutil.copyfile(BASE + n, BAK + n)
    with open(BASE + n, "w", encoding="utf-8") as f:
        f.write(t)

def swap(text, old, new, label):
    c = text.count(old)
    if c != 1:
        print("STOP: could not find the spot for '" + label + "' (found " + str(c) + "). Nothing was changed.")
        sys.exit(1)
    return text.replace(old, new)

# ---------------- Account.kt : fetch the YouTube Music profile picture
PHOTO = '''    suspend fun profilePhoto(cookie: String): String? = withContext(Dispatchers.IO) {
        try {
            val sid = cookieValue(cookie, "SAPISID") ?: cookieValue(cookie, "__Secure-3PAPISID") ?: return@withContext null
            val client = JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", CLIENT_VERSION)
                .put("hl", "en")
                .put("gl", "US")
            val body = JSONObject().put("context", JSONObject().put("client", client))
            val req = Request.Builder()
                .url(ORIGIN + "/youtubei/v1/account/account_menu?prettyPrint=false")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .header("Cookie", cookie)
                .header("Authorization", authHeader(sid))
                .header("X-Goog-AuthUser", "0")
                .header("Origin", ORIGIN)
                .header("X-Origin", ORIGIN)
                .header("Referer", "$ORIGIN/")
                .header("User-Agent", UA)
                .build()
            val text = http.newCall(req).execute().use { r ->
                if (!r.isSuccessful) null else r.body?.string()
            }
            if (text == null) return@withContext null
            var found: String? = null
            walk(JSONObject(text)) { key, v ->
                if (found == null && key == "accountPhoto") found = lastThumb(v.optJSONArray("thumbnails"))
            }
            found
        } catch (e: Exception) {
            null
        }
    }

'''
ac = read("Account.kt")
ac = swap(ac, "    suspend fun playlists(cookie: String): List<RemotePlaylist> = withContext(Dispatchers.IO) {",
          PHOTO + "    suspend fun playlists(cookie: String): List<RemotePlaylist> = withContext(Dispatchers.IO) {", "account photo")

# ---------------- Screens.kt : header picture + version
AVATAR = r'''

// ------------------------------------------------------------ home header picture

object ProfilePic {
    var url by mutableStateOf<String?>(null)
}

@Composable
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
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
        )
    }
}
'''
IMPORTS = (
    "import androidx.compose.runtime.getValue\n"
    "import androidx.compose.runtime.setValue\n"
    "import androidx.compose.runtime.mutableStateOf\n"
    "import androidx.compose.runtime.remember\n"
    "import androidx.compose.ui.layout.ContentScale\n"
    "import androidx.compose.ui.platform.LocalContext\n"
    "import coil.compose.AsyncImage\n"
    "import coil.request.ImageRequest\n"
)
sc = read("Screens.kt")
sc = swap(sc, "package com.prismwave.app\n", "package com.prismwave.app\n\n" + IMPORTS, "screens imports")
sc = swap(sc, """        Box(
            Modifier
                .bounceClick { s.screen = Screen.Settings }
                .size(56.dp)
                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                .clip(CircleShape)
        ) { AppLogo(56.dp) }
""", "        ProfileAvatar(s)\n", "header avatar")
sc = swap(sc, 'Section("v22") {', '''Section("v22.1") {
                        ChangeList(listOf(
                            "Home header shows your YouTube Music profile picture when you are signed in",
                            "Lighter glass blur on the bars",
                            "Splash: made by flagsnerd appears letter by letter, blurred to clear",
                            "New app icon"
                        ))
                    }
                    Section("v22") {''', "changelog")
sc = swap(sc, 'Text("Version v22", color = Color.White)', 'Text("Version v22.1", color = Color.White)', "version")
sc = sc.rstrip("\n") + "\n" + AVATAR

# ---------------- Components.kt : lighter glass
co = read("Components.kt")
co = swap(co, "                    blurLayer.renderEffect = BlurEffect(backdrop.radius * k, backdrop.radius * k, TileMode.Clamp)\n",
          "                    val r = backdrop.radius * 0.4f * k\n                    blurLayer.renderEffect = BlurEffect(r, r, TileMode.Clamp)\n", "glass radius")
co = swap(co, ".background(Surface2.copy(alpha = if (useBlur) alpha * 0.6f else alpha))",
          ".background(Surface2.copy(alpha = if (useBlur) alpha * 0.4f else alpha))", "glass tint")

# ---------------- MainActivity.kt : splash letters
LETTERS = r'''

@Composable
fun GlowLetters(accent: Color) {
    val text = "made by flagsnerd"
    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { i, ch ->
            if (ch == ' ') {
                Spacer(Modifier.width(9.dp))
            } else {
                val p = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(600L + i * 70L)
                    p.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
                }
                val glow = kotlin.math.sin(p.value * Math.PI.toFloat())
                Text(
                    ch.toString(),
                    color = lerp(accent, Color.White, p.value),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                    style = TextStyle(shadow = Shadow(accent.copy(alpha = 0.9f * glow), Offset.Zero, 18f * glow + 0.1f)),
                    modifier = Modifier.graphicsLayer {
                        alpha = p.value
                        val r = (1f - p.value) * 16f
                        renderEffect = if (Build.VERSION.SDK_INT >= 31 && r > 0.5f) BlurEffect(r, r, TileMode.Decal) else null
                    }
                )
            }
        }
    }
}
'''
MIMPORTS = (
    "import androidx.compose.foundation.layout.Row\n"
    "import androidx.compose.foundation.layout.width\n"
    "import androidx.compose.ui.graphics.Shadow\n"
    "import androidx.compose.ui.geometry.Offset\n"
    "import androidx.compose.ui.text.TextStyle\n"
)
ma = read("MainActivity.kt")
ma = swap(ma, "package com.prismwave.app\n", "package com.prismwave.app\n\n" + MIMPORTS, "main imports")
ma = swap(ma, "GlowWords(s.accent)", "GlowLetters(s.accent)", "splash words")
ma = ma.rstrip("\n") + "\n" + LETTERS

write("Account.kt", ac)
write("Screens.kt", sc)
write("Components.kt", co)
write("MainActivity.kt", ma)
print("v22.1 applied. Now: cd ~/PrismWave && git add -A && git commit -m v22.1 && git push")
