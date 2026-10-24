package com.prismwave.app

import androidx.compose.animation.core.animateFloat

import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Speed
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ------------------------------------------------------------ shared bits

@Composable
fun Header(s: AppState) {
    var stats by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLogo(42.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            "PrismWave",
            color = Color.White,
            fontFamily = PrismFont,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            modifier = Modifier.weight(1f)
        )
        RoundBtn(Icons.Rounded.History, 0.9f, {
            s.openPlaylist = null
            s.libraryOpen = "History"
            s.screen = Screen.Library
        }, 46.dp)
        Spacer(Modifier.width(8.dp))
        RoundBtn(Icons.Rounded.BarChart, 0.9f, { stats = true }, 46.dp)
        Spacer(Modifier.width(8.dp))
        RoundBtn(Icons.Rounded.Settings, 0.9f, { s.screen = Screen.Settings }, 46.dp)
    }
    if (stats) {
        LaunchedEffect(Unit) {
            Pages.show("stats")
            stats = false
        }
    }
}

@Composable
fun LoadingRow(s: AppState) {
    val tr = androidx.compose.animation.core.rememberInfiniteTransition(label = "sk")
    val a by tr.animateFloat(
        0.05f, 0.16f,
        androidx.compose.animation.core.infiniteRepeatable(tween(850), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "ska"
    )
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = a)))
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.width(180.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(Color.White.copy(alpha = a)))
                    Box(Modifier.width(110.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = a)))
                }
            }
        }
    }
}

@Composable
fun QuickCard(song: Song, s: AppState, from: List<Song>, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier
            .softClick { s.play(song, from) }
            .width(300.dp)
            .height(340.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
    ) {
        Cover(song, Modifier.fillMaxSize(), 0.dp, 90.sp, quality = 800)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.85f))
                )
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
            Text(
                song.title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(song.artist, color = Color.White.copy(alpha = 0.75f), fontSize = 16.sp, maxLines = 1)
        }
    }
}

@Composable
fun RecentCard(song: Song, s: AppState, from: List<Song>, modifier: Modifier = Modifier) {
    Column(modifier.softClick { s.play(song, from) }.width(140.dp)) {
        Box {
            Cover(song, Modifier.size(140.dp), 24.dp, 44.sp)
            PlayBadge(s.accent, Modifier.align(Alignment.BottomEnd).padding(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(song.artist, color = TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SongRowList(title: String, songs: List<Song>, s: AppState) {
    if (songs.isEmpty()) return
    SectionTitle(title)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) { itemsIndexed(songs.take(12)) { i, sg -> RecentCard(sg, s, songs, Modifier.appear(i)) } }
}

// ------------------------------------------------------------ home

@Composable
private fun QuickPicksGrid(songs: List<Song>, s: AppState) {
    val cfg = LocalConfiguration.current
    val itemW = (cfg.screenWidthDp * 0.9f).dp
    val rows = minOf(4, songs.size).coerceAtLeast(1)
    LazyHorizontalGrid(
        rows = GridCells.Fixed(rows),
        modifier = Modifier.fillMaxWidth().height((66 * rows).dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        gridItems(songs.take(20)) { sg ->
            val cur = s.current?.id == sg.id
            Row(
                Modifier
                    .width(itemW)
                    .height(66.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (cur) s.accent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { s.play(sg, songs) }
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Cover(sg, Modifier.size(54.dp), 12.dp, 18.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        sg.title,
                        color = if (cur) s.accent else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        sg.artist,
                        color = TextDim,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SongShelf(songs: List<Song>, s: AppState) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) { itemsIndexed(songs.take(12)) { i, sg -> RecentCard(sg, s, songs, Modifier.appear(i)) } }
}

@Composable
fun HomeScreen(s: AppState) {
    LaunchedEffect(Opts.offline) {
        if (!Opts.offline) {
            s.loadFeed("quick", "new music hits")
            s.loadFeed("chill", "chill songs")
            s.loadFeed("workout", "workout songs")
        }
    }
    val quick = if (Opts.offline) s.offlineSongs() else s.feeds["quick"]
    val chill = if (Opts.offline) emptyList<Song>() else (s.feeds["chill"] ?: emptyList())
    val workout = if (Opts.offline) emptyList<Song>() else (s.feeds["workout"] ?: emptyList())
    val chips = listOf("Relax", "Workout", "Energize", "Party", "Commute", "Focus", "Sleep")
    val history = s.history.toList()
    val recentIds = history.map { it.id }.toSet()
    val forgotten = s.liked.filter { it.id !in recentIds }
    val speedDial = s.topPlayed()
    val ytPlaylists = s.playlists.filter { it.id.startsWith("yt_") }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(430.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        0f to s.accent.copy(alpha = 0.30f),
                        0.42f to s.accent.copy(alpha = 0.12f),
                        1f to Color.Transparent
                    )
                )
        )
        LazyColumn(
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = 220.dp
            )
        ) {
            item { Header(s) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Chip("Offline", Opts.offline, s.accent) {
                            Opts.offline = !Opts.offline
                            Opts.save()
                        }
                    }
                    items(chips) { c ->
                        Chip(c, false, s.accent) {
                            s.searchQuery = c + " music"
                            s.screen = Screen.Search
                        }
                    }
                }
            }
            if (history.isNotEmpty() && Opts.showKeep) {
                item { Spacer(Modifier.height(14.dp)) }
                item {
                    val cardW = (LocalConfiguration.current.screenWidthDp * 0.86f).dp
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) { items(history.take(3)) { sg -> FeaturedCard(sg, history, s, Modifier.width(cardW)) } }
                }
            }
            item { SectionTitle("Quick picks") }
            item {
                if (quick == null) LoadingRow(s) else QuickPicksGrid(quick, s)
            }
            if (history.size > 3 && Opts.showKeep) {
                item { Spacer(Modifier.height(18.dp)) }
                item { SectionTitle("Recently played") }
                item { SongShelf(history.drop(3), s) }
            }
            if (speedDial.size >= 3 && Opts.showSpeed) {
                item { Spacer(Modifier.height(18.dp)) }
                item { SectionTitle("Speed dial") }
                item { SongShelf(speedDial, s) }
            }
            if (ytPlaylists.isNotEmpty()) {
                item { Spacer(Modifier.height(18.dp)) }
                item { SectionTitle("Your playlists") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) { items(ytPlaylists.take(12)) { p -> PlaylistCard(p, s) } }
                }
            }
            if (forgotten.size >= 3 && Opts.showForgotten) {
                item { Spacer(Modifier.height(18.dp)) }
                item { SectionTitle("Forgotten favorites") }
                item { SongShelf(forgotten, s) }
            }
            item { Spacer(Modifier.height(18.dp)) }
            if (Opts.showMixes) {
                item { SongRowList("Chill", chill, s) }
                item { SongRowList("Workout", workout, s) }
            }
        }
    }
}

// ------------------------------------------------------------ search / explore

private data class Mood(val name: String, val query: String, val a: Color, val b: Color)

private val Moods = listOf(
    Mood("Jazz", "jazz music", Color(0xFF8FA3C8), Color(0xFF6C84AE)),
    Mood("2010s", "2010s hits", Color(0xFF7DBF74), Color(0xFF5E9E5A)),
    Mood("Romance", "romantic songs", Color(0xFFA82810), Color(0xFF7A1A0A)),
    Mood("1960s", "1960s hits", Color(0xFF7DBF74), Color(0xFF5E9E5A)),
    Mood("Sleep", "sleep music", Color(0xFF5A3A9A), Color(0xFF3F2680)),
    Mood("1950s", "1950s hits", Color(0xFF7DBF74), Color(0xFF5E9E5A)),
    Mood("Chill", "chill songs", Color(0xFF3E8E8A), Color(0xFF2B6B68)),
    Mood("2000s", "2000s hits", Color(0xFFB8864A), Color(0xFF8E6232)),
    Mood("Workout", "workout songs", Color(0xFFC0561F), Color(0xFF9A3E10)),
    Mood("1990s", "1990s hits", Color(0xFFB36A6A), Color(0xFF8E4A4A)),
    Mood("Party", "party songs", Color(0xFFC04A8A), Color(0xFF983070)),
    Mood("1980s", "1980s hits", Color(0xFF6A7BB8), Color(0xFF4A5A94)),
    Mood("Focus", "focus music", Color(0xFF4A8A5A), Color(0xFF326A40)),
    Mood("Hip-hop", "hip hop songs", Color(0xFF8A6A3A), Color(0xFF6A4E28)),
    Mood("Rock", "rock songs", Color(0xFF9A3A3A), Color(0xFF762626)),
    Mood("Classical", "classical music", Color(0xFF6A5A8A), Color(0xFF4E4070)),
    Mood("Lo-fi", "lofi beats", Color(0xFF5A8A9A), Color(0xFF3E6A7A)),
    Mood("Acoustic", "acoustic songs", Color(0xFFA8844A), Color(0xFF82642E))
)

@Composable
private fun MoodTile(m: Mood, s: AppState, modifier: Modifier) {
    Box(
        modifier
            .softClick { s.searchQuery = m.query }
            .height(96.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.horizontalGradient(listOf(m.b, m.a)))
            .padding(18.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.14f))
        )
        Text(
            m.name,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomStart),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SearchScreen(s: AppState) {
    var hits by remember { mutableStateOf<List<Hit>>(emptyList()) }
    var kind by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    LaunchedEffect(s.searchQuery, kind) {
        err = ""
        val q = s.searchQuery.trim()
        if (q.length < 2) {
            hits = emptyList()
            busy = false
            return@LaunchedEffect
        }
        delay(500)
        busy = true
        try {
            hits = if (Opts.offline) {
                s.offlineSongs()
                    .filter { it.title.contains(q, true) || it.artist.contains(q, true) }
                    .map { Hit(0, it.title, it.artist, it.thumb, it.url, it) }
            } else {
                Engine.searchHits(q, kind)
            }
        } catch (e: Exception) {
            err = "Search failed: " + (e.message ?: "unknown").take(80)
        }
        busy = false
    }
    val searching = s.searchQuery.trim().length >= 2
    LazyColumn(contentPadding = PaddingValues(top = statusTop(), bottom = 220.dp)) {
        item { Header(s) }
        item {
            OutlinedTextField(
                value = s.searchQuery,
                onValueChange = { s.searchQuery = it },
                singleLine = true,
                shape = RoundedCornerShape(30.dp),
                placeholder = { Text("Search YouTube Music", color = TextDim) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White) },
                trailingIcon = { Icon(Icons.Rounded.Public, contentDescription = null, tint = Color.White) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = CardBg2,
                    unfocusedContainerColor = CardBg2,
                    focusedBorderColor = s.accent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = s.accent
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )
        }
        if (!searching) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    listOf("Explore" to Icons.Rounded.Explore, "Suggestions" to Icons.Rounded.AutoAwesome)
                        .forEachIndexed { i, (label, icon) ->
                            Column(
                                Modifier.weight(1f).clickable { tab = i }.padding(top = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(icon, contentDescription = null, tint = if (tab == i) s.accent else TextDim)
                                Text(label, color = if (tab == i) s.accent else TextDim, fontSize = 15.sp)
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    Modifier
                                        .width(if (tab == i) 56.dp else 0.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(s.accent)
                                )
                            }
                        }
                }
            }
            if (tab == 0) {
                item { SectionTitle("Mood & Genres") }
                itemsIndexed(Moods.chunked(2)) { ri, pair ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { MoodTile(it, s, Modifier.weight(1f).appear(ri)) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                val h = s.history.toList()
                if (h.isNotEmpty()) {
                    item { SectionTitle("Recently played") }
                    items(h.take(8)) { SongRow(it, s, h) }
                }
                val q = s.feeds["quick"] ?: emptyList()
                if (q.isNotEmpty()) {
                    item { SectionTitle("Trending") }
                    items(q.take(8)) { SongRow(it, s, q) }
                }
            }
        } else {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(listOf("All", "Songs", "Videos", "Albums", "Artists", "Playlists")) { i, label ->
                        Chip(label, kind == i, s.accent) { kind = i }
                    }
                }
            }
            if (busy) item { LoadingRow(s) }
            if (err.isNotEmpty()) item { Text(err, color = TextDim, modifier = Modifier.padding(20.dp)) }
            if (!busy) {
                if (hits.isEmpty() && err.isEmpty()) {
                    item { Text("Nothing found", color = TextDim, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }
                }
                val songList = hits.mapNotNull { it.song }
                items(hits) { h ->
                    val sg = h.song
                    if (sg != null) SongRow(sg, s, songList) else HitRow(h, s)
                }
            }
        }
    }
}

// ------------------------------------------------------------ library

@Composable
private fun LibTile(
    title: String,
    sub: String,
    icon: ImageVector,
    iconBg: Color,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .softClick(onClick)
            .clip(RoundedCornerShape(26.dp))
            .background(CardBg)
            .padding(18.dp)
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = accent)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(sub, color = TextDim, fontSize = 14.sp)
    }
}

@Composable
private fun PlaylistCard(p: Playlist, s: AppState) {
    Column(
        Modifier
            .softClick { s.openPlaylist = p.id }
            .width(168.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(CardBg)
            .padding(12.dp)
    ) {
        val first = p.songs.firstOrNull()
        Box {
            if (first != null) {
                Cover(first, Modifier.size(144.dp), 20.dp, 44.sp)
            } else {
                Box(Modifier.size(144.dp).clip(RoundedCornerShape(20.dp)).background(CardBg2))
            }
            if (first != null) {
                PlayBadge(s.accent, Modifier.align(Alignment.BottomEnd).padding(8.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(p.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(p.songs.size.toString() + " tracks", color = TextDim, fontSize = 13.sp)
    }
}

private fun depthOf(page: String): Int = when {
    page.startsWith("pl:") -> 2
    page.startsWith("lib:") -> 1
    else -> 0
}

@Composable
fun LibraryScreen(s: AppState) {
    val page = when {
        s.openPlaylist != null -> "pl:" + s.openPlaylist
        s.libraryOpen != null -> "lib:" + s.libraryOpen
        else -> "main"
    }
    AnimatedContent(
        targetState = page,
        transitionSpec = {
            if (s.reduceMotion) {
                fadeIn(tween(120)) togetherWith fadeOut(tween(80))
            } else {
                val dir = if (depthOf(targetState) >= depthOf(initialState)) 1 else -1
                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> w * dir / 3 } +
                    fadeIn(tween(240))) togetherWith
                    (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> -w * dir / 3 } +
                        fadeOut(tween(160)))
            }
        },
        label = "library"
    ) { p ->
        when {
            p.startsWith("pl:") -> PlaylistScreen(s, p.removePrefix("pl:"))
            p.startsWith("lib:") -> SongListScreen(s, p.removePrefix("lib:"))
            else -> LibraryMain(s)
        }
    }
}

@Composable
private fun LibraryMain(s: AppState) {
    var creating by remember { mutableStateOf(false) }
    val top = s.topPlayed()
    LazyColumn(contentPadding = PaddingValues(top = statusTop(), bottom = 220.dp)) {
        item { Header(s) }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Chip("Library", s.libTab == 0, s.accent) { s.libTab = 0 } }
                item { Chip("Playlists", s.libTab == 1, s.accent) { s.libTab = 1 } }
            }
        }
        if (s.signedIn && s.accountMsg.isNotEmpty()) {
            item {
                Text(
                    s.accountMsg,
                    color = TextDim,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
        if (s.libTab == 1) {
            item {
                Row(
                    Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(s.accent)
                        .clickable { creating = true }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("New playlist", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
            }
            if (s.playlists.isEmpty()) {
                item { Text("No playlists yet", color = TextDim, modifier = Modifier.padding(horizontal = 20.dp)) }
            }
            items(s.playlists.toList()) { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { s.openPlaylist = p.id }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val first = p.songs.firstOrNull()
                    if (first != null) {
                        Cover(first, Modifier.size(60.dp), 14.dp)
                    } else {
                        Box(Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).background(CardBg2))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(p.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(p.songs.size.toString() + " tracks", color = TextDim, fontSize = 13.sp)
                    }
                }
            }
        } else {
            item {
                val first = top.firstOrNull()
                Column(
                    Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(30.dp))
                        .background(CardBg2)
                        .padding(20.dp)
                ) {
                    Row {
                        if (first != null) {
                            Cover(first, Modifier.size(96.dp), 18.dp, 34.sp)
                            Spacer(Modifier.width(16.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(s.accent.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) { Text("MOST PLAYED", color = s.accent, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                first?.title ?: "Play something first",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(top.size.toString() + " songs", color = TextDim, fontSize = 15.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(s.accent)
                                .clickable { if (first != null) s.play(first, top) }
                                .padding(horizontal = 22.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text("Play all", color = Color.Black, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.weight(1f))
                        RoundBtn(Icons.Rounded.ArrowForward, 0.9f, { s.libraryOpen = "My top 50" })
                    }
                }
            }
            val pink = Color(0xFF5A3A3A)
            val olive = Color(0xFF4A4224)
            val tiles = listOf(
                listOf("Liked songs", s.liked.size.toString() + " tracks", Icons.Rounded.Favorite, pink, Color(0xFFFF8A9A)),
                listOf("Offline", "Downloaded", Icons.Rounded.Download, olive, s.accent),
                listOf("Cached", "Instant playback", Icons.Rounded.Sync, olive, s.accent),
                listOf("Local Files", "On device", Icons.Rounded.Folder, olive, s.accent),
                listOf("My top 50", "All time", Icons.Rounded.TrendingUp, olive, s.accent)
            )
            itemsIndexed(tiles.chunked(2)) { ri, pair ->
                Row(
                    Modifier.appear(ri).fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { t ->
                        LibTile(
                            t[0] as String, t[1] as String, t[2] as ImageVector,
                            t[3] as Color, t[4] as Color, Modifier.weight(1f)
                        ) { s.libraryOpen = t[0] as String }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item { SongRowList("Recently played", s.history.toList(), s) }
            item {
                SectionTitle("Your playlists", "See all") { s.libTab = 1 }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(s.playlists.toList()) { PlaylistCard(it, s) }
                    item {
                        Column(
                            Modifier
                                .width(168.dp)
                                .height(214.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(CardBg)
                                .clickable { creating = true },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = s.accent)
                            Text("New playlist", color = Color.White)
                        }
                    }
                }
            }
            val artists = (s.history.toList() + s.liked.toList()).distinctBy { it.artist }.take(12)
            if (artists.isNotEmpty()) {
                item { SectionTitle("Your artists") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(artists) { a ->
                            Column(
                                Modifier
                                    .width(88.dp)
                                    .clickable {
                                        s.searchQuery = a.artist
                                        s.screen = Screen.Search
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Cover(a, Modifier.size(88.dp), 44.dp, 30.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(a.artist, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
    if (creating) {
        NamePlaylistDialog("New playlist", "", { n ->
            val id = s.createPlaylist(n)
            creating = false
            s.openPlaylist = id
        }, { creating = false })
    }
}

@Composable
fun SongListScreen(s: AppState, title: String) {
    val songs = when (title) {
        "Liked songs" -> s.liked.toList()
        "History" -> s.history.toList()
        "My top 50" -> s.topPlayed()
        "Offline" -> DownloadStore.songs.toList()
        "Cached" -> s.cachedSongs()
        else -> emptyList()
    }
    LazyColumn(contentPadding = PaddingValues(top = statusTop(), bottom = 220.dp)) {
        item {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundBtn(Icons.Rounded.ArrowBack, 0.9f, { s.libraryOpen = null })
                Spacer(Modifier.width(14.dp))
                Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (songs.isEmpty()) {
            item {
                Text(
                    when (title) {
                        "Liked songs" -> "No liked songs yet. Open a song's menu and tap Like."
                        "Offline" -> "No downloads yet. Open a song's menu and tap Download."
                        "History", "My top 50" -> "Play something first"
                        "Cached" -> "Songs you play are kept here for instant, offline playback."
                        else -> "Coming in a later wave"
                    },
                    color = TextDim,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
        if (songs.isNotEmpty()) {
            item {
                Row(
                    Modifier
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(s.accent)
                        .clickable { s.play(songs[0], songs) }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("Play all", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        items(songs) { SongRow(it, s, songs) }
    }
}

// ------------------------------------------------------------ playlist page

@Composable
fun PlaylistScreen(s: AppState, id: String) {
    val pl = s.playlists.find { it.id == id }
    if (pl == null) {
        LaunchedEffect(Unit) { s.openPlaylist = null }
        return
    }
    val ctx = LocalContext.current
    var unlocked by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    val total = pl.songs.sumOf { it.seconds }
    val first = pl.songs.firstOrNull()
    val art = if (first != null) rememberArtColor(first) else s.accent
    val tintTop by animateColorAsState(lerp(Night, art, 0.60f), tween(600), label = "pltop")
    val tintBot by animateColorAsState(lerp(Night, art, 0.20f), tween(600), label = "plbot")
    val allDown = pl.songs.isNotEmpty() && pl.songs.all { DownloadStore.isDownloaded(it) }
    val busy = pl.songs.any { DownloadStore.progress.containsKey(it.id) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(tintTop, tintBot)))) {
        LazyColumn(
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = 220.dp
            )
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundBtn(Icons.Rounded.ArrowBack, 0.6f, { s.openPlaylist = null })
                    Spacer(Modifier.weight(1f))
                    RoundBtn(
                        if (unlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        0.6f,
                        { unlocked = !unlocked },
                        tint = if (unlocked) s.accent else Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    RoundBtn(Icons.Rounded.MoreVert, 0.6f, { menu = true })
                }
            }
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp, vertical = 8.dp)
                        .aspectRatio(1f)
                        .shadow(24.dp, RoundedCornerShape(20.dp))
                ) {
                    if (first != null) {
                        Cover(first, Modifier.fillMaxSize(), 20.dp, 120.sp, quality = 1200)
                    } else {
                        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(CardBg2))
                    }
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        pl.name,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        pl.songs.size.toString() + " songs  •  " + fmtLong(total),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .bounceClick {
                                if (pl.songs.isNotEmpty()) {
                                    s.shuffle = true
                                    s.play(pl.songs.random(), pl.songs)
                                }
                            }
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.30f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Row(
                        Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.42f))
                            .bounceClick { if (pl.songs.isNotEmpty()) s.play(pl.songs[0], pl.songs) },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Play", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        Modifier
                            .bounceClick {
                                if (allDown) {
                                    Toast.makeText(ctx, "All songs are downloaded. Remove them from a song's menu.", Toast.LENGTH_SHORT).show()
                                } else {
                                    DownloadStore.startAll(ctx, pl.songs)
                                }
                            }
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.30f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            busy -> CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            allDown -> Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = s.accent, modifier = Modifier.size(26.dp))
                            else -> Icon(Icons.Rounded.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(14.dp)) }
            if (pl.songs.isEmpty()) {
                item {
                    Text(
                        "Empty. Open any song's menu and tap Add to playlist.",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
            itemsIndexed(pl.songs, key = { _, sg -> sg.id }) { i, song ->
                val cur = s.current?.id == song.id
                val down = DownloadStore.isDownloaded(song)
                var rowMenu by remember { mutableStateOf(false) }
                Column(Modifier.animateItem()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (cur) s.accent.copy(alpha = 0.16f) else Color.Transparent)
                            .clickable { s.play(song, pl.songs) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            (i + 1).toString(),
                            color = if (cur) s.accent else Color.White.copy(alpha = 0.6f),
                            fontSize = 15.sp,
                            modifier = Modifier.width(30.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                song.title,
                                color = if (cur) s.accent else Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                song.artist + "  •  " + fmt(song.seconds),
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (down) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = s.accent,
                                modifier = Modifier.padding(end = 8.dp).size(18.dp)
                            )
                        }
                        if (unlocked) {
                            Icon(
                                Icons.Rounded.KeyboardArrowUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp).clickable { s.moveInPlaylist(pl.id, i, -1) }
                            )
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(34.dp).clickable { s.moveInPlaylist(pl.id, i, 1) }
                            )
                        } else {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(28.dp).clickable { rowMenu = true }
                            )
                        }
                    }
                    Box(Modifier.padding(start = 50.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                }
                if (rowMenu) SongMenu(song, s, pl.id) { rowMenu = false }
            }
        }
    }
    if (menu) {
        LaunchedEffect(Unit) {
            Pages.show("plmenu", null, pl.id)
            menu = false
        }
    }
    if (renaming) {
        NamePlaylistDialog("Rename playlist", pl.name, { n ->
            s.renamePlaylist(pl.id, n)
            renaming = false
        }, { renaming = false })
    }
}

// ------------------------------------------------------------ settings

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBg)
            .padding(18.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ActionButton(text: String, accent: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .bounceClick { if (enabled) onClick() }
            .clip(RoundedCornerShape(50))
            .background(if (enabled) accent else CardBg2)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(text, color = if (enabled) Color.Black else TextDim, fontWeight = FontWeight.SemiBold)
    }
}

private class SettingsItem(val key: String, val title: String, val subtitle: String, val icon: ImageVector)

@Composable
private fun SettingsGroup(items: List<SettingsItem>, accent: Color, base: Int, onOpen: (String) -> Unit) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { i, item ->
            val big = 28.dp
            val small = 6.dp
            val top = if (i == 0) big else small
            val bottom = if (i == items.lastIndex) big else small
            Row(
                Modifier
                    .appear(base + i)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
                    .background(CardBg)
                    .softClick { onOpen(item.key) }
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Text(
                        item.subtitle,
                        color = TextDim,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangeList(lines: List<String>) {
    lines.forEach { Text("•  " + it, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp)) }
}

@Composable
private fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White)
            Text(sub, color = TextDim, fontSize = 13.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsScreen(s: AppState) {
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) s.exportTo(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) s.importFrom(uri) }
    var page by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = page != null) { page = null }
    val titles = mapOf(
        "account" to "Account",
        "stats" to "Listening Stats",
        "appearance" to "Appearance",
        "playback" to "Playback",
        "performance" to "Performance",
        "cache" to "Storage and cache",
        "backup" to "Backup & restore",
        "behavior" to "Behavior",
        "content" to "Content",
        "about" to "About",
        "changelog" to "What's new"
    )
    Column(Modifier.verticalScroll(rememberScrollState()).padding(top = statusTop(), bottom = 220.dp)) {
      AnimatedContent(
        targetState = page,
        transitionSpec = {
            val forward = targetState != null
            if (s.reduceMotion) {
                fadeIn(tween(120)) togetherWith fadeOut(tween(80))
            } else {
                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> if (forward) w / 4 else -w / 4 } +
                    fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { w -> if (forward) -w / 4 else w / 4 } +
                        fadeOut(tween(160)))
            }
        },
        label = "settingsPage"
      ) { pg ->
       Column {
        if (pg == null) {
            Column(Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp)) {
                RoundBtn(Icons.Rounded.ArrowBack, 0.9f, { s.screen = Screen.Home })
                Spacer(Modifier.height(26.dp))
                Text("Settings", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Medium)
            }
            SettingsGroup(
                listOf(
                    SettingsItem("account", "Account", "Sign in and manage account sync", Icons.Rounded.AccountCircle),
                    SettingsItem("stats", "Listening Stats", "Your songs played and listening time", Icons.Rounded.BarChart)
                ),
                s.accent,
                0
            ) { page = it }
            SettingsGroup(
                listOf(
                    SettingsItem("appearance", "Appearance", "Themes, layout, and player styles", Icons.Rounded.Palette),
                    SettingsItem("playback", "Playback", "Audio behavior, crossfade, and sleep", Icons.Rounded.MusicNote),
                    SettingsItem("performance", "Performance", "Blur, motion, and battery saver", Icons.Rounded.Speed),
                    SettingsItem("cache", "Storage and cache", "Cached songs and size limit", Icons.Rounded.Download)
                ),
                s.accent,
                2
            ) { page = it }
            SettingsGroup(
                listOf(
                    SettingsItem("behavior", "Behavior", "Haptics, taps, and look extras", Icons.Rounded.TouchApp),
                    SettingsItem("content", "Content", "Choose what shows on Home", Icons.Rounded.Home),
                    SettingsItem("backup", "Backup & restore", "Save or bring back your library", Icons.Rounded.Folder),
                    SettingsItem("changelog", "What's new", "Changes in each version", Icons.Rounded.History),
                    SettingsItem("about", "About", "Version and build info", Icons.Rounded.Info)
                ),
                s.accent,
                5
            ) { page = it }
        } else {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundBtn(Icons.Rounded.ArrowBack, 0.9f, { page = null })
                Spacer(Modifier.width(14.dp))
                Text(titles[pg] ?: "Settings", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            when (pg) {
                "account" -> {
        Section("YouTube Music account") {
            if (s.signedIn) {
                Text("Signed in", color = Color.White)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton(if (s.importing) "Importing..." else "Import playlists", s.accent, !s.importing) {
                        s.importAccount()
                    }
                    ActionButton("Sign out", s.accent) { s.signOut() }
                }
            } else {
                Text("Sign in to bring over your playlists and liked songs.", color = TextDim)
                Spacer(Modifier.height(10.dp))
                ActionButton("Sign in", s.accent) { s.showLogin = true }
            }
            if (s.accountMsg.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(s.accountMsg, color = TextDim, fontSize = 13.sp)
            }
        }

                }
                "stats" -> {
        Section("Stats") {
            Text("Songs played: " + s.playCount, color = Color.White)
            Text("Listening time: " + (s.listenSeconds / 60) + " min", color = Color.White)
            Text("Liked songs: " + s.liked.size, color = Color.White)
            Text("Playlists: " + s.playlists.size, color = Color.White)
        }

                }
                "appearance" -> {
        Section("Look") {
            Text("Bar tint " + (s.glass * 100).toInt() + "%", color = TextDim)
            Slider(
                value = s.glass,
                onValueChange = { s.glass = it },
                valueRange = 0.3f..1f,
                onValueChangeFinished = { s.saveSettings() }
            )
            Text("Blur strength " + s.blurRadius.toInt() + " (Android 12+)", color = TextDim)
            Slider(
                value = s.blurRadius,
                onValueChange = { s.blurRadius = it },
                valueRange = 10f..200f,
                onValueChangeFinished = { s.saveSettings() }
            )
            Text("Accent", color = TextDim)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(Gold, Color(0xFF4DB6AC), Color(0xFFE57A8A), Color(0xFF8AB4F8), Color(0xFFB39DDB), Color(0xFFFFA45C)).forEach { c ->
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c)
                            .clickable {
                                s.accent = c
                                s.saveSettings()
                            }
                    )
                }
            }
        }
        Section("Player style") {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Chip("Album", s.playerStyle == 0, s.accent) {
                    s.playerStyle = 0
                    s.saveSettings()
                }
                Chip("Immersive", s.playerStyle == 1, s.accent) {
                    s.playerStyle = 1
                    s.saveSettings()
                }
                listOf("Vinyl", "Minimal").forEach { name ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CardBg2)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) { Text(name, color = TextDim, fontSize = 15.sp) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("More player styles are coming soon.", color = TextDim, fontSize = 13.sp)
        }

                }
                "playback" -> {
        Section("Playback") {
            Text("Crossfade " + s.crossfade.toInt() + " s (coming soon)", color = TextDim)
            Slider(value = s.crossfade, onValueChange = { s.crossfade = it }, valueRange = 0f..12f)
            val sl = s.sleepLeft
            Text(
                if (sl > 0) "Sleep timer: " + fmt(sl) + " left" else "Sleep timer: off (set it in the player)",
                color = TextDim
            )
        }

                }
                "cache" -> {
                    val ctx = LocalContext.current
                    var tick by remember { mutableStateOf(0) }
                    Section("Cached songs") {
                        Text("Using " + AudioCache.usedMb(ctx, tick) + " MB of " + Opts.cacheMb + " MB", color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("Songs you play are saved here for instant and offline playback. Find them in Library, Cached.", color = TextDim, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(256, 512, 1024, 2048).forEach { mb ->
                                Chip(if (mb >= 1024) (mb / 1024).toString() + " GB" else mb.toString() + " MB", Opts.cacheMb == mb, s.accent) {
                                    Opts.cacheMb = mb
                                    Opts.save()
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("The size limit applies after you restart the app.", color = TextDim, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        ActionButton("Clear cache", s.accent) {
                            AudioCache.clear(ctx)
                            tick++
                        }
                    }
                }
                "performance" -> {
        Section("Performance") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Blur on bars", color = Color.White, modifier = Modifier.weight(1f))
                Switch(
                    checked = s.blurOn,
                    onCheckedChange = {
                        s.blurOn = it
                        s.saveSettings()
                    }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reduce motion", color = Color.White, modifier = Modifier.weight(1f))
                Switch(
                    checked = s.reduceMotion,
                    onCheckedChange = {
                        s.reduceMotion = it
                        s.saveSettings()
                    }
                )
            }
            Text("If the app ever feels slow, turn both of these on.", color = TextDim, fontSize = 13.sp)
        }
        Section("Battery") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Battery saver (no blur)", color = Color.White, modifier = Modifier.weight(1f))
                Switch(
                    checked = s.batterySaver,
                    onCheckedChange = {
                        s.batterySaver = it
                        s.saveSettings()
                    }
                )
            }
        }

                }
                "behavior" -> {
                    Section("Feel") {
                        ToggleRow("Haptic feedback", "A small tick when you tap", Opts.haptics) { Opts.haptics = it; Opts.save() }
                        ToggleRow("Bounce on tap", "Buttons squish when pressed", Opts.bounce) { Opts.bounce = it; Opts.save() }
                    }
                    Section("Playback") {
                        ToggleRow("Preload next songs", "Loads the next 2 songs while one plays (uses data)", Opts.preload) { Opts.preload = it; Opts.save() }
                        ToggleRow("Fade in and out", "Soft volume fade on play, pause and skip", Opts.fade) { Opts.fade = it; Opts.save() }
                        ToggleRow("Headphone smarts", "Pause on disconnect, resume when they reconnect", Opts.headphones) { Opts.headphones = it; Opts.save() }
                        ToggleRow("Offline mode", "Only show downloaded and cached songs", Opts.offline) { Opts.offline = it; Opts.save() }
                        Text(
                            "Fade between songs: " + (if (Opts.crossfade == 0) "off" else Opts.crossfade.toString() + " s"),
                            color = TextDim
                        )
                        Slider(
                            value = Opts.crossfade.toFloat(),
                            onValueChange = { Opts.crossfade = it.toInt() },
                            valueRange = 0f..12f,
                            steps = 11,
                            onValueChangeFinished = { Opts.save() }
                        )
                    }
                    Section("Status bar") {
                        ToggleRow("Blur under status bar", "Blurred strip on every screen", Opts.statusBlur) { Opts.statusBlur = it; Opts.save() }
                        Text("Blur strength " + (Opts.blurAmount * 100).toInt() + "%", color = TextDim)
                        Slider(
                            value = Opts.blurAmount,
                            onValueChange = { Opts.blurAmount = it },
                            valueRange = 0.2f..2f,
                            onValueChangeFinished = { Opts.save() }
                        )
                        Text("Darkness " + (Opts.blurDark * 100).toInt() + "%", color = TextDim)
                        Slider(
                            value = Opts.blurDark,
                            onValueChange = { Opts.blurDark = it },
                            valueRange = 0f..0.9f,
                            onValueChangeFinished = { Opts.save() }
                        )
                    }
                    Section("Look") {
                        ToggleRow("Living background", "Tint the app with the song's cover color", Opts.livingBg) { Opts.livingBg = it; Opts.save() }
                    }
                }
                "content" -> {
                    Section("Home sections") {
                        ToggleRow("Keep listening", "Your recent songs", Opts.showKeep) { Opts.showKeep = it; Opts.save() }
                        ToggleRow("Speed dial", "Your most played", Opts.showSpeed) { Opts.showSpeed = it; Opts.save() }
                        ToggleRow("Forgotten favorites", "Liked songs you haven't played lately", Opts.showForgotten) { Opts.showForgotten = it; Opts.save() }
                        ToggleRow("Chill and Workout", "Mix shelves at the bottom", Opts.showMixes) { Opts.showMixes = it; Opts.save() }
                    }
                }
                "changelog" -> {
                    Section("v18") {
                        ChangeList(listOf(
                            "Synced lyrics (Options, Lyrics)",
                            "In-app equalizer with presets and bass boost",
                            "Fade between songs, slider in Behavior",
                            "Real radio: endless related songs",
                            "Queue: move up/down, remove, Play next, Add to end",
                            "Real artist page, Car mode, recap with top songs and artists"
                        ))
                    }
                    Section("v17") {
                        ChangeList(listOf(
                            "Fix: song ending no longer skips several songs",
                            "Keep listening at the top of Home: 3 swipeable cards",
                            "Preload the next 2 songs while one plays",
                            "Offline mode, fade in and out, headphone smarts",
                            "Solid Tempo and Pitch dialog, new mini player glow"
                        ))
                    }
                    Section("v16") {
                        ChangeList(listOf(
                            "Search filters: All, Songs, Videos, Albums, Artists, Playlists",
                            "Cached songs: replay fast and offline, list in Library, size in Settings",
                            "Status bar blur strength and darkness controls",
                            "Fix: song ending no longer gets stuck, retries then skips",
                            "Loading ring on the play button",
                            "Animated player icons, mini player slides up with a glow",
                            "Swipe song rows and the mini player, shimmer loading"
                        ))
                    }
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
                "about" -> {
                    Section("PrismWave") {
                        Text("Version v18", color = Color.White)
                        Text("Private build, made from scratch", color = TextDim, fontSize = 13.sp)
                    }
                }
                "backup" -> {
        Section("Backup") {
            Text(
                "Saves liked songs, playlists, history and stats to a file you choose. Your login is never included.",
                color = TextDim,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("Back up", s.accent) { exportLauncher.launch("PrismWave-backup.json") }
                ActionButton("Restore", s.accent) {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
            }
            if (s.backupMsg.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(s.backupMsg, color = TextDim, fontSize = 13.sp)
            }
        }

                }
            }
        }
       }
      }
    }
}
