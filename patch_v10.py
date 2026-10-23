import os, sys
d = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app")

def patch(fname, edits):
    path = os.path.join(d, fname)
    t = open(path).read()
    for old, new in edits:
        if new in t:
            continue
        if t.count(old) != 1:
            print("FAILED", fname, "anchor not found:", old[:60]); sys.exit(1)
        t = t.replace(old, new)
    open(path, "w").write(t)
    print("patched", fname)

patch("State.kt", [
('    init {\n        liked.addAll(loadSongs("liked"))',
 '    init {\n        DownloadStore.ensure(ctx)\n        liked.addAll(loadSongs("liked"))'),
('                val url = Engine.streamUrl(song.url)\n                var c = controller',
 '                val url = DownloadStore.localUri(song) ?: Engine.streamUrl(song.url)\n                var c = controller'),
])

patch("Components.kt", [
('    var pick by remember { mutableStateOf(false) }\n    if (pick) {\n        AddToPlaylistDialog(song, s) { pick = false; onDismiss() }',
 '    val ctx = LocalContext.current\n    var pick by remember { mutableStateOf(false) }\n    if (pick) {\n        AddToPlaylistDialog(song, s) { pick = false; onDismiss() }'),
('                    add("Add to playlist" to { pick = true })\n',
 '''                    add("Add to playlist" to { pick = true })
                    add((if (DownloadStore.isDownloaded(song)) "Remove download" else "Download") to {
                        if (DownloadStore.isDownloaded(song)) DownloadStore.remove(ctx, song) else DownloadStore.start(ctx, song)
                        onDismiss()
                    })
'''),
])

patch("MainActivity.kt", [
('                            Box(Modifier.fillMaxSize().statusBarsPadding()) {\n                                when (scr) {',
 '                            Box(\n                                Modifier.fillMaxSize().then(\n                                    if (scr == Screen.Library && s.openPlaylist != null) Modifier else Modifier.statusBarsPadding()\n                                )\n                            ) {\n                                when (scr) {'),
])
print("done")
