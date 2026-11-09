import os, sys

here = os.path.dirname(os.path.abspath(__file__))
app = os.path.join(here, "app", "src", "main", "java", "com", "prismwave", "app")

# Screens.kt: stickyHeader is part of LazyColumn itself, there is nothing to import
p = os.path.join(app, "Screens.kt")
t = open(p, encoding="utf-8").read()
bad = "import androidx.compose.foundation.lazy.stickyHeader\n"
if bad in t:
    t = t.replace(bad, "")
    open(p, "w", encoding="utf-8").write(t)
    print("Screens.kt fixed: removed the wrong stickyHeader import")
else:
    print("Screens.kt already fine")
