import os, sys
p = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/State.kt")
s = open(p, encoding="utf-8").read()

def rep(old, new):
    global s
    n = s.count(old)
    if n != 1:
        print("FAILED: found %d times: %s" % (n, old.strip()[:60]))
        sys.exit(1)
    s = s.replace(old, new)

if "warmingFor" in s:
    print("already patched")
    sys.exit(0)

rep("    private var queuedId: String? = null\n",
    "    private var queuedId: String? = null\n    private var warmingFor: String? = null\n")

rep("        fading = false\n        queuedId = null\n        pausedByUnplug = false\n",
    "        fading = false\n        queuedId = null\n        warmingFor = null\n        pausedByUnplug = false\n")

rep("        retriedId = null\n        queuedId = null\n        playCount++\n        known[sg.id] = sg\n",
    "        retriedId = null\n        queuedId = null\n        warmingFor = null\n        playCount++\n        known[sg.id] = sg\n")

rep("XFade.start(ctx, nxt.id, url, (leftSec * 1000).toLong() - 150L)",
    "XFade.begin(ctx, nxt.id, url, (leftSec * 1000).toLong() - 150L)")

rep("    // the first song has ended: the main player continues the next song",
'''    // a few seconds before the crossfade: load the next song silently on the helper player
    private fun warmOverlap() {
        val nid = queuedId ?: return
        if (warmingFor == nid || XFade.preparedId == nid || XFade.active) return
        val nxt = queue.firstOrNull { it.id == nid } ?: known[nid] ?: return
        warmingFor = nid
        scope.launch {
            try {
                val url = DownloadStore.localUri(nxt) ?: SongCache.uri(ctx, nxt.id) ?: Engine.streamUrl(nxt.url)
                if (current == null || queuedId != nid) {
                    warmingFor = null
                    return@launch
                }
                XFade.prepare(ctx, nid, url)
            } catch (e: Exception) {
                warmingFor = null
            }
        }
    }

    // the first song has ended: the main player continues the next song''')

rep("                val cf = (if (AutoMix.on) AutoMix.leadFor(current?.id ?: \"\") else null) ?: Opts.crossfade.toFloat()\n",
'''                val cf = (if (AutoMix.on) AutoMix.leadFor(current?.id ?: "") else null) ?: Opts.crossfade.toFloat()
                if (dur / 1000f > cf * 3 && left > cf && left <= cf + 8f) {
                    warmOverlap()
                } else if (left > cf + 10f && XFade.preparedId != null) {
                    XFade.stop()
                    warmingFor = null
                }
''')

open(p, "w", encoding="utf-8").write(s)
print("patched State.kt")
