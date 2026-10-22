package com.prismwave.app

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
        PLogo(36.dp, s.accent)
        Spacer(Modifier.width(10.dp))
        Text(
            "PrismWave",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
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
        AlertDialog(
            onDismissRequest = { stats = false },
            confirmButton = { TextButton(onClick = { stats = false }) { Text("Close") } },
            title = { Text("Your stats") },
            text = {
                Column {
                    Text("Songs played: " + s.playCount)
                    Text("Listening time: " + (s.listenSeconds / 60) + " min")
                    Text("Liked songs: " + s.liked.size)
                    Text("Playlists: " + s.playlists.size)
                }
            }
        )
    }
}

@Composable
fun LoadingRow(s: AppState) {
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = s.accent, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun QuickCard(song: Song, s: AppState, from: List<Song>) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .width(300.dp)
            .height(340.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
            .clickable { s.play(song, from) }
    ) {
        Cover(song, Modifier.fillMaxSize(), 0.dp, 90.sp)
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
fun RecentCard(song: Song, s: AppState, from: List<Song>) {
    Column(Modifier.width(140.dp).clickable { s.play(song, from) }) {
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
    ) { items(songs.take(12)) { RecentCard(it, s, songs) } }
}

// ------------------------------------------------------------ home

@Composable
fun HomeScreen(s: AppState) {
    LaunchedEffect(Unit) {
        s.loadFeed("quick", "new music hits")
        s.loadFeed("chill", "chill songs")
        s.loadFeed("workout", "workout songs")
    }
    val quick = s.feeds["quick"]
    val chill = s.feeds["chill"] ?: emptyList()
    val workout = s.feeds["workout"] ?: emptyList()
    val chips = listOf("Relax", "Workout", "Energize", "Party", "Commute", "Focus", "Sleep")
    LazyColumn(contentPadding = PaddingValues(bottom = 220.dp)) {
        item { Header(s) }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chips) { c ->
                    Chip(c, false, s.accent) {
                        s.searchQuery = c + " music"
                        s.screen = Screen.Search
                    }
                }
            }
        }
        item { SectionTitle("Quick picks") }
        item {
            if (quick == null) LoadingRow(s) else LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) { items(quick.take(8)) { QuickCard(it, s, quick) } }
        }
        item { SongRowList("Recently played", s.history.toList(), s) }
        item { SongRowList("Chill", chill, s) }
        item { SongRowList("Workout", workout, s) }
        if (s.liked.isNotEmpty()) {
            item { SectionTitle("Your favorites") }
            val l = s.liked.toList()
            items(l.take(4)) { SongRow(it, s, l) }
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
            .height(96.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.horizontalGradient(listOf(m.b, m.a)))
            .clickable { s.searchQuery = m.query }
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
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    LaunchedEffect(s.searchQuery) {
        err = ""
        val q = s.searchQuery.trim()
        if (q.length < 2) {
            results = emptyList()
            busy = false
            return@LaunchedEffect
        }
        delay(500)
        busy = true
        try {
            results = Engine.search(q)
        } catch (e: Exception) {
            err = "Search failed: " + (e.message ?: "unknown").take(80)
        }
        busy = false
    }
    val searching = s.searchQuery.trim().length >= 2
    LazyColumn(contentPadding = PaddingValues(bottom = 220.dp)) {
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
                    focusedContainerColor = Surface2,
                    unfocusedContainerColor = Surface2,
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
                items(Moods.chunked(2)) { pair ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { MoodTile(it, s, Modifier.weight(1f)) }
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
            if (busy) item { LoadingRow(s) }
            if (err.isNotEmpty()) item { Text(err, color = TextDim, modifier = Modifier.padding(20.dp)) }
            if (!busy) {
                item { SectionTitle("Results") }
                if (results.isEmpty() && err.isEmpty()) {
                    item { Text("Nothing found", color = TextDim, modifier = Modifier.padding(horizontal = 20.dp)) }
                }
                items(results) { SongRow(it, s, results) }
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
            .clip(RoundedCornerShape(26.dp))
            .background(Surface1)
            .clickable(onClick = onClick)
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
            .width(168.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Surface1)
            .clickable { s.openPlaylist = p.id }
            .padding(12.dp)
    ) {
        val first = p.songs.firstOrNull()
        Box {
            if (first != null) {
                Cover(first, Modifier.size(144.dp), 20.dp, 44.sp)
            } else {
                Box(Modifier.size(144.dp).clip(RoundedCornerShape(20.dp)).background(Surface2))
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

@Composable
fun LibraryScreen(s: AppState) {
    val plId = s.openPlaylist
    if (plId != null) {
        PlaylistScreen(s, plId)
        return
    }
    val open = s.libraryOpen
    if (open != null) {
        SongListScreen(s, open)
        return
    }
    var creating by remember { mutableStateOf(false) }
    val top = s.topPlayed()
    LazyColumn(contentPadding = PaddingValues(bottom = 220.dp)) {
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
                        Box(Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).background(Surface2))
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
                        .background(Surface2)
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
            items(tiles.chunked(2)) { pair ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
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
                                .background(Surface1)
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
        else -> emptyList()
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 220.dp)) {
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
                        "History", "My top 50" -> "Play something first"
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
    var unlocked by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    val total = pl.songs.sumOf { it.seconds }
    LazyColumn(contentPadding = PaddingValues(bottom = 220.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(400.dp)) {
                val first = pl.songs.firstOrNull()
                if (first != null) {
                    Cover(first, Modifier.fillMaxSize(), 0.dp, 120.sp)
                } else {
                    Box(Modifier.fillMaxSize().background(Surface2))
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Night)
                        )
                    )
                )
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundBtn(Icons.Rounded.ArrowBack, 0.6f, { s.openPlaylist = null })
                    Spacer(Modifier.weight(1f))
                    RoundBtn(Icons.Rounded.MoreVert, 0.6f, { menu = true })
                }
                Column(
                    Modifier.align(Alignment.BottomCenter).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        pl.name,
                        color = Color.White,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Text(
                        pl.songs.size.toString() + " songs  •  " + fmtLong(total),
                        color = TextDim,
                        fontSize = 16.sp
                    )
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundBtn(Icons.Rounded.Shuffle, 0.9f, {
                    if (pl.songs.isNotEmpty()) {
                        s.shuffle = true
                        s.play(pl.songs.random(), pl.songs)
                    }
                }, 52.dp)
                Spacer(Modifier.width(14.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White)
                        .clickable { if (pl.songs.isNotEmpty()) s.play(pl.songs[0], pl.songs) }
                        .padding(horizontal = 36.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text("Play", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(s.accent.copy(alpha = 0.25f))
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) { Text("Custom order", color = s.accent, fontWeight = FontWeight.Medium) }
                Spacer(Modifier.weight(1f))
                Icon(
                    if (unlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = if (unlocked) s.accent else Color.White,
                    modifier = Modifier.size(30.dp).clickable { unlocked = !unlocked }
                )
            }
        }
        if (pl.songs.isEmpty()) {
            item {
                Text(
                    "Empty. Open any song's menu and tap Add to playlist.",
                    color = TextDim,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
        itemsIndexed(pl.songs) { i, song ->
            val cur = s.current?.id == song.id
            var rowMenu by remember { mutableStateOf(false) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (cur) s.accent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { s.play(song, pl.songs) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Cover(song, Modifier.size(52.dp), 12.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = if (cur) s.accent else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist + "  •  " + fmt(song.seconds),
                        color = TextDim,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        tint = TextDim,
                        modifier = Modifier.size(28.dp).clickable { rowMenu = true }
                    )
                }
            }
            if (rowMenu) SongMenu(song, s, pl.id) { rowMenu = false }
        }
    }
    if (menu) {
        AlertDialog(
            onDismissRequest = { menu = false },
            confirmButton = { TextButton(onClick = { menu = false }) { Text("Close") } },
            title = { Text(pl.name) },
            text = {
                Column {
                    Text(
                        "Rename",
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().clickable { menu = false; renaming = true }.padding(vertical = 12.dp)
                    )
                    Text(
                        "Delete playlist",
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                menu = false
                                s.openPlaylist = null
                                s.deletePlaylist(pl.id)
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        )
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
            .background(Surface1)
            .padding(18.dp)
    ) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
fun SettingsScreen(s: AppState) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 220.dp)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundBtn(Icons.Rounded.ArrowBack, 0.9f, { s.screen = Screen.Home })
            Spacer(Modifier.width(14.dp))
            Text("Settings", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
        Section("Look") {
            Text("Bar tint " + (s.glass * 100).toInt() + "%", color = TextDim)
            Slider(value = s.glass, onValueChange = { s.glass = it }, valueRange = 0.3f..1f)
            Text("Blur strength " + s.blurRadius.toInt() + " (Android 12+)", color = TextDim)
            Slider(value = s.blurRadius, onValueChange = { s.blurRadius = it }, valueRange = 10f..90f)
            Text("Accent", color = TextDim)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(Gold, Color(0xFF4DB6AC), Color(0xFFE57A8A), Color(0xFF8AB4F8)).forEach { c ->
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c)
                            .clickable { s.accent = c }
                    )
                }
            }
        }
        Section("Battery") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Battery saver (no blur)", color = Color.White, modifier = Modifier.weight(1f))
                Switch(checked = s.batterySaver, onCheckedChange = { s.batterySaver = it })
            }
        }
        Section("Playback") {
            Text("Crossfade " + s.crossfade.toInt() + " s (coming soon)", color = TextDim)
            Slider(value = s.crossfade, onValueChange = { s.crossfade = it }, valueRange = 0f..12f)
            val sl = s.sleepLeft
            Text(
                if (sl > 0) "Sleep timer: " + fmt(sl) + " left" else "Sleep timer: off (set it in the player)",
                color = TextDim
            )
        }
        Section("Stats") {
            Text("Songs played: " + s.playCount, color = Color.White)
            Text("Listening time: " + (s.listenSeconds / 60) + " min", color = Color.White)
            Text("Liked songs: " + s.liked.size, color = Color.White)
            Text("Playlists: " + s.playlists.size, color = Color.White)
        }
    }
}
