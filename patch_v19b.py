import glob, os

D = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")

# 1) MainActivity needs fillMaxWidth
p = D + "MainActivity.kt"
t = open(p, encoding="utf-8").read()
need = "import androidx.compose.foundation.layout.fillMaxWidth"
if need not in t:
    t = t.replace("package com.prismwave.app\n", "package com.prismwave.app\n\n" + need + "\n", 1)
    open(p, "w", encoding="utf-8").write(t)
    print("MainActivity: added fillMaxWidth import")

# 2) remove duplicate import lines everywhere (they break the build)
for f in glob.glob(D + "*.kt"):
    lines = open(f, encoding="utf-8").read().split("\n")
    seen = set()
    out = []
    removed = 0
    for ln in lines:
        if ln.startswith("import "):
            if ln in seen:
                removed += 1
                continue
            seen.add(ln)
        out.append(ln)
    if removed:
        open(f, "w", encoding="utf-8").write("\n".join(out))
        print(os.path.basename(f) + ": removed " + str(removed) + " duplicate imports")
print("done")
