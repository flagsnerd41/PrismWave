import os, sys

BASE = os.path.expanduser("~/PrismWave/app/src/main/")
disc = BASE + "java/com/prismwave/app/Discord.kt"
man = BASE + "AndroidManifest.xml"
bad = False

t = open(disc, encoding="utf-8").read()
a = t.find('// Discord shows a "get the app" page')
b = t.find('/** Starts the Discord status once')
if a < 0 or b < 0 or b < a:
    print("PROBLEM in Discord.kt: could not find the login block")
    bad = True
else:
    t = t[:a] + '''/** Opens the Discord sign-in in its own plain Android screen (not inside Compose). */
@Composable
fun DiscordLogin() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        DiscordRpc.loginOpen = false
        ctx.startActivity(android.content.Intent(ctx, DiscordLoginActivity::class.java))
    }
}

''' + t[b:]

m = open(man, encoding="utf-8").read()
anchor = '        <service\n            android:name=".PlaybackService"'
if m.count(anchor) != 1:
    print("PROBLEM in AndroidManifest.xml: service anchor found", m.count(anchor), "times")
    bad = True
elif ".DiscordLoginActivity" in m:
    print("manifest already has the login screen")
else:
    m = m.replace(anchor, '        <activity\n            android:name=".DiscordLoginActivity"\n            android:exported="false" />\n\n' + anchor)

if bad:
    print("\nNothing was changed. Send me the lines above.")
    sys.exit(1)

open(disc, "w", encoding="utf-8").write(t)
open(man, "w", encoding="utf-8").write(m)
print("patched Discord.kt and AndroidManifest.xml")
print("\nDone. Now: cd ~/PrismWave && git add -A && git commit -m discord-login && git push")
