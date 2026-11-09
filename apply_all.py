import os, subprocess, sys

here = os.path.dirname(os.path.abspath(__file__))
for name in ("patch_fix_build.py", "patch_added_cover_morph.py"):
    r = subprocess.run([sys.executable, os.path.join(here, name)])
    if r.returncode != 0:
        print("Stopped at", name)
        sys.exit(1)
print("ALL DONE. Now: git add -A && git commit && git push")
