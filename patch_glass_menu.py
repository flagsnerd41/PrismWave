import os, re

path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "app/src/main/java/com/prismwave/app/Player.kt")
src = open(path, encoding="utf-8").read()
bad = 0

def rep(pat, new, rx):
    global src, bad
    n = len(re.findall(pat, src)) if rx else src.count(pat)
    if n != 1:
        print("PROBLEM: found " + str(n) + " times: " + pat[:60])
        bad += 1
        return
    src = re.sub(pat, lambda m: new, src) if rx else src.replace(pat, new)

rep("package com.prismwave.app\n", "package com.prismwave.app\n\nimport androidx.compose.foundation.border\n", False)

# the player behind the menu blurs while the menu is open
rep("    val lyShift by animateFloatAsState(",
    "    val menuBlur by animateDpAsState(\n        if (menuDialog && !Opts.lite && Build.VERSION.SDK_INT >= 31) 14.dp else 0.dp,\n        tween(300),\n        label = \"menublur\"\n    )\n    val lyShift by animateFloatAsState(", False)
rep(".clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))",
    ".then(if (menuBlur > 0.5.dp) Modifier.blur(menuBlur) else Modifier)\n            .clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp))", False)

# the menu card becomes frosted glass with a bright rim
rep(r'\.background\(Night\)\s*\.clickable\(indication = null, interactionSource = remember \{ MutableInteractionSource\(\) \}\) \{ \}',
    ".background(if (Opts.lite) Night else Night.copy(alpha = 0.62f))\n                .border(\n                    1.dp,\n                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.06f))),\n                    RoundedCornerShape(30.dp)\n                )\n                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }", True)

open(path, "w", encoding="utf-8").write(src)
print("ALL GOOD" if bad == 0 else "Some edits failed: send me this screen")
