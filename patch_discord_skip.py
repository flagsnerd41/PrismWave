import os, sys

P = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/Discord.kt")
if not os.path.exists(P):
    print("MISSING FILE:", P)
    sys.exit(1)
t = open(P, encoding="utf-8").read()
edits = []

def add(old, new):
    edits.append((old, new))

add(r'''    private val client = OkHttpClient()
''', r'''    private val client = OkHttpClient()
    private val http = OkHttpClient.Builder().callTimeout(8, java.util.concurrent.TimeUnit.SECONDS).build()
''')
add(r'''    private var lastPos = 0f
''', r'''    private var lastPos = 0f
    private var lastAt = 0L
    private var imgJob: Job? = null
''')
add(r'''                lastSong = song
                lastPos = s.position
                lastPlaying = playing''', r'''                // a new song always starts at 0, even if the position has not updated yet
                lastPos = if (song?.id != lastSong?.id) 0f else s.position
                lastAt = System.currentTimeMillis()
                lastSong = song
                lastPlaying = playing''')
add(r'''    private fun push() {
        if (!ready) return
        val sg = lastSong
        val acts = JSONArray()
        if (sg != null && lastPlaying) acts.put(buildActivity(sg))
        val d = JSONObject().put("since", 0).put("activities", acts).put("status", "online").put("afk", false)
        ws?.send(JSONObject().put("op", 3).put("d", d).toString())
    }''', r'''    private fun sendActivity(a: JSONObject?) {
        val acts = JSONArray()
        if (a != null) acts.put(a)
        val d = JSONObject().put("since", 0).put("activities", acts).put("status", "online").put("afk", false)
        ws?.send(JSONObject().put("op", 3).put("d", d).toString())
    }

    private fun push() {
        if (!ready) return
        val sg = lastSong
        if (sg == null || !lastPlaying) {
            sendActivity(null)
            return
        }
        // the new song shows up at once, the pictures follow a moment later
        sendActivity(buildActivity(sg, false))
        imgJob?.cancel()
        imgJob = scope.launch {
            val rich = buildActivity(sg, true)
            if (lastSong?.id == sg.id && lastPlaying) sendActivity(rich)
        }
    }''')
add(r'''    private fun buildActivity(sg: Song): JSONObject {
        val start = System.currentTimeMillis() - (lastPos * 1000).toLong()''', r'''    private fun buildActivity(sg: Song, pics: Boolean): JSONObject {
        val start = lastAt - (lastPos * 1000).toLong()''')
add(r'''        val cover = sg.thumb?.let { resolveImage(artUrl(it, 512)) }
        val logo = resolveImage(LOGO_URL)''', r'''        val cover = if (pics) sg.thumb?.let { resolveImage(artUrl(it, 512)) } else null
        val logo = if (pics) resolveImage(LOGO_URL) else null''')
add(r'''            client.newCall(req).execute().use { r ->''', r'''            http.newCall(req).execute().use { r ->''')

bad = False
for old, new in edits:
    n = t.count(old)
    if n != 1:
        print("PROBLEM - found", n, "times:", old[:70].replace("\n", " | "))
        bad = True
    else:
        t = t.replace(old, new)
if bad:
    print("\nNothing was changed. Send me the lines above.")
    sys.exit(1)
open(P, "w", encoding="utf-8").write(t)
print("patched Discord.kt")
print("\nDone. Now run: cd ~/PrismWave && git add -A && git commit -m discord-skip && git push")
