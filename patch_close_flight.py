import os, re
path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "app/src/main/java/com/prismwave/app/MainActivity.kt")
src = open(path, encoding="utf-8").read()
old = "exit = slideOutVertically(tween(420, easing = FastOutSlowInEasing)) { it / 2 } + fadeOut(tween(320))"
new = "exit = if (MiniCover.rect != null && !s.reduceMotion && PlayerStage.stage == 0 && Pages.open != \"lyrics\") fadeOut(tween(320)) else slideOutVertically(tween(420, easing = FastOutSlowInEasing)) { it / 2 } + fadeOut(tween(320))"
n = src.count(old)
if n != 1:
    print("PROBLEM: found " + str(n) + " times, send me this screen")
else:
    open(path, "w", encoding="utf-8").write(src.replace(old, new))
    print("ALL GOOD")
