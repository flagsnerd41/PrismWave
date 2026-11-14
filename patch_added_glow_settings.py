#!/usr/bin/env python3
# added moving glow, About at the top of Settings, Discord at the top of Integration, smoother settings animations
# run from anywhere:  python3 ~/PrismWave/patch_added_glow_settings.py
import os
import re
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

# ---------------------------------------------------------------- Player.kt: glow behind the controls
t = load("Player.kt")
t = must_replace(
    t,
    "            .background(Brush.verticalGradient(listOf(c1, c2, c3)))\n",
    "            .background(Brush.verticalGradient(listOf(c1, c2, c3)))\n"
    "            .glowBehind(art, s.playing, s.batterySaver, 0.35f)\n",
    "Album glow",
)
t = must_replace(
    t,
    "        // sharp cover stage on top, fading into the backdrop\n        Box(",
    "        Box(Modifier.fillMaxSize().glowBehind(art, s.playing, s.batterySaver, 0.42f))\n"
    "        // sharp cover stage on top, fading into the backdrop\n        Box(",
    "Immersive glow",
)
files["Player.kt"] = t

# ---------------------------------------------------------------- LyricsStage.kt: glow behind the lyrics
t = load("LyricsStage.kt")
t = must_replace(
    t,
    "        // 2. the cover: it flies up, then shrinks into the corner\n",
    "        // the moving glow behind the half view and the full lyrics\n"
    "        Box(Modifier.fillMaxSize().graphicsLayer { alpha = st().coerceIn(0f, 1f) }.glowBehind(art, s.playing, s.batterySaver, 0f))\n"
    "        // 2. the cover: it flies up, then shrinks into the corner\n",
    "Lyrics glow",
)
files["LyricsStage.kt"] = t

# ---------------------------------------------------------------- Settings2.kt
t = load("Settings2.kt")

# the Discord logo is a vector resource
t = must_replace(
    t,
    "import androidx.compose.ui.platform.LocalContext\n",
    "import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.res.vectorResource\n",
    "vectorResource import",
)

# groups slide in one after another on every page
t = must_replace(
    t,
    "@Composable\nprivate fun PfGroup(content: @Composable ColumnScope.() -> Unit) {\n"
    "    Column(\n"
    "        Modifier\n"
    "            .padding(horizontal = 16.dp, vertical = 2.dp)",
    "private object PfSeq {\n"
    "    private var n = 0\n"
    "    fun next(): Int {\n"
    "        n = (n + 1) % 7\n"
    "        return n\n"
    "    }\n"
    "}\n\n"
    "@Composable\nprivate fun PfGroup(content: @Composable ColumnScope.() -> Unit) {\n"
    "    val seq = remember { PfSeq.next() }\n"
    "    Column(\n"
    "        Modifier\n"
    "            .appear(seq)\n"
    "            .padding(horizontal = 16.dp, vertical = 2.dp)",
    "PfGroup appear",
)

# rows squish when pressed
t = must_replace(
    t,
    "            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)\n"
    "            .padding(horizontal = 16.dp, vertical = 14.dp)",
    "            .then(if (onClick != null) Modifier.softClick { onClick() } else Modifier)\n"
    "            .padding(horizontal = 16.dp, vertical = 14.dp)",
    "PfRow press",
)

# the About banner (new building block) before the screen
banner = '''@Composable
private fun PfAboutBanner(accent: Color, onOpen: () -> Unit) {
    Row(
        Modifier
            .appear(0)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(accent.copy(alpha = 0.32f), CardBg)))
            .softClick { onOpen() }
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLogo(56.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("About PrismWave", color = Color.White, fontFamily = RetroBlendy, fontSize = 24.sp)
            Text("Version 1.5 and updates", color = TextDim, fontSize = 14.sp)
        }
        Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
}

'''
t = must_replace(
    t,
    "// ------------------------------------------------------------ the screen (Material 3 large top bar)\n",
    banner + "// ------------------------------------------------------------ the screen (Material 3 large top bar)\n",
    "About banner",
)

# smoother page change: the new page slides and scales in with a soft ease, the old one slides back and fades
t = must_replace(
    t,
    "                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> if (forward) w / 4 else -w / 4 } +\n"
    "                    fadeIn(tween(260))) togetherWith\n"
    "                    (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> if (forward) -w / 4 else w / 4 } +\n"
    "                        fadeOut(tween(160)))",
    "                (slideInHorizontally(tween(480, easing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))) { w -> if (forward) w / 3 else -w / 5 } +\n"
    "                    fadeIn(tween(340, delayMillis = 60)) +\n"
    "                    androidx.compose.animation.scaleIn(tween(480, easing = androidx.compose.animation.core.CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)), initialScale = 0.94f)) togetherWith\n"
    "                    (slideOutHorizontally(tween(360, easing = FastOutSlowInEasing)) { w -> if (forward) -w / 6 else w / 3 } +\n"
    "                        fadeOut(tween(200)) +\n"
    "                        androidx.compose.animation.scaleOut(tween(360, easing = FastOutSlowInEasing), targetScale = 0.96f))",
    "Page animation",
)

# About at the very top of the main list
t = must_replace(
    t,
    "                if (pg == null) {\n                    PfMainGroup(",
    "                if (pg == null) {\n                    PfAboutBanner(acc) { page = \"about\" }\n                    PfMainGroup(",
    "About on top",
)
t = must_replace(
    t,
    '                            PfItem("changelog", "What\'s new", "Changes in each version", Icons.Rounded.History),\n'
    '                            PfItem("about", "About PrismWave", "Version, updates and credits", Icons.Rounded.Info)\n',
    '                            PfItem("changelog", "What\'s new", "Changes in each version", Icons.Rounded.History)\n',
    "About out of the bottom group",
)

# moving glow settings, in Appearance before Extras
t = must_replace(
    t,
    '    PfLabel("Extras", a)\n',
    '    GlowPrefs.init(LocalContext.current.applicationContext)\n'
    '    PfLabel("Moving glow", a)\n'
    '    PfGroup {\n'
    '        PfSwitch(\n'
    '            Icons.Rounded.AutoAwesome, "Moving glow",\n'
    '            "Bright and dark colors from the cover flow behind the player and the lyrics. Off in Lite mode and Battery saver",\n'
    '            GlowPrefs.on, a\n'
    '        ) {\n'
    '            GlowPrefs.on = it\n'
    '            GlowPrefs.save()\n'
    '        }\n'
    '        PfChoice(Icons.Rounded.Palette, "Glow strength", listOf("Soft", "Normal", "Strong"), GlowPrefs.intensity, a) {\n'
    '            GlowPrefs.intensity = it\n'
    '            GlowPrefs.save()\n'
    '        }\n'
    '        PfChoice(Icons.Rounded.Speed, "Glow speed", listOf("Slow", "Normal", "Fast"), GlowPrefs.speed, a) {\n'
    '            GlowPrefs.speed = it\n'
    '            GlowPrefs.save()\n'
    '        }\n'
    '    }\n'
    '    PfLabel("Extras", a)\n',
    "Glow settings",
)

# Discord: cut the old block out of the Integration page and put a new section with the logo at the top
start_marker = '    PfLabel("Discord", a)\n    PfGroup {\n        PfSwitch(\n            Icons.Rounded.AutoAwesome, "Discord status",'
end_marker = "    if (DiscordRpc.loginOpen) DiscordLogin()\n"
i0 = t.find(start_marker)
i1 = t.find(end_marker, i0)
if i0 < 0 or i1 < 0 or t.count(start_marker) != 1:
    print("FAILED: Discord block markers (" + str(i0) + ", " + str(i1) + ")")
    sys.exit(1)
t = t[:i0] + t[i1 + len(end_marker):]
t = must_replace(t, '    PfLabel("ListenBrainz", a)\n', '    PfDiscordSection()\n    PfLabel("ListenBrainz", a)\n', "Discord on top")

discord = '''@Composable
private fun PfDiscordSection() {
    val a = Color(0xFF5865F2)
    val ctx = LocalContext.current
    val logo = ImageVector.vectorResource(R.drawable.ic_discord)
    Row(
        Modifier
            .appear(0)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(a, Color(0xFF4752C4))))
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(logo, contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text("Discord", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Show what you listen to on your profile", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
        }
    }
    PfGroup {
        PfSwitch(
            logo, "Discord status",
            if (DiscordRpc.hasToken()) "Shows the song you listen to on your Discord profile"
            else "Sign in to Discord first. Unofficial, so an alt account is safer",
            DiscordRpc.enabled && DiscordRpc.hasToken(), a
        ) { on ->
            if (on && !DiscordRpc.hasToken()) DiscordRpc.loginOpen = true
            else DiscordRpc.setEnabled(ctx, on)
        }
        if (DiscordRpc.hasToken()) {
            PfField("Discord application ID (optional, adds cover art)", DiscordRpc.appId, a, false) {
                DiscordRpc.setAppId(ctx, it)
            }
            PfAction(Icons.Rounded.Lock, "Sign out of Discord", "Forget the Discord login on this phone", a) {
                DiscordRpc.logout(ctx)
            }
        }
        if (!DiscordRpc.hasToken()) {
            PfAction(
                Icons.Rounded.Lock, "Use token from clipboard",
                "If the sign-in page does not work, copy your Discord token and tap here", a
            ) {
                val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val tok = (cm.primaryClip?.getItemAt(0)?.text?.toString() ?: "").trim().trim('"')
                if (tok.length > 40) DiscordRpc.onToken(ctx.applicationContext, tok)
            }
        }
        if (DiscordRpc.status.isNotEmpty()) PfNote(DiscordRpc.status)
    }
    if (DiscordRpc.loginOpen) DiscordLogin()
}

'''
t = must_replace(
    t,
    "@Composable\nprivate fun PfIntegrationPage(s: AppState) {\n",
    discord + "@Composable\nprivate fun PfIntegrationPage(s: AppState) {\n",
    "Discord section function",
)

# changelog
marker = "private fun PfChangelogPage(s: AppState) {\n    val a = s.accent\n"
t = must_replace(
    t,
    marker,
    marker +
    '    PfChange("Added moving glow, Discord on top and smoother settings", a, listOf(\n'
    '        "Moving glow behind the player controls and the lyrics: bright and dark colors from the cover flow and move. Settings, Appearance",\n'
    '        "About PrismWave is now the first thing in Settings",\n'
    '        "Discord is at the top of Integration with its logo",\n'
    '        "Settings: pages slide and scale in smoothly, cards rise one after another, rows squish when pressed"\n'
    '    ))\n',
    "Changelog",
)
files["Settings2.kt"] = t

# ---------------------------------------------------------------- Extras.kt (red dot on What's new)
t = load("Extras.kt")
t, n = re.subn(r'const val ID = "[^"]*"', 'const val ID = "added-glow-settings"', t, count=1)
if n != 1:
    print("FAILED: WhatsNew id")
    sys.exit(1)
files["Extras.kt"] = t

for name, text in files.items():
    with open(os.path.join(ROOT, name), "w", encoding="utf-8") as f:
        f.write(text)
    print("patched " + name)
print("done")
