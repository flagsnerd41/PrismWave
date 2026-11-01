import pathlib
import re
import sys

p = pathlib.Path.home() / "PrismWave/app/src/main/java/com/prismwave/app/MainActivity.kt"
s = p.read_text()
ok = True


def done(name, good):
    global ok
    print(("OK    " if good else "FAIL  ") + name)
    if not good:
        ok = False


# 1) start the Studio settings when the app starts
if "StudioPrefs.init" in s:
    done("init (already applied)", True)
else:
    n = s.replace("VideoSync.init(appCtx) }", "VideoSync.init(appCtx); StudioPrefs.init(appCtx) }", 1)
    done("init", n != s)
    s = n

# 2) string lights around the splash logo
if "StringLightsFrame" in s:
    done("splash lights (already applied)", True)
else:
    n = s.replace("{ AppLogo(112.dp) }", "{ StringLightsFrame(112.dp) { AppLogo(112.dp) } }", 1)
    done("splash lights", n != s)
    s = n

# 3) Star Struck effect on Home (above the screens, below the bars and the player)
if "StarStruckHost" in s:
    done("star struck (already applied)", True)
else:
    pat = re.compile(r"\n[ \t]*AnimatedVisibility\(\n[ \t]*visible = true,\n[ \t]*modifier = Modifier\.align\(Alignment\.TopCenter\)")
    m = pat.search(s)
    if m:
        indent = re.match(r"\n([ \t]*)", m.group(0)).group(1)
        s = s[:m.start()] + "\n" + indent + "StarStruckHost(s)" + s[m.start():]
    done("star struck", m is not None)

# 4) the sparkle button + AI Sound Studio page
if "StudioHost" in s:
    done("studio host (already applied)", True)
else:
    pat = re.compile(r"\n([ \t]*)PagesHost\(s\)")
    m = pat.search(s)
    if m:
        s = s[:m.end()] + "\n" + m.group(1) + "StudioHost(s)" + s[m.end():]
    done("studio host", m is not None)

if ok:
    p.write_text(s)
    print("\nAll good. Now: git add -A && git commit -m v29 && git push")
else:
    print("\nSomething did not match, nothing was changed. Send me this screen.")
    sys.exit(1)
