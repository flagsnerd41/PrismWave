import pathlib
import re
import sys

base = pathlib.Path.home() / "PrismWave/app/src/main/java/com/prismwave/app"
ok = True
files = {}


def load(name):
    if name not in files:
        files[name] = (base / name).read_text()
    return files[name]


def step(file, name, marker, fn):
    """fn(text) returns new text or None when the anchor was not found"""
    global ok
    t = load(file)
    if marker in t:
        print("OK    " + name + " (already applied)")
        return
    n = fn(t)
    if n is None:
        print("FAIL  " + name)
        ok = False
    else:
        files[file] = n
        print("OK    " + name)


def replace_once(old, new):
    def f(t):
        return t.replace(old, new, 1) if old in t else None
    return f


# ---- Account.kt: also read the account name (same request as the picture)
step(
    "Account.kt", "account name", "ProfilePic.name = nm",
    replace_once(
        '                if (found == null && key == "accountPhoto") found = lastThumb(v.optJSONArray("thumbnails"))\n',
        '                if (found == null && key == "accountPhoto") found = lastThumb(v.optJSONArray("thumbnails"))\n'
        '                if (key == "accountName" && ProfilePic.name == null) {\n'
        '                    val nm = firstRunText(v).trim()\n'
        '                    if (nm.isNotEmpty()) ProfilePic.name = nm\n'
        '                }\n',
    ),
)

# ---- Screens.kt
step(
    "Screens.kt", "profile name holder", "var name by mutableStateOf<String?>(null)",
    replace_once(
        "object ProfilePic {\n    var url by mutableStateOf<String?>(null)\n",
        "object ProfilePic {\n    var url by mutableStateOf<String?>(null)\n    var name by mutableStateOf<String?>(null)\n",
    ),
)
step(
    "Screens.kt", "name cleared on sign out", "ProfilePic.name = null",
    replace_once(
        "        } else {\n            ProfilePic.url = null\n        }",
        "        } else {\n            ProfilePic.url = null\n            ProfilePic.name = null\n        }",
    ),
)


def header(t):
    a = t.find("@Composable\nprivate fun HomeHeader2(s: AppState) {")
    b = t.find("@Composable\nprivate fun FeaturedHero(")
    if a < 0 or b < 0 or b < a:
        return None
    new = (
        "@Composable\n"
        "private fun HomeHeader2(s: AppState) {\n"
        "    val name = ProfilePic.name\n"
        "    Row(\n"
        "        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 6.dp),\n"
        "        verticalAlignment = Alignment.CenterVertically\n"
        "    ) {\n"
        "        Column(Modifier.weight(1f)) {\n"
        "            if (s.signedIn && !name.isNullOrBlank()) {\n"
        "                Text(skyGreeting(), color = Color.White.copy(alpha = 0.75f), fontSize = 18.sp)\n"
        "                Text(\n"
        "                    name,\n"
        "                    color = Color.White,\n"
        "                    fontFamily = PrismFont,\n"
        "                    fontSize = 36.sp,\n"
        "                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,\n"
        "                    maxLines = 1,\n"
        "                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis\n"
        "                )\n"
        "            } else {\n"
        "                Text(skyGreeting(), color = Color.White, fontFamily = PrismFont, fontSize = 36.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)\n"
        "            }\n"
        "        }\n"
        "        ProfileAvatar(s)\n"
        "    }\n"
        "}\n\n"
    )
    return t[:a] + new + t[b:]


step("Screens.kt", "greeting header", "skyGreeting()", header)


def glow(t):
    i = t.find("val hero = bestQuick?.firstOrNull()")
    if i < 0:
        return None
    old = (
        "        Box(\n"
        "            Modifier\n"
        "                .fillMaxWidth()\n"
        "                .height(430.dp)\n"
        "                .align(Alignment.TopCenter)\n"
        "                .background(\n"
        "                    Brush.verticalGradient(\n"
        "                        0f to s.accent.copy(alpha = 0.30f),\n"
        "                        0.42f to s.accent.copy(alpha = 0.12f),\n"
        "                        1f to Color.Transparent\n"
        "                    )\n"
        "                )\n"
        "        )\n"
    )
    j = t.find(old, i)
    if j < 0:
        return None
    return t[:j] + "        HomeTopGlow(s, Modifier.align(Alignment.TopCenter))\n" + t[j + len(old):]


step("Screens.kt", "sky glow on Home", "HomeTopGlow(", glow)


# ---- MainActivity.kt: the cat above the mini player / nav bar (nav bar itself is not changed)
def cat(t):
    m = re.search(r"\n([ \t]*)MiniPlayer\(s\)\n[ \t]*NavBar\(s\)", t)
    if not m:
        return None
    return t[:m.start()] + "\n" + m.group(1) + "NavCat(s)" + t[m.start():]


step("MainActivity.kt", "cat", "NavCat(s)", cat)

# ---- StudioUi.kt: switches for the cat and the sky (+ a way to try every time of day)
UI = (
    "\n        Spacer(Modifier.height(10.dp))\n"
    "        Row(verticalAlignment = Alignment.CenterVertically) {\n"
    "            Column(Modifier.weight(1f)) {\n"
    "                Text(\"Cat on the bottom bar\", color = Color.White, fontSize = 15.sp)\n"
    "                Text(\"A cat that cleans itself above the bar\", color = TextDim, fontSize = 12.sp)\n"
    "            }\n"
    "            Switch(checked = LookPrefs.catOn, onCheckedChange = { LookPrefs.catOn = it; LookPrefs.save() })\n"
    "        }\n"
    "        Spacer(Modifier.height(10.dp))\n"
    "        Row(verticalAlignment = Alignment.CenterVertically) {\n"
    "            Column(Modifier.weight(1f)) {\n"
    "                Text(\"Sky glow on Home\", color = Color.White, fontSize = 15.sp)\n"
    "                Text(\"Sun, sunset or moon that follows the time of day\", color = TextDim, fontSize = 12.sp)\n"
    "            }\n"
    "            Switch(checked = LookPrefs.skyOn, onCheckedChange = { LookPrefs.skyOn = it; LookPrefs.save() })\n"
    "        }\n"
    "        Spacer(Modifier.height(8.dp))\n"
    "        Text(\"Try the sky\", color = TextDim, fontSize = 12.sp)\n"
    "        Spacer(Modifier.height(6.dp))\n"
    "        Row(Modifier.horizontalScroll(rememberScrollState())) {\n"
    "            listOf(\"Auto\" to null, \"Morning\" to 8f, \"Afternoon\" to 13f, \"Evening\" to 18.5f, \"Night\" to 23f).forEach { (n, hr) ->\n"
    "                StudioChip(n, LookPrefs.previewHour == hr, acc) { LookPrefs.previewHour = hr }\n"
    "            }\n"
    "        }\n"
)


def ui(t):
    anchor = "Switch(checked = StudioPrefs.lightsOn, onCheckedChange = { StudioPrefs.lightsOn = it; StudioPrefs.save() })\n        }\n"
    i = t.find(anchor)
    if i < 0:
        return None
    k = i + len(anchor)
    return t[:k] + UI.lstrip("\n") + t[k:]


step("StudioUi.kt", "settings switches", "LookPrefs.catOn", ui)

if ok:
    for name, text in files.items():
        (base / name).write_text(text)
    print("\nAll good. Now: git add -A && git commit -m v31 && git push")
else:
    print("\nSomething did not match, nothing was changed. Send me this screen.")
    sys.exit(1)
