import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PATCH_DIR = ROOT / "scripts" / "patches"

for name in ("patch_fix_build.py", "patch_added_cover_morph.py"):
    script = PATCH_DIR / name
    result = subprocess.run([sys.executable, str(script)], cwd=ROOT)
    if result.returncode != 0:
        print("Stopped at", script.name)
        sys.exit(result.returncode)

print("ALL DONE. Now: git add -A && git commit && git push")
