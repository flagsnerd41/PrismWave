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

# ---------------- Player.kt: cover blur-out + flip
pp = os.path.join(app, "Player.kt")
t = read(pp)
if "TransitionCover(" not in t:
    pairs = [
        ("Cover(song, Modifier.fillMaxSize(), 20.dp, 96.sp, quality = 1000)",
         "TransitionCover(s, song, Modifier.fillMaxSize(), 20.dp, 96.sp, 1000)"),
        ("Cover(song, Modifier.fillMaxSize(), 0.dp, 160.sp, quality = 1200)",
         "TransitionCover(s, song, Modifier.fillMaxSize(), 0.dp, 160.sp, 1200)"),
    ]
    for old, new in pairs:
        if t.count(old) != 1:
            fail("Player.kt: could not find " + old)
        t = t.replace(old, new)
    write(pp, t)
    print("Player.kt patched: cover blur-out and flip")
else:
    print("Player.kt already patched")

# ---------------- Settings2.kt: Material 3 + Transitions page
sp = os.path.join(app, "Settings2.kt")
t = read(sp)

if "// M3-SETTINGS" not in t:
    a = t.find("// ------------------------------------------------------------ building blocks (ArchiveTune style)")
    b = t.find("// ------------------------------------------------------------ pages")
    if a < 0 or b < 0 or b < a:
        fail("Settings2.kt: section markers not found")
    block = read(os.path.join(here, "settings_m3_block.txt"))
    t = t[:a] + block + t[b:]
    print("Settings2.kt: Material 3 building blocks and screen installed")
else:
    print("Settings2.kt: Material 3 already installed")

imports = [
    "androidx.compose.foundation.layout.WindowInsets",
    "androidx.compose.material3.ExperimentalMaterial3Api",
    "androidx.compose.material3.FilledTonalIconButton",
    "androidx.compose.material3.IconButtonDefaults",
    "androidx.compose.material3.LargeTopAppBar",
    "androidx.compose.material3.Slider",
    "androidx.compose.material3.SliderDefaults",
    "androidx.compose.material3.TopAppBarDefaults",
    "androidx.compose.ui.input.nestedscroll.nestedScroll",
]
anchor = "import androidx.activity.compose.BackHandler\n"
if anchor not in t:
    fail("Settings2.kt: import anchor not found")
add = ""
for imp in imports:
    if ("import " + imp + "\n") not in t:
        add += "import " + imp + "\n"
if add:
    t = t.replace(anchor, anchor + add, 1)

page = read(os.path.join(here, "transitions_page.txt"))
i = t.find("@Composable\nprivate fun PfTransitionsPage(")
if i >= 0:
    t = t[:i].rstrip() + "\n\n" + page
else:
    t = t.rstrip() + "\n\n" + page

anchor2 = '    PfChange("v27", a, listOf(\n'
e8 = (
    '    PfChange("Added 8D audio and Transitions", a, listOf(\n'
    '        "8D audio: the sound slowly circles around your head, speed slider",\n'
    '        "New Transitions page: crossfade, audio blur-out and 8D in one place"\n'
    '    ))\n'
)
en = (
    '    PfChange("Added cover blur-out, cover flip and Material 3 settings", a, listOf(\n'
    '        "Cover blur-out: the album cover slowly blurs as the song ends",\n'
    '        "Cover flip: the cover square flips over to the next song",\n'
    '        "Settings rebuilt in Material 3: large collapsing title, tonal icons, Material sliders"\n'
    '    ))\n'
)
if "Added 8D audio and Transitions" not in t:
    if t.count(anchor2) != 1:
        fail("Settings2.kt: changelog anchor not found")
    t = t.replace(anchor2, e8 + anchor2, 1)
if "Added cover blur-out" not in t:
    key = '    PfChange("Added 8D audio and Transitions"'
    if t.count(key) != 1:
        fail("Settings2.kt: changelog entry anchor not found")
    t = t.replace(key, en + key, 1)

write(sp, t)
print("Settings2.kt: Transitions page ready")
print("All done. Now: git add -A && git commit && git push")
