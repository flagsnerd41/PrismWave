import os, re, sys

p = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/MainActivity.kt")
t = open(p, encoding="utf-8").read()

# 1) old blurred navigation bar is back
if "FloatingNavBar(s)" in t:
    t = t.replace("FloatingNavBar(s)", "NavBar(s)", 1)
    print("navigation bar: back to the blurred one")
else:
    print("navigation bar: already the old one")

# 2) remove the top title bar and its scroll tracking
t, k = re.subn(r'if \(showBar && s\.openPlaylist == null\) \{.*?fontWeight = FontWeight\.Bold\s*\)\s*\}\s*\}', '', t, count=1, flags=re.S)
print("top title bar: removed" if k else "top title bar: already gone")
t = t.replace(".nestedScroll(scrollConn)", "")

open(p, "w", encoding="utf-8").write(t)
print("done")
