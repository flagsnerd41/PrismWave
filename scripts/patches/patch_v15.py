import os, sys

p = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/Screens.kt")
t = open(p, encoding="utf-8").read()


def rep(old, new):
    global t
    if old not in t:
        sys.exit("MISSING: " + old[:80])
    t = t.replace(old, new, 1)


rep('SettingsItem("about", "About", "Version and build info", Icons.Rounded.Info)',
    'SettingsItem("changelog", "What\'s new", "Changes in each version", Icons.Rounded.History),\n'
    '                    SettingsItem("about", "About", "Version and build info", Icons.Rounded.Info)')
rep('"about" to "About"', '"about" to "About",\n        "changelog" to "What\'s new"')
rep('Text("Version v12", color = Color.White)', 'Text("Version v15", color = Color.White)')
rep('@Composable\nprivate fun ToggleRow(',
    '''@Composable
private fun ChangeList(lines: List<String>) {
    lines.forEach { Text("•  " + it, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp)) }
}

@Composable
private fun ToggleRow(''')
rep('                "about" -> {',
    '''                "changelog" -> {
                    Section("v15") { ChangeList(listOf("What's new page in Settings")) }
                    Section("v14") {
                        ChangeList(listOf(
                            "Swipe a song row: right to like, left to queue",
                            "Swipe the mini player to skip songs",
                            "Glowing cover shadow in the Album player",
                            "Shimmer placeholders instead of spinners"
                        ))
                    }
                    Section("v13") {
                        ChangeList(listOf(
                            "Big Keep listening card on Home",
                            "Tempo and Pitch is a middle dialog again",
                            "Three-dots menu is a bottom sheet again"
                        ))
                    }
                    Section("v12") {
                        ChangeList(listOf(
                            "Slide-in pages for queue, sleep, add to playlist, details, stats",
                            "Blurred status bar on every screen",
                            "Behavior, Content and About settings",
                            "Pitch - and + buttons"
                        ))
                    }
                    Section("v11 and earlier") {
                        ChangeList(listOf(
                            "Tempo and Pitch, downloads, Apple Music style playlist page",
                            "ArchiveTune style Home and Immersive player",
                            "Duration bug fixed",
                            "YouTube Music search, streaming, backup and sign-in"
                        ))
                    }
                }
                "about" -> {''')
open(p, "w", encoding="utf-8").write(t)
print("v15 patch applied")
