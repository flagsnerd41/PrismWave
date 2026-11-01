import pathlib
import sys

p = pathlib.Path.home() / "PrismWave/app/src/main/java/com/prismwave/app/StudioUi.kt"
s = p.read_text()
if "StarStruckOverlay2(" in s:
    print("OK    new Star Struck (already applied)")
elif "StarStruckOverlay(Modifier.fillMaxSize())" in s:
    s = s.replace("StarStruckOverlay(Modifier.fillMaxSize())", "StarStruckOverlay2(Modifier.fillMaxSize())", 1)
    p.write_text(s)
    print("OK    new Star Struck")
    print("\nAll good. Now: git add -A && git commit -m v32 && git push")
else:
    print("FAIL  new Star Struck\nNothing was changed. Send me this screen.")
    sys.exit(1)
