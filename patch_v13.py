import os, re, sys

D = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app/")
names = ["Player.kt", "Screens.kt"]
files = {n: open(D + n, encoding="utf-8").read() for n in names}


def rep(n, old, new):
    if old not in files[n]:
        sys.exit("MISSING in " + n + ": " + old[:80])
    files[n] = files[n].replace(old, new)


def cut(n, start, end, new):
    t = files[n]
    i = t.find(start)
    if i < 0:
        sys.exit("MISSING start in " + n + ": " + start[:80])
    j = t.find(end, i)
    if j < 0:
        sys.exit("MISSING end in " + n + ": " + end[:80])
    files[n] = t[:i] + new + t[j:]


OLD_A = '''        AnimatedVisibility(visible = visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { close() }
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = 300f)) { it },
            exit = slideOutVertically(tween(220)) { it }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxH)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Night)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                Box(
                    Modifier
                        .padding(vertical = 14.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(44.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                )
'''
# 1) three-dots menu goes back to the bottom slide-in sheet
cut("Player.kt",
    "        AnimatedVisibility(\n            visible = visible,\n            enter = slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),",
    "                Row(\n                    Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(CardBg).padding(16.dp),",
    OLD_A)
# 2) Tempo and Pitch opens as the middle dialog again
rep("Player.kt",
    ") { Pages.show(\"tempo\") }\n                }\n            }\n            }\n        }\n    }\n",
    ") { tempoOpen = true }\n                }\n            }\n        }\n    }\n    if (tempoOpen) TempoPitchDialog(s) { tempoOpen = false }\n")

# 3) Keep listening featured card on Home
t = files["Screens.kt"]
t, k = re.subn(
    r'item \{ SectionTitle\("Keep listening"\) \}\s*item \{ SongShelf\(history, s\) \}',
    'item { FeaturedCard(history.first(), history, s) }\n'
    '                if (history.size > 1) {\n'
    '                    item { Spacer(Modifier.height(14.dp)) }\n'
    '                    item { SongShelf(history.drop(1), s) }\n'
    '                }', t)
if k != 1:
    sys.exit("MISSING Keep listening block")
files["Screens.kt"] = t

for n in names:
    open(D + n, "w", encoding="utf-8").write(files[n])
print("v13 patch applied")
