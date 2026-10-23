import os, sys
path = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/MainActivity.kt")
t = open(path).read()

if "TopBlur(" in t:
    print("MainActivity.kt already patched"); sys.exit(0)

def rep(old, new):
    global t
    if t.count(old) != 1:
        print("FAILED, anchor not found:", old[:60]); sys.exit(1)
    t = t.replace(old, new)

rep("import android.os.Build\n",
    "import android.os.Build\nimport androidx.compose.foundation.layout.WindowInsets\nimport androidx.compose.foundation.layout.asPaddingValues\nimport androidx.compose.foundation.layout.statusBars\n")

rep("                Box(Modifier.fillMaxSize().statusBarsPadding()) {\n                    AnimatedContent(",
    "                Box(Modifier.fillMaxSize()) {\n                    AnimatedContent(")

rep("""                        when (scr) {
                            Screen.Home -> HomeScreen(s)
                            Screen.Search -> SearchScreen(s)
                            Screen.Library -> LibraryScreen(s)
                            Screen.Settings -> SettingsScreen(s)
                        }
""", """                        if (scr == Screen.Home) {
                            HomeScreen(s)
                        } else {
                            Box(Modifier.fillMaxSize().statusBarsPadding()) {
                                when (scr) {
                                    Screen.Search -> SearchScreen(s)
                                    Screen.Library -> LibraryScreen(s)
                                    else -> SettingsScreen(s)
                                }
                            }
                        }
""")

rep("            Column(\n                Modifier\n                    .align(Alignment.BottomCenter)",
"""            AnimatedVisibility(
                visible = s.screen == Screen.Home,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) { TopBlur(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 18.dp) }
            Column(
                Modifier
                    .align(Alignment.BottomCenter)""")

open(path, "w").write(t)
print("patched MainActivity.kt")
