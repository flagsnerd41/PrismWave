import pathlib
import sys

base = pathlib.Path.home() / "PrismWave/app/src/main/java/com/prismwave/app"
ok = True
files = {}


def load(n):
    if n not in files:
        files[n] = (base / n).read_text()
    return files[n]


def step(file, name, marker, old, new):
    global ok
    t = load(file)
    if marker in t:
        print("OK    " + name + " (already applied)")
        return
    if old not in t:
        print("FAIL  " + name)
        ok = False
        return
    files[file] = t.replace(old, new, 1)
    print("OK    " + name)


V = "Video.kt"
step(V, "default quality 720p", '"videoQuality2", 2',
     '.getInt("videoQuality", 0)', '.getInt("videoQuality2", 2)')
step(V, "save quality", 'putInt("videoQuality2", q)',
     'putInt("videoQuality", q)', 'putInt("videoQuality2", q)')
step(V, "fast players", "VideoWarm.take(song, low)",
     "val player = remember { ExoPlayer.Builder(ctx).build().apply { volume = 0f } }",
     "val player = remember { VideoWarm.take(song, low) ?: VideoWarm.newPlayer(ctx, low) }")
step(V, "mark video used", "VideoWarm.markUsed(ctx)",
     "    DisposableEffect(player) {\n        if (low) VideoSync.back = player else VideoSync.main = player\n",
     "    DisposableEffect(player) {\n        VideoWarm.markUsed(ctx)\n        if (low) VideoSync.back = player else VideoSync.main = player\n")
step(V, "ready player is shown at once", "val vs0 = player.videoSize",
     "        player.addListener(l)\n        onDispose {",
     "        player.addListener(l)\n"
     "        if (player.playbackState == Player.STATE_READY) ready = true\n"
     "        val vs0 = player.videoSize\n"
     "        if (vs0.width > 0) vsize = IntSize(vs0.width, vs0.height)\n"
     "        onDispose {")
step(V, "reuse a prepared video", "setMediaId(song.id)",
     "        player.setMediaItem(MediaItem.fromUri(url))\n        player.prepare()\n        player.seekTo((s.position * 1000).toLong())\n",
     "        if (player.currentMediaItem?.mediaId != song.id) {\n"
     "            player.setMediaItem(MediaItem.Builder().setUri(url).setMediaId(song.id).build())\n"
     "            player.prepare()\n"
     "        }\n"
     "        player.seekTo((s.position * 1000).toLong())\n"
     "        if (player.playbackState == Player.STATE_READY) ready = true\n")

# background loading while the full player is open
M = "MainActivity.kt"
t = load(M)
if "VideoWarmup(s)" in t:
    print("OK    background loading (already applied)")
else:
    import re
    m = re.search(r"(?m)^([ \t]*)PagesHost\(s\)[ \t]*\n", t)
    if m:
        files[M] = t[:m.end()] + m.group(1) + "VideoWarmup(s)\n" + t[m.end():]
        print("OK    background loading")
    else:
        print("FAIL  background loading")
        ok = False

if ok:
    for n, tx in files.items():
        (base / n).write_text(tx)
    print("\nAll good. Now: git add -A && git commit -m v35 && git push")
else:
    print("\nSomething did not match, nothing was changed. Send me this screen.")
    sys.exit(1)
