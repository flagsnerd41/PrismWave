import os, sys

here = os.path.dirname(os.path.abspath(__file__))
app = os.path.join(here, "app", "src", "main", "java", "com", "prismwave", "app")

def fail(msg):
    print("PATCH FAILED:", msg)
    sys.exit(1)

# Player.kt: slower and softer move, and the player shrinks a little while it leaves
p = os.path.join(app, "Player.kt")
t = open(p, encoding="utf-8").read()
if "stiffness = 150f" not in t:
    if t.count("spring(dampingRatio = 0.85f, stiffness = 260f)") < 1:
        fail("Player.kt: step animation not found, apply the stage zip first")
    t = t.replace("spring(dampingRatio = 0.85f, stiffness = 260f)", "spring(dampingRatio = 0.9f, stiffness = 150f)")
    old = "modifier = Modifier.graphicsLayer { alpha = (1f - halfP.value * 3f).coerceAtLeast(0f) },"
    new = (
        "modifier = Modifier.graphicsLayer {\n"
        "                val sc = 1f - 0.07f * halfP.value.coerceIn(0f, 1f)\n"
        "                scaleX = sc\n"
        "                scaleY = sc\n"
        "                alpha = (1f - halfP.value * 2f).coerceAtLeast(0f)\n"
        "            },"
    )
    if t.count(old) != 1:
        fail("Player.kt: player fade not found")
    t = t.replace(old, new)
    open(p, "w", encoding="utf-8").write(t)
    print("Player.kt patched: softer step animation")
else:
    print("Player.kt already patched")

# LyricsStage.kt: the half view fades in slower, so you see the cover fly
p = os.path.join(app, "LyricsStage.kt")
t = open(p, encoding="utf-8").read()
old = ".graphicsLayer { alpha = (st() * 3f).coerceIn(0f, 1f) }"
if old in t and t.count(old) == 1:
    t = t.replace(old, ".graphicsLayer { alpha = (st() * 1.6f).coerceIn(0f, 1f) }")
    open(p, "w", encoding="utf-8").write(t)
    print("LyricsStage.kt patched: slower fade, the cover flight shows")
else:
    print("LyricsStage.kt: nothing to change")
print("All done. Now: git add -A && git commit && git push")
