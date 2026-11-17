import os

D = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
total = 0
for n in ["State.kt", "Pages.kt", "Player.kt", "Screens.kt", "Components.kt"]:
    p = D + n
    t = open(p, encoding="utf-8").read()
    c = t.count("setQueue(")
    if c:
        open(p, "w", encoding="utf-8").write(t.replace("setQueue(", "replaceQueue("))
        print(n + ": renamed " + str(c))
        total += c
print("done, " + str(total) + " places" if total else "nothing to rename (already fixed)")
