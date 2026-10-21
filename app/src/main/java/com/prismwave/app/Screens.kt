package com.prismwave.app

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun Header(s: AppState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PLogo(34.dp, s.accent)
        Spacer(Modifier.width(10.dp))
        Text(
            "PrismWave",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        RoundBtn(Icons.Rounded.Search, s.glassAlpha(), { s.screen = Screen.Search })
        Spacer(Modifier.width(8.dp))
        RoundBtn(Icons.Rounded.Settings, s.glassAlpha(), { s.screen = Screen.Settings })
    }
}

@Composable
fun ChipsRow(s: AppState) {
    val chips = listOf("Relax", "Energize", "Workout", "Focus", "Commute", "Party", "Sleep")
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(chips) { _, c ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Surface2.copy(alpha = 0.7f))
                    .clickable {
                        s.searchQuery = c + " music"
                        s.screen = Screen.Search
                    }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(c, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun QuickCard(song: Song, s: AppState, from: List<Song>) {
    Box(
        Modifier
            .width(280.dp)
            .height(190.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable { s.play(song, from) }
    ) {
        Cover(song, Modifier.fillMaxSize(), 0.dp, 72.sp)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text(song.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(song.artist, color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp, maxLines = 1)
        }
    }
}

@Composable
fun SmallCard(song: Song, s: AppState, from: List<Song>) {
    Column(Modifier.width(140.dp).clickable { s.play(song, from) }) {
        Cover(song, Modifier.size(140.dp), 18.dp, 44.sp)
        Spacer(Modifier.height(8.dp))
        Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(song.artist, color = TextDim, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
fun LoadingRow(s: AppState) {
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = s.accent, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun HomeScreen(s: AppState) {
    LaunchedEffect(Unit) {
        s.loadFeed("quick", "new music hits")
        s.loadFeed("chill", "chill songs")
        s.loadFeed("workout", "workout songs")
    }
    val quick = s.feeds["quick"]
    val chill = s.feeds["chill"]
    val workout = s.feeds["workout"]
    LazyColumn(contentPadding = PaddingValues(bottom = 180.dp)) {
        item { Header(s) }
        item { ChipsRow(s) }
        item { SectionTitle("Quick picks") }
        item {
            if (quick == null) LoadingRow(s) else LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) { items(quick.take(8)) { QuickCard(it, s, quick) } }
        }
        if (s.history.isNotEmpty()) {
            item { SectionTitle("Keep listening") }
            item {
                val h = s.history.toList()
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) { items(h.take(12)) { SmallCard(it, s, h) } }
            }
        }
        if (chill != null && chill.isNotEmpty()) {
            item { SectionTitle("Chill") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) { items(chill.take(10)) { SmallCard(it, s, chill) } }
            }
        }
        if (workout != null && workout.isNotEmpty()) {
            item { SectionTitle("Workout") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) { items(workout.take(10)) { SmallCard(it, s, workout) } }
            }
        }
        if (s.liked.isNotEmpty()) {
            item { SectionTitle("Your favorites") }
            val l = s.liked.toList()
            items(l.take(4)) { SongRow(it, s, l) }
        }
    }
}

@Composable
fun SearchScreen(s: AppState) {
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
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
    LazyColumn(contentPadding = PaddingValues(bottom = 180.dp)) {
        item {
            Text(
                "Search",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 12.dp)
            )
        }
        item {
            OutlinedTextField(
                value = s.searchQuery,
                onValueChange = { s.searchQuery = it },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                placeholder = { Text("Songs, artists, albums", color = TextDim) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = TextDim) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = s.accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    cursorColor = s.accent
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )
        }
        if (busy) item { LoadingRow(s) }
        if (err.isNotEmpty()) {
            item { Text(err, color = TextDim, modifier = Modifier.padding(20.dp)) }
        }
        if (s.searchQuery.trim().length < 2) {
            if (s.history.isNotEmpty()) {
                item { SectionTitle("Recently played") }
                val h = s.history.toList()
                items(h.take(10)) { SongRow(it, s, h) }
            }
        } else if (!busy) {
            item { SectionTitle("Results") }
            if (results.isEmpty() && err.isEmpty()) {
                item { Text("Nothing found", color = TextDim, modifier = Modifier.padding(horizontal = 20.dp)) }
            }
            items(results) { SongRow(it, s, results) }
        }
    }
}

private data class TileInfo(val name: String, val icon: ImageVector, val count: String)

@Composable
fun LibraryScreen(s: AppState) {
    var open by remember { mutableStateOf<String?>(null) }
    val top = s.topPlayed()
    val tiles = listOf(
        TileInfo("Liked", Icons.Rounded.Favorite, s.liked.size.toString() + " songs"),
        TileInfo("History", Icons.Rounded.Home, s.history.size.toString() + " songs"),
        TileInfo("Top 50", Icons.Rounded.Star, top.size.toString() + " songs"),
        TileInfo("Offline", Icons.Rounded.Download, "Soon"),
        TileInfo("Cached", Icons.Rounded.Storage, "Soon"),
        TileInfo("Local Files", Icons.Rounded.Folder, "Soon")
    )
    val o = open
    if (o != null) {
        val songs = when (o) {
            "Liked" -> s.liked.toList()
            "History" -> s.history.toList()
            "Top 50" -> top
            else -> emptyList()
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 180.dp)) {
            item {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "< Back",
                        color = s.accent,
                        modifier = Modifier.clickable { open = null }.padding(vertical = 8.dp)
                    )
                    Text(o, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (songs.isEmpty()) {
                item {
                    Text(
                        if (o == "Liked") "No liked songs yet, tap the heart on a song" else "Coming in a later wave",
                        color = TextDim,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
            items(songs) { SongRow(it, s, songs) }
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 180.dp)) {
        item {
            Text(
                "Library",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 14.dp)
            )
        }
        item {
            val first = top.firstOrNull()
            Box(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.horizontalGradient(listOf(s.accent.copy(alpha = 0.9f), Surface2)))
                    .clickable { if (first != null) s.play(first, top) }
                    .padding(22.dp)
            ) {
                Column(Modifier.align(Alignment.CenterStart)) {
                    Text("Most played", color = Color.Black.copy(alpha = 0.7f), fontSize = 14.sp)
                    Text(
                        first?.title ?: "Play something first",
                        color = Color.Black,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(first?.artist ?: "", color = Color.Black.copy(alpha = 0.7f), fontSize = 15.sp, maxLines = 1)
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
        items(tiles.chunked(2)) { pair ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { t ->
                    Column(
                        Modifier
                            .weight(1f)
                            .height(112.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Surface1)
                            .clickable { open = t.name }
                            .padding(16.dp)
                    ) {
                        Icon(t.icon, contentDescription = null, tint = s.accent)
                        Spacer(Modifier.weight(1f))
                        Text(t.name, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(t.count, color = TextDim, fontSize = 13.sp)
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

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
    Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 180.dp)) {
        Text(
            "Settings",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 10.dp)
        )
        Section("Look") {
            Text("Glass opacity " + (s.glass * 100).toInt() + "%", color = TextDim)
            Slider(value = s.glass, onValueChange = { s.glass = it }, valueRange = 0.4f..1f)
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
                Text("Battery saver (solid surfaces)", color = Color.White, modifier = Modifier.weight(1f))
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
        }
    }
}
