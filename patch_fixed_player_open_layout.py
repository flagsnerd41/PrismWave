import os, sys

p = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/MainActivity.kt")
s = open(p, encoding="utf-8").read()

old = '''                                if (!(flight && s.showPlayer)) {
                                    val sc = 0.30f + 0.70f * p
                                    scaleX = sc
                                    scaleY = sc
                                    transformOrigin = TransformOrigin(0.5f, 1f - (112.dp.toPx() + 36.dp.toPx()) / size.height)
                                    alpha = (p * 2.2f).coerceIn(0f, 1f)
                                }
'''
new = '''                                if (!(flight && s.showPlayer)) {
                                    // the player keeps its real layout the whole time (no shrinking), the growing
                                    // rounded shape only reveals it, so nothing jumps when the animation ends
                                    alpha = (p * 2.2f).coerceIn(0f, 1f)
                                }
'''
n = s.count(old)
if n == 0 and "keeps its real layout the whole time" in s:
    print("already patched")
    sys.exit(0)
if n != 1:
    print("FAILED: found %d times" % n)
    sys.exit(1)
open(p, "w", encoding="utf-8").write(s.replace(old, new))
print("patched MainActivity.kt")
