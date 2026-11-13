import os, re
base = os.path.join(os.path.dirname(os.path.abspath(__file__)), "app/src/main/java/com/prismwave/app")
bad = 0

def edit(name, pairs):
    global bad
    path = os.path.join(base, name)
    src = open(path, encoding="utf-8").read()
    for pat, rep, rx in pairs:
        n = len(re.findall(pat, src)) if rx else src.count(pat)
        if n != 1:
            print("PROBLEM in " + name + ": found " + str(n) + " times: " + pat[:50])
            bad += 1
            continue
        src = re.sub(pat, lambda m: rep, src) if rx else src.replace(pat, rep)
    open(path, "w", encoding="utf-8").write(src)

edit("Updater.kt", [
    ("    suspend fun download(ctx: Context, i: UpdateInfo)",
     "    var status by mutableStateOf(\"\")\n\n    // the Check for updates button in Settings\n    suspend fun checkNow(ctx: Context) {\n        status = \"Checking...\"\n        checked = false\n        dismissed = false\n        info = null\n        check(ctx)\n        status = if (info != null) \"New version found\" else \"You have the latest version\"\n    }\n\n    suspend fun download(ctx: Context, i: UpdateInfo)", False),
])
edit("Settings2.kt", [
    (r'private fun PfAboutPage\(s: AppState\) \{\s*val a = s\.accent',
     "private fun PfAboutPage(s: AppState) {\n    val a = s.accent\n    val uctx = LocalContext.current\n    val uscope = rememberCoroutineScope()", True),
    ('PfInfo(Icons.Rounded.Info, "Version", "v27", a)',
     'PfInfo(Icons.Rounded.Info, "Version", "v27", a)\n        PfAction(\n            Icons.Rounded.Sync, "Check for updates",\n            if (Updater.status.isNotEmpty()) Updater.status else "Look for a newer version", a,\n            Updater.status != "Checking..."\n        ) { uscope.launch { Updater.checkNow(uctx) } }', False),
])
print("ALL GOOD" if bad == 0 else "Some edits failed: send me this screen")
