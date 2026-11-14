import os, re
path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "app/src/main/java/com/prismwave/app/Screens.kt")
src = open(path, encoding="utf-8").read()
pat = r'\) \{ AppLogo\(42\.dp\) \}'
n = len(re.findall(pat, src))
if n != 1:
    print("PROBLEM: found " + str(n) + " times, send me this screen")
else:
    src = re.sub(pat, lambda m: ") { PrismSpinner(Modifier.size(width = 60.dp, height = 46.dp), s.reduceMotion || Opts.lite) }", src)
    open(path, "w", encoding="utf-8").write(src)
    print("ALL GOOD")
