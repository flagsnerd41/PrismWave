import os, sys
path = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/State.kt")
t = open(path).read()
if "fun applyTempoPitch" in t:
    print("State.kt already patched"); sys.exit(0)
def rep(old, new):
    global t
    if t.count(old) != 1:
        print("FAILED, anchor not found:", old[:60]); sys.exit(1)
    t = t.replace(old, new)
rep("import androidx.media3.common.PlaybackException\n",
    "import androidx.media3.common.PlaybackException\nimport androidx.media3.common.PlaybackParameters\n")
rep("    var crossfade by mutableStateOf(0f)\n",
"""    var crossfade by mutableStateOf(0f)
    var tempo by mutableStateOf(1f)
    var pitch by mutableStateOf(1f)

    fun applyTempoPitch() {
        controller?.playbackParameters = PlaybackParameters(tempo, pitch)
    }
""")
open(path, "w").write(t)
print("patched State.kt")
