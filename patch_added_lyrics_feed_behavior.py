import os, sys

base = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")


def load(name):
    return open(base + name, encoding="utf-8").read()


def save(name, text):
    open(base + name, "w", encoding="utf-8").write(text)


def rep(text, old, new, what):
    n = text.count(old)
    if n != 1:
        print("FAILED (%s): found %d times" % (what, n))
        sys.exit(1)
    return text.replace(old, new)


# ------------------------------------------------ 1) lyrics: letter by letter fill with a soft moving glow
s = load("WordLyrics.kt")
if "each letter fills" in s:
    print("WordLyrics.kt already patched")
else:
    a = s.find("        val now = clock()\n        buildAnnotatedString {")
    b = s.find("    Text(\n        body,")
    if a < 0 or b < 0 or b < a:
        print("FAILED (WordLyrics block)")
        sys.exit(1)
    new = '''        val now = clock()
        buildAnnotatedString {
            words.forEachIndexed { i, w ->
                val len = (w.endMs - w.startMs).coerceAtLeast(120L).toFloat()
                val f = ((now - w.startMs) / len).coerceIn(0f, 1f)
                val n = w.text.length.coerceAtLeast(1)
                val edge = 1.8f
                val after = if (now <= w.endMs) 0f else ((now - w.endMs) / 500f).coerceIn(0f, 1f)
                for (c in 0 until n) {
                    // each letter fills one after the other with a soft edge, and the edge glows
                    val cf = ((f * (n + edge) - c) / edge).coerceIn(0f, 1f)
                    val lit = if (cf <= 0f) 0f
                    else if (now <= w.endMs) kotlin.math.sin(Math.PI * cf).toFloat().coerceAtLeast(0.30f)
                    else 0.30f * (1f - after)
                    val st = if (lit > 0.02f) {
                        SpanStyle(
                            color = lerp(dim, Color.White, cf),
                            shadow = Shadow(Color.White.copy(alpha = 0.9f * lit), Offset.Zero, 28f * lit)
                        )
                    } else {
                        SpanStyle(color = lerp(dim, Color.White, cf))
                    }
                    withStyle(st) { append(w.text[c].toString()) }
                }
                if (i < words.lastIndex) append(" ")
            }
        }
    }
'''
    s = s[:a] + new + s[b:]
    save("WordLyrics.kt", s)
    print("patched WordLyrics.kt")

# ------------------------------------------------ 3) Vibrant theme as the default (once)
s = load("Pages.kt")
if "themeVibrant" in s:
    print("Pages.kt already patched")
else:
    s = rep(s, '        theme = p.getInt("theme", 4)\n',
            '''        // everyone moves once to the Vibrant theme, after that the choice is theirs
        if (!p.getBoolean("themeVibrant", false)) {
            p.edit().putInt("theme", 5).putBoolean("themeVibrant", true).apply()
        }
        theme = p.getInt("theme", 5)
''', "theme")
    save("Pages.kt", s)
    print("patched Pages.kt")

# ------------------------------------------------ 2) Material 3 progress bar as the default (once)
s = load("Extras2.kt")
if "seekM3" in s:
    print("Extras2.kt already patched")
else:
    s = rep(s, '        style = ctx.getSharedPreferences("prismwave", 0).getInt("seekStyle", 0)\n',
            '''        val sp = ctx.getSharedPreferences("prismwave", 0)
        // everyone moves once to the Material 3 progress bar, after that the choice is theirs
        if (!sp.getBoolean("seekM3", false)) {
            sp.edit().putInt("seekStyle", 1).putBoolean("seekM3", true).apply()
        }
        style = sp.getInt("seekStyle", 1)
''', "seek")
    save("Extras2.kt", s)
    print("patched Extras2.kt")

# ------------------------------------------------ 4) Home feed refreshes itself every 10 minutes
s = load("Fresh.kt")
if "lastAt" in s:
    print("Fresh.kt already patched")
else:
    s = rep(s, "    var busy by mutableStateOf(false)\n",
            "    var busy by mutableStateOf(false)\n    var stamp by mutableStateOf(0L)\n    private var lastAt = 0L\n", "fields")
    s = rep(s, '                .putInt("day", day)\n',
            '                .putInt("day", day)\n                .putLong("ts", System.currentTimeMillis())\n', "save ts")
    s = rep(s, '        if (p.getInt("day", 0) != day || p.getInt("v", 0) != 2) return false\n',
            '        val ts = p.getLong("ts", 0L)\n        if (System.currentTimeMillis() - ts > 10L * 60L * 1000L || p.getInt("v", 0) != 2) return false\n', "cache age")
    s = rep(s, "            quick = qs\n            shelves.clear()\n            shelves.addAll(ss)\n            true\n",
            "            quick = qs\n            shelves.clear()\n            shelves.addAll(ss)\n            lastAt = ts\n            stamp = ts\n            true\n", "load stamp")
    s = rep(s, "        if (!force && quick.isNotEmpty()) return\n",
            "        if (!force && quick.isNotEmpty() && System.currentTimeMillis() - lastAt < 10L * 60L * 1000L) return\n", "memory age")
    s = rep(s, "                    shelves.addAll(built.second)\n                    save(day)\n",
            "                    shelves.addAll(built.second)\n                    lastAt = System.currentTimeMillis()\n                    stamp = lastAt\n                    save(day)\n", "built stamp")
    s = rep(s, "        val rng = if (force) Random(System.nanoTime()) else Random(dayKey().toLong() * 7919L)\n",
            "        val rng = Random(System.nanoTime())\n", "rng")
    s = rep(s, "now - it.value < 14L * 24 * 3600 * 1000", "now - it.value < 6L * 3600 * 1000", "seen window")
    s = rep(s, "        if (!Opts.offline) Fresh.refresh(ctx, s)\n    }\n}\n",
            '''        if (!Opts.offline) Fresh.refresh(ctx, s)
    }
    // a new set of picks every 10 minutes, so the Home never shows the same songs for days
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10L * 60L * 1000L)
            if (BehaviorPrefs.autoRefresh && !Opts.offline) {
                Fresh.refresh(ctx, s, true)
                s.reloadYtHome()
            }
        }
    }
}
''', "effect")
    save("Fresh.kt", s)
    print("patched Fresh.kt")

s = load("State.kt")
if "fun reloadYtHome" in s:
    print("State.kt already patched")
else:
    s = rep(s, "    // the real YouTube Music Home of the signed-in account\n    fun loadYtHome() {",
            "    fun reloadYtHome() {\n        ytHomeLoaded = false\n        loadYtHome()\n    }\n\n    // the real YouTube Music Home of the signed-in account\n    fun loadYtHome() {", "reloadYtHome")
    save("State.kt", s)
    print("patched State.kt")

s = load("Screens.kt")
if "Fresh.stamp" in s:
    print("Screens.kt already patched")
else:
    s = rep(s, "    val heroList = if (ytList != null) (ytList.take(8) + (freshList ?: emptyList())).distinctBy { it.id }.take(14) else freshList\n",
            '''    val heroList = if (ytList != null) {
        // fresh songs first, then a different handful of your YouTube Music picks each time
        (freshList.orEmpty().take(8) + ytList.shuffled(java.util.Random(Fresh.stamp)).take(6) + freshList.orEmpty())
            .distinctBy { it.id }.take(14)
    } else freshList
''', "hero")
    save("Screens.kt", s)
    print("patched Screens.kt")

# ------------------------------------------------ 6) Behavior + About pages, and BehaviorEffects
s = load("MainActivity.kt")
if "BehaviorEffects" in s:
    print("MainActivity.kt already patched")
else:
    s = rep(s, "            ScrobbleWatcher(s)\n", "            ScrobbleWatcher(s)\n            BehaviorEffects(s)\n", "effects")
    save("MainActivity.kt", s)
    print("patched MainActivity.kt")

s = load("Settings2.kt")
if "BehaviorPrefs.keepAwake" in s:
    print("Settings2.kt already patched")
else:
    s = rep(s, '''        PfSwitch(Icons.Rounded.TouchApp, "Bounce on tap", "Buttons squish when pressed", Opts.bounce, a) {
            Opts.bounce = it
            Opts.save()
        }
    }
}
''', '''        PfSwitch(Icons.Rounded.TouchApp, "Bounce on tap", "Buttons squish when pressed", Opts.bounce, a) {
            Opts.bounce = it
            Opts.save()
        }
        PfSwitch(Icons.Rounded.TouchApp, "Buzz on new song", "A small vibration when the next song starts", BehaviorPrefs.buzz, a) {
            BehaviorPrefs.buzz = it
            BehaviorPrefs.save(bctx)
        }
    }
    PfLabel("While listening", a)
    PfGroup {
        PfSwitch(Icons.Rounded.Speed, "Keep screen on", "The screen stays awake while a song plays", BehaviorPrefs.keepAwake, a) {
            BehaviorPrefs.keepAwake = it
            BehaviorPrefs.save(bctx)
        }
        PfSwitch(Icons.Rounded.TouchApp, "Shake to skip", "Shake your phone to go to the next song while PrismWave is open", BehaviorPrefs.shake, a) {
            BehaviorPrefs.shake = it
            BehaviorPrefs.save(bctx)
        }
        if (BehaviorPrefs.shake) {
            PfChoice(
                Icons.Rounded.TouchApp, "Shake strength", listOf("Gentle", "Normal", "Hard"), BehaviorPrefs.shakeLevel, a,
                "How hard you need to shake"
            ) {
                BehaviorPrefs.shakeLevel = it
                BehaviorPrefs.save(bctx)
            }
        }
    }
    PfLabel("Home", a)
    PfGroup {
        PfSwitch(Icons.Rounded.Sync, "Refresh Home every 10 minutes", "New picks without tapping Refresh", BehaviorPrefs.autoRefresh, a) {
            BehaviorPrefs.autoRefresh = it
            BehaviorPrefs.save(bctx)
        }
    }
}
''', "behavior tail")
    s = rep(s, "private fun PfBehaviorPage(s: AppState) {\n    val a = s.accent\n",
            "private fun PfBehaviorPage(s: AppState) {\n    val a = s.accent\n    val bctx = LocalContext.current\n    remember { BehaviorPrefs.init(bctx); 0 }\n", "behavior head")
    tg = "https://t.me/+1A-2I7QrBFIzMTU0"
    s = rep(s, '''        PfNote("Private build, made from scratch. Build " + build)
    }
}
''', '''        PfNote("Private build, made from scratch. Build " + build)
    }
    PfLabel("Your account", a)
    PfGroup {
        PfInfo(Icons.Rounded.AccountCircle, "Account", if (s.signedIn) (ProfilePic.name ?: "YouTube Music") else "Not signed in", a)
        PfInfo(Icons.Rounded.PlayArrow, "Songs played", s.playCount.toString(), a)
        PfInfo(
            Icons.Rounded.Speed, "Listening time",
            (s.listenSeconds / 3600).toString() + " h " + ((s.listenSeconds / 60) % 60).toString() + " min", a
        )
        PfInfo(Icons.Rounded.MusicNote, "Liked songs", s.liked.size.toString(), a)
        PfInfo(Icons.Rounded.Folder, "Playlists", s.playlists.size.toString(), a)
    }
    PfLabel("Community", a)
    PfGroup {
        PfAction(Icons.Rounded.AutoAwesome, "Join Telegram", "Updates, help and new builds", a) {
            try {
                uctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("TGURL")))
            } catch (e: Exception) {
            }
        }
        PfAction(Icons.Rounded.Info, "Report a problem", "Tell us in the Telegram group", a) {
            try {
                uctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("TGURL")))
            } catch (e: Exception) {
            }
        }
    }
    PfLabel("Credits", a)
    PfGroup {
        PfInfo(Icons.Rounded.Info, "Made by", "flagsnerd", a)
        PfInfo(Icons.Rounded.MusicNote, "Songs and search", "NewPipeExtractor", a)
        PfInfo(Icons.Rounded.Palette, "Design", "Material 3 + ArchiveTune style", a)
    }
}
'''.replace("TGURL", tg), "about tail")
    s = rep(s, "private fun PfChangelogPage(s: AppState) {\n    val a = s.accent\n",
            """private fun PfChangelogPage(s: AppState) {
    val a = s.accent
    PfChange("Added letter by letter lyrics glow, new defaults, auto refresh and more behavior", a, listOf(
        "Lyrics: every letter fills when it is sung, with a soft moving glow",
        "Material 3 progress bar and the Vibrant theme are the new defaults",
        "Home picks refresh by themselves every 10 minutes (Behavior, Home)",
        "Behavior: keep screen on, shake to skip, buzz on new song",
        "About: your account numbers, Join Telegram and credits"
    ))
""", "changelog")
    save("Settings2.kt", s)
    print("patched Settings2.kt")
print("done")
