#!/usr/bin/env python3
# added fresh daily recommendations, real Mood & Genres, and a typing box that grows out of the Search tab
# run from anywhere:  python3 ~/PrismWave/patch_added_fresh_moods_search.py
import os
import re
import sys

ROOT = os.path.expanduser("~/PrismWave/app/src/main/java/com/prismwave/app")


def load(name):
    with open(os.path.join(ROOT, name), encoding="utf-8") as f:
        return f.read()


def must_replace(text, old, new, label):
    n = text.count(old)
    if n != 1:
        print("FAILED: " + label + " (found " + str(n) + " times, needed 1)")
        sys.exit(1)
    return text.replace(old, new, 1)


def swap_between(text, start, end, new, label):
    i0 = text.find(start)
    i1 = text.find(end)
    if i0 < 0 or i1 < 0 or i1 < i0 or text.count(start) != 1:
        print("FAILED: " + label + " markers (" + str(i0) + ", " + str(i1) + ")")
        sys.exit(1)
    return text[:i0] + new + text[i1:]


files = {}

# ---------------------------------------------------------------- Screens.kt
t = load("Screens.kt")

# a "Mood & Genres" chip first on Home
t = must_replace(
    t,
    "                    items(moods) { c ->\n",
    "                    item {\n"
    "                        Chip(\"Mood & Genres\", true, s.accent) {\n"
    "                            s.searchQuery = \"\"\n"
    "                            s.screen = Screen.Search\n"
    "                        }\n"
    "                    }\n"
    "                    items(moods) { c ->\n",
    "Home mood chip",
)

# fresh picks come first, then YouTube's own, then the old ones
t = must_replace(
    t,
    "    val bestQuick = ytQuick ?: picks ?: quick\n"
    "    val community = (chill + workout).distinctBy { it.id }.take(20)\n",
    "    FreshEffect(s)\n"
    "    val bestQuick = Fresh.quick.takeIf { it.size >= 6 } ?: ytQuick ?: picks ?: quick\n"
    "    val community = (chill + workout).distinctBy { it.id }.take(20)\n",
    "Fresh quick",
)
t = must_replace(
    t,
    "            item { HomeSection(if (ytQuick == null && picks != null) \"Picked for you\" else \"Quick picks\") }\n"
    "            item {\n"
    "                if (bestQuick == null) LoadingRow(s) else QuickPickCarousel(bestQuick, s)\n"
    "            }\n",
    "            item {\n"
    "                HomeSection(\n"
    "                    if (Fresh.quick.size >= 6) \"Fresh for you\"\n"
    "                    else if (ytQuick == null && picks != null) \"Picked for you\"\n"
    "                    else \"Quick picks\"\n"
    "                )\n"
    "            }\n"
    "            item {\n"
    "                if (bestQuick == null) LoadingRow(s) else QuickPickCarousel(bestQuick, s)\n"
    "            }\n"
    "            item { FreshHome(s) }\n",
    "Fresh shelves",
)

# Search: load the real categories, drop the old text box (the bar at the bottom is the text box now)
t = must_replace(
    t,
    "    SearchHistory.init(searchCtx)\n",
    "    SearchHistory.init(searchCtx)\n    MoodApi.ensure()\n",
    "Mood load",
)
t = swap_between(
    t,
    "        item {\n            OutlinedTextField(\n                value = s.searchQuery,",
    "        if (!searching) {\n            if (SearchHistory.items.isNotEmpty() && s.searchQuery.isBlank()) {",
    "        item { Spacer(Modifier.height(4.dp)) }\n",
    "Search box",
)
t = must_replace(
    t,
    "                itemsIndexed(Moods.chunked(2)) { ri, pair ->\n",
    "                if (MoodApi.cats.isNotEmpty()) moodGridItems(s) else itemsIndexed(Moods.chunked(2)) { ri, pair ->\n",
    "Mood grid",
)
files["Screens.kt"] = t

# ---------------------------------------------------------------- MainActivity.kt
t = load("MainActivity.kt")
t = must_replace(
    t,
    "package com.prismwave.app\n",
    "package com.prismwave.app\nimport androidx.compose.foundation.layout.imePadding\n",
    "imePadding import",
)
t = must_replace(
    t,
    "            Column(\n"
    "                Modifier\n"
    "                    .align(Alignment.BottomCenter)\n"
    "                    .navigationBarsPadding()\n"
    "                    .padding(horizontal = 16.dp, vertical = 12.dp),\n"
    "                verticalArrangement = Arrangement.spacedBy(10.dp)",
    "            MoodsHost(s)\n"
    "            Column(\n"
    "                Modifier\n"
    "                    .align(Alignment.BottomCenter)\n"
    "                    .navigationBarsPadding()\n"
    "                    .imePadding()\n"
    "                    .padding(horizontal = 16.dp, vertical = 12.dp),\n"
    "                verticalArrangement = Arrangement.spacedBy(10.dp)",
    "Dock with keyboard",
)
t = must_replace(
    t,
    "                    s.openPlaylist != null -> s.openPlaylist = null\n",
    "                    MoodNav.playlist != null -> MoodNav.playlist = null\n"
    "                    MoodNav.cat != null -> MoodNav.cat = null\n"
    "                    s.openPlaylist != null -> s.openPlaylist = null\n",
    "Back button",
)
files["MainActivity.kt"] = t

# ---------------------------------------------------------------- Settings2.kt
t = load("Settings2.kt")
marker = "private fun PfChangelogPage(s: AppState) {\n    val a = s.accent\n"
t = must_replace(
    t,
    marker,
    marker +
    '    PfChange("Added fresh picks, real Mood & Genres and a typing box in the bar", a, listOf(\n'
    '        "Home: Fresh for you. New songs every day, built from what you play and like, and songs you already know or saw lately never come back",\n'
    '        "More shelves: Because you played, More from your top artist, Discover a new style, and a mood for the time of day. Refresh picks gives a new set",\n'
    '        "Mood & Genres from YouTube Music in Search, with colors and covers. Open a category to see its playlists and play them",\n'
    '        "Tap Search in the bottom bar and it grows into the typing box, with the keyboard ready"\n'
    '    ))\n',
    "Changelog",
)
files["Settings2.kt"] = t

# ---------------------------------------------------------------- Extras.kt (red dot on What's new)
t = load("Extras.kt")
t, n = re.subn(r'const val ID = "[^"]*"', 'const val ID = "added-fresh-moods-search"', t, count=1)
if n != 1:
    print("FAILED: WhatsNew id")
    sys.exit(1)
files["Extras.kt"] = t

for name, text in files.items():
    with open(os.path.join(ROOT, name), "w", encoding="utf-8") as f:
        f.write(text)
    print("patched " + name)
print("done")
