import os, sys

here = os.path.dirname(os.path.abspath(__file__))
app = os.path.join(here, "app", "src", "main", "java", "com", "prismwave", "app")

def fail(msg):
    print("PATCH FAILED:", msg)
    sys.exit(1)

p = os.path.join(app, "Player.kt")
t = open(p, encoding="utf-8").read()
if "frosted" in t:
    print("Player.kt already patched")
    sys.exit(0)

old = (
    "                Box(\n"
    "                    Modifier.fillMaxSize().background(\n"
    "                        Brush.verticalGradient(\n"
    "                            0.44f to Color.Transparent,\n"
    "                            0.60f to bottom.copy(alpha = 0.30f),\n"
    "                            0.80f to bottom.copy(alpha = 0.80f),\n"
    "                            1f to bottom\n"
    "                        )\n"
    "                    )\n"
    "                )\n"
)
new = (
    "                // the bottom of the video melts into a blurred copy of the cover: the title area is frosted, no hard edge\n"
    "                if (!Opts.lite) {\n"
    "                    Box(\n"
    "                        Modifier\n"
    "                            .fillMaxSize()\n"
    "                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }\n"
    "                            .drawWithContent {\n"
    "                                drawContent()\n"
    "                                drawRect(\n"
    "                                    brush = Brush.verticalGradient(0.46f to Color.Transparent, 0.78f to Color.Black),\n"
    "                                    blendMode = BlendMode.DstIn\n"
    "                                )\n"
    "                            }\n"
    "                    ) {\n"
    "                        Cover(song, Modifier.fillMaxSize().blur(36.dp), 0.dp, 160.sp, quality = 400)\n"
    "                    }\n"
    "                }\n"
    "                Box(\n"
    "                    Modifier.fillMaxSize().background(\n"
    "                        Brush.verticalGradient(0.50f to Color.Transparent, 1f to bottom.copy(alpha = 0.85f))\n"
    "                    )\n"
    "                )\n"
)
n = t.count(old)
if n != 1:
    fail("video overlay: found " + str(n) + " times")
t = t.replace(old, new)
open(p, "w", encoding="utf-8").write(t)
print("Player.kt patched: frosted blur at the bottom of the video")
print("All done. Now: git add -A && git commit && git push")
