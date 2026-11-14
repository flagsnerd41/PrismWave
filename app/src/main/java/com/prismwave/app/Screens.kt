package com.prismwave.app

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.material.icons.rounded.Close

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.MoreHoriz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.KeyboardArrowRight

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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer

// ------------------------------------------------------------ shared bits

@Composable
fun Header(s: AppState) {
    var stats by remember { mutableStateOf(false) }
    var titleFs by remember { mutableStateOf(26f) }
    val hdrA by animateFloatAsState(if (HeaderPos.hide) 0f else 1f, tween(250), label = "hdr")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .onGloballyPositioned { HeaderPos.logo = it.boundsInRoot() }
                .graphicsLayer { alpha = hdrA }
        ) { AppLogo(42.dp) }
        Spacer(Modifier.width(12.dp))
        Text(
            "PrismWave",
            color = Color.White,
            fontFamily = RetroBlendy,
            fontSize = titleFs.sp,
            maxLines = 1,
            softWrap = false,
            onTextLayout = { if (it.hasVisualOverflow && titleFs > 12f) titleFs -= 1f },
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned { HeaderPos.title = it.boundsInRoot() }
                .graphicsLayer { alpha = hdrA }
        )
        RoundBtn(Icons.Rounded.History, 0.9f, {
            s.openPlaylist = null
            s.libraryOpen = "History"
            s.screen = Screen.Library
        }, 46.dp)
        Spacer(Modifier.width(8.dp))
        RoundBtn(Icons.Rounded.BarChart, 0.9f, { stats = true }, 46.dp)
        Spacer(Modifier.width(8.dp))
        WhatsNewButton(s)
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
    val cref = remember { CoverRef() }
    Column(modifier.softClick { openFrom(s, song, from, cref, 24f) }.width(140.dp)) {
        Box {
            Cover(song, Modifier.size(140.dp).trackCover(cref), 24.dp, 44.sp)
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
            val cref = remember { CoverRef() }
            Row(
                Modifier
                    .width(itemW)
                    .height(66.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (cur) s.accent.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { openFrom(s, sg, songs, cref, 12f) }
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Cover(sg, Modifier.size(54.dp).trackCover(cref), 12.dp, 18.sp)
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
private fun HomeScreenOld(s: AppState) {
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(s: AppState) {
    var hits by remember { mutableStateOf<List<Hit>>(emptyList()) }
    var kind by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    val searchCtx = LocalContext.current
    SearchHistory.init(searchCtx)
    var sugg by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(s.searchQuery) {
        val sq = s.searchQuery.trim()
        if (sq.length < 2 || Opts.offline) {
            sugg = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        sugg = Suggest.fetch(sq)
        delay(1300)
        SearchHistory.add(sq)
    }
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
            if (SearchHistory.items.isNotEmpty() && s.searchQuery.isBlank()) {
                item { SectionTitle("Recent searches", "Clear") { SearchHistory.clear() } }
                items(SearchHistory.items.take(6)) { rq ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { s.searchQuery = rq }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = TextDim, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(rq, color = Color.White, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Rounded.Close, contentDescription = "Remove", tint = TextDim,
                            modifier = Modifier.size(22.dp).clickable { SearchHistory.remove(rq) }
                        )
                    }
                }
            }
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
            if (sugg.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sugg.take(8)) { sq ->
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                                    .clickable { s.searchQuery = sq }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = TextDim, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(sq, color = Color.White, fontSize = 14.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
            stickyHeader {
                Box(Modifier.fillMaxWidth().background(Night.copy(alpha = 0.94f))) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(listOf("All", "Songs", "Videos", "Albums", "Artists", "Playlists")) { i, label ->
                            Chip(label, kind == i, s.accent) { kind = i }
                        }
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
                PlaylistArt(p, first, 144.dp, 20.dp)
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

// ------------------------------------------------------------ Library, fresh design

@Composable
private fun LibraryTabs(s: AppState) {
    val labels = listOf("Library", "Playlists", "Artists")
    BoxWithConstraints(
        Modifier
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(CardBg)
    ) {
        val w = maxWidth / labels.size
        val x by animateDpAsState(w * s.libTab, spring(dampingRatio = 0.8f, stiffness = 420f), label = "libTabX")
        Box(
            Modifier
                .offset(x = x)
                .width(w)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(s.accent)
        )
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { i, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { s.libTab = i },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (s.libTab == i) Color.Black else Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// a big rounded tile with a soft color, a few covers stacked in the corner and the name at the bottom
@Composable
private fun BentoTile(
    title: String,
    sub: String,
    icon: ImageVector,
    tint: Color,
    covers: List<Song>,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.45f), tint.copy(alpha = 0.14f))))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .softClick(onClick)
    ) {
        Row(Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 14.dp)) {
            covers.take(3).forEachIndexed { i, sg ->
                Cover(sg, Modifier.offset(x = (-14 * i).dp).size(34.dp).border(2.dp, Color(0xFF14111A), CircleShape), 17.dp, 12.sp)
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.height(10.dp))
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PlaylistTile(p: Playlist, s: AppState, modifier: Modifier) {
    Column(modifier.softClick { s.openPlaylist = p.id }) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            PlaylistArt(p, p.songs.firstOrNull(), maxWidth, 24.dp)
        }
        Spacer(Modifier.height(8.dp))
        Text(p.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(p.songs.size.toString() + " tracks", color = TextDim, fontSize = 13.sp)
    }
}

@Composable
private fun NewPlaylistTile(s: AppState, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.softClick(onClick)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .size(maxWidth)
                    .clip(RoundedCornerShape(24.dp))
                    .background(s.accent.copy(alpha = 0.14f))
                    .border(1.5.dp, s.accent.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Add, contentDescription = null, tint = s.accent, modifier = Modifier.size(40.dp)) }
        }
        Spacer(Modifier.height(8.dp))
        Text("New playlist", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text("Start a mix", color = TextDim, fontSize = 13.sp)
    }
}

@Composable
private fun LibraryMain(s: AppState) {
    var creating by remember { mutableStateOf(false) }
    var sort by remember { mutableStateOf(0) }
    val top = s.topPlayed()
    LazyColumn(contentPadding = PaddingValues(top = statusTop(), bottom = 220.dp)) {
        item { Header(s) }
        item {
            Column(Modifier.padding(horizontal = 24.dp).padding(top = 4.dp)) {
                Text("Your library", color = Color.White, fontFamily = PrismFont, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(
                    s.liked.size.toString() + " liked  •  " + s.playlists.size + " playlists  •  " + DownloadStore.songs.size + " offline",
                    color = TextDim,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        item { LibraryTabs(s) }
        if (s.signedIn && s.accountMsg.isNotEmpty()) {
            item {
                Text(
                    s.accountMsg,
                    color = TextDim,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }
        }
        if (s.libTab == 1) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(listOf("Recent", "A to Z", "Most songs")) { i, label ->
                        Chip(label, sort == i, s.accent) { sort = i }
                    }
                }
            }
            val list = when (sort) {
                1 -> s.playlists.sortedBy { it.name.lowercase() }
                2 -> s.playlists.sortedByDescending { it.songs.size }
                else -> s.playlists.toList()
            }
            val cells: List<Playlist?> = list + listOf<Playlist?>(null)
            itemsIndexed(cells.chunked(2)) { ri, pair ->
                Row(
                    Modifier.appear(ri).fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    pair.forEach { p ->
                        if (p == null) {
                            NewPlaylistTile(s, Modifier.weight(1f)) { creating = true }
                        } else {
                            PlaylistTile(p, s, Modifier.weight(1f))
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else if (s.libTab == 2) {
            val all = s.history.toList() + s.liked.toList() + s.playlists.flatMap { it.songs }
            val counts = all.groupingBy { it.artist }.eachCount()
            val artists = all.distinctBy { it.artist }.sortedByDescending { counts[it.artist] ?: 0 }.take(60)
            if (artists.isEmpty()) {
                item { Text("Play some music and your artists show up here", color = TextDim, modifier = Modifier.padding(24.dp)) }
            }
            itemsIndexed(artists.chunked(3)) { ri, row ->
                Row(
                    Modifier.appear(ri).fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    row.forEach { a ->
                        Column(
                            Modifier
                                .weight(1f)
                                .softClick {
                                    s.searchQuery = a.artist
                                    s.screen = Screen.Search
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                Cover(a, Modifier.size(maxWidth), maxWidth / 2, 30.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(a.artist, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text((counts[a.artist] ?: 0).toString() + " songs", color = TextDim, fontSize = 12.sp)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        } else {
            val pink = Color(0xFFFF6B8A)
            val gold = s.accent
            val teal = Color(0xFF4DB6AC)
            val blue = Color(0xFF7C9CFF)
            val violet = Color(0xFFB07CFF)
            item {
                Row(
                    Modifier.appear(0).fillMaxWidth().padding(horizontal = 20.dp).height(252.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoTile(
                        "Liked songs", s.liked.size.toString() + " tracks", Icons.Rounded.Favorite, pink,
                        s.liked.toList(), Modifier.weight(1f).fillMaxHeight()
                    ) { s.libraryOpen = "Liked songs" }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BentoTile(
                            "Offline", DownloadStore.songs.size.toString() + " downloaded", Icons.Rounded.Download, gold,
                            DownloadStore.songs.toList(), Modifier.weight(1f).fillMaxWidth()
                        ) { s.libraryOpen = "Offline" }
                        BentoTile(
                            "Cached", "Instant playback", Icons.Rounded.Sync, teal,
                            emptyList(), Modifier.weight(1f).fillMaxWidth()
                        ) { s.libraryOpen = "Cached" }
                    }
                }
            }
            item {
                Row(
                    Modifier.appear(1).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoTile(
                        "Local files", "On device", Icons.Rounded.Folder, blue,
                        emptyList(), Modifier.weight(1f).fillMaxHeight()
                    ) { s.libraryOpen = "Local Files" }
                    BentoTile(
                        "My top 50", "All time", Icons.Rounded.TrendingUp, violet,
                        top, Modifier.weight(1f).fillMaxHeight()
                    ) { s.libraryOpen = "My top 50" }
                }
            }
            if (s.history.isNotEmpty()) {
                item {
                    HomeSection("Recently played") {
                        s.openPlaylist = null
                        s.libraryOpen = "History"
                    }
                }
                item { HomeShelf(s.history.toList(), s) }
            }
            if (s.playlists.isNotEmpty()) {
                item { HomeSection("Your playlists") { s.libTab = 1 } }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) { items(s.playlists.toList().take(10)) { PlaylistCard(it, s) } }
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
                        Cover(song, Modifier.size(50.dp), 10.dp, 18.sp)
                        Spacer(Modifier.width(14.dp))
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
                    Box(Modifier.padding(start = 84.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
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
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
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
private fun SettingsScreenOld(s: AppState) {
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
                Text(titles[pg] ?: "Settings", color = Color.White, fontFamily = PrismFont, fontSize = 26.sp, fontWeight = FontWeight.Bold)
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
                        Text("Using " + SongCache.usedMb(ctx, tick) + " MB of " + Opts.cacheMb + " MB", color = Color.White)
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
                        Text("Oldest songs are removed first when the limit is reached.", color = TextDim, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        ActionButton("Clear cache", s.accent) {
                            SongCache.clear(ctx)
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
                    Section("Style") {
                        Text("Theme", color = TextDim)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Warm", "Dark", "AMOLED", "Midnight").forEachIndexed { i, n ->
                                Chip(n, Opts.theme == i, s.accent) {
                                    Opts.theme = i
                                    Opts.save()
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        ToggleRow("Lite mode", "Turns off glow, blur and heavy effects for low-end phones", Opts.lite) { Opts.lite = it; Opts.save() }
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
                    Section("v22.1") {
                        ChangeList(listOf(
                            "Home header shows your YouTube Music profile picture when you are signed in",
                            "Lighter glass blur on the bars",
                            "Splash: made by flagsnerd appears letter by letter, blurred to clear",
                            "New app icon"
                        ))
                    }
                    Section("v22") {
                        ChangeList(listOf(
                            "New Home design: greeting header, Featured card, Recently Played and Speed Dial shelves",
                            "Lighter glass on the bars: the blur runs on a quarter-size copy, same look, much less work",
                            "Softer highlight on the glass edge"
                        ))
                    }
                    Section("v21") {
                        ChangeList(listOf(
                            "Cache rebuilt: songs are saved as real files and play from there",
                            "Lyrics button on the player and a full-screen karaoke view"
                        ))
                    }
                    Section("v20") {
                        ChangeList(listOf(
                            "Fix: audio cutting a few seconds into the next song when the app is in the background",
                            "The player now moves to the next song by itself, so playback never stops in between"
                        ))
                    }
                    Section("v19") {
                        ChangeList(listOf(
                            "Themes: Warm, Dark, AMOLED, Midnight",
                            "Floating bottom bar with a glowing selected tab",
                            "Title bar that fades in as you scroll",
                            "Welcome animation: made by flagsnerd, word by word",
                            "Gradient cards and bolder titles",
                            "Lite mode for low-end phones"
                        ))
                    }
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
                        Text("Version v22.1", color = Color.White)
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


// ------------------------------------------------------------ home v22 (new design)

private fun hourNow(): Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

private fun greetingText(): String {
    val h = hourNow()
    return when {
        h < 5 -> "Good Night"
        h < 12 -> "Good Morning"
        h < 18 -> "Good Afternoon"
        else -> "Good Evening"
    }
}

@Composable
private fun HomeHeader2(s: AppState) {
    val name = ProfilePic.name
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            if (s.signedIn && !name.isNullOrBlank()) {
                Text(skyGreeting(), color = Color.White.copy(alpha = 0.75f), fontSize = 18.sp)
                Text(
                    name,
                    color = Color.White,
                    fontFamily = PrismFont,
                    fontSize = 36.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                Text(skyGreeting(), color = Color.White, fontFamily = PrismFont, fontSize = 36.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        }
        ProfileAvatar(s)
    }
}

@Composable
private fun FeaturedHero(song: Song, from: List<Song>, s: AppState) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(190.dp)
            .softClick { s.play(song, from) }
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
    ) {
        Cover(song, Modifier.fillMaxSize(), 0.dp, 90.sp, quality = 800)
        if (!Opts.lite && !s.batterySaver && android.os.Build.VERSION.SDK_INT >= 31) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.horizontalGradient(0f to Color.Black, 0.55f to Color.Black, 0.85f to Color.Transparent),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                Cover(song, Modifier.fillMaxSize().blur(30.dp), 0.dp, 90.sp, quality = 400)
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)))
            }
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.72f),
                        0.55f to Color.Black.copy(alpha = 0.25f),
                        1f to Color.Transparent
                    )
                )
            )
        }
        Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(0.62f).padding(start = 22.dp)) {
            Text("Featured", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                song.title,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(song.artist, color = Color.White.copy(alpha = 0.75f), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.22f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Text("Play", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun HomeSection(title: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).clickable(enabled = onClick != null) { onClick?.invoke() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.White, fontFamily = PrismFont, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            if (onClick != null) {
                Icon(
                    Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeCard(song: Song, from: List<Song>, s: AppState, modifier: Modifier = Modifier) {
    val cur = s.current?.id == song.id
    val cref = remember { CoverRef() }
    Column(modifier.softClick { openFrom(s, song, from, cref, 20f) }.width(150.dp)) {
        Cover(song, Modifier.size(150.dp).trackCover(cref), 20.dp, 44.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            song.title,
            color = if (cur) s.accent else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(song.artist, color = TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HomeShelf(songs: List<Song>, s: AppState) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) { itemsIndexed(songs.take(15)) { i, sg -> HomeCard(sg, songs, s, Modifier.appear(i)) } }
}

@Composable
private fun HomeScreenV22(s: AppState) {
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
    LaunchedEffect(s.signedIn, Opts.offline, history.size > 0) {
        if (!Opts.offline) {
            s.loadYtHome()
            if (!s.signedIn) s.loadPicks()
        }
    }
    val ytQuick = s.ytHome.firstOrNull { it.title.contains("Quick picks", true) }?.songs
    val picks = if (s.signedIn) null else s.feeds["picks"]
    val bestQuick = ytQuick ?: picks ?: quick
    val hero = bestQuick?.firstOrNull() ?: speedDial.firstOrNull() ?: history.firstOrNull()
    Box(Modifier.fillMaxSize()) {
        HomeTopGlow(s, Modifier.align(Alignment.TopCenter))
        LazyColumn(
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = 220.dp
            )
        ) {
            item { HomeHeader2(s) }
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
            if (hero != null) {
                item { Spacer(Modifier.height(16.dp)) }
                item { FeaturedHero(hero, bestQuick ?: listOf(hero), s) }
            }
            if (history.isNotEmpty() && Opts.showKeep) {
                item {
                    HomeSection("Recently Played") {
                        s.openPlaylist = null
                        s.libraryOpen = "History"
                        s.screen = Screen.Library
                    }
                }
                item { HomeShelf(history, s) }
            }
            if (speedDial.size >= 3 && Opts.showSpeed) {
                item {
                    HomeSection("Speed Dial") {
                        s.openPlaylist = null
                        s.libraryOpen = "My top 50"
                        s.screen = Screen.Library
                    }
                }
                item { HomeShelf(speedDial, s) }
            }
            item { HomeSection(if (ytQuick == null && picks != null) "Picked for you" else "Quick picks") }
            item {
                if (bestQuick == null) LoadingRow(s) else QuickPicksGrid(bestQuick, s)
            }
            // the other shelves of your YouTube Music Home (Listen again, and so on)
            s.ytHome.filter { !it.title.contains("Quick picks", true) }.take(6).forEach { sh ->
                item { HomeSection(sh.title) }
                item { HomeShelf(sh.songs, s) }
            }
            if (ytPlaylists.isNotEmpty()) {
                item { HomeSection("Your playlists") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) { items(ytPlaylists.take(12)) { p -> PlaylistCard(p, s) } }
                }
            }
            if (forgotten.size >= 3 && Opts.showForgotten) {
                item { HomeSection("Forgotten favorites") }
                item { HomeShelf(forgotten, s) }
            }
            if (Opts.showMixes) {
                item { SongRowList("Chill", chill, s) }
                item { SongRowList("Workout", workout, s) }
            }
        }
    }
}


// ------------------------------------------------------------ home (ArchiveTune style layout)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun QuickPickCarousel(songs: List<Song>, s: AppState) {
    val list = songs.take(12)
    if (list.isEmpty()) return
    val cfg = LocalConfiguration.current
    val cardW = (cfg.screenWidthDp * 0.74f).dp
    val cardH = cardW * 1.1f
    val side = (cfg.screenWidthDp.dp - cardW) / 2
    val state = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(state)
    LazyRow(
        state = state,
        flingBehavior = fling,
        contentPadding = PaddingValues(horizontal = side),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(list) { i, sg -> QuickPickCard(sg, list, s, i, state, cardW, cardH) }
    }
}

@Composable
private fun QuickPickCard(
    song: Song,
    from: List<Song>,
    s: AppState,
    index: Int,
    state: LazyListState,
    w: androidx.compose.ui.unit.Dp,
    h: androidx.compose.ui.unit.Dp
) {
    val shape = RoundedCornerShape(32.dp)
    val cref = remember { CoverRef() }
    Box(
        Modifier
            .width(w)
            .height(h)
            .graphicsLayer {
                val info = state.layoutInfo
                val item = info.visibleItemsInfo.firstOrNull { x -> x.index == index }
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                val dist = if (item != null) {
                    kotlin.math.abs(item.offset + item.size / 2f - center) / (item.size + 12.dp.toPx())
                } else {
                    1f
                }
                val d = dist.coerceIn(0f, 1f)
                scaleX = 1f - 0.10f * d
                scaleY = 1f - 0.10f * d
                alpha = 1f - 0.35f * d
            }
            .softClick { openFrom(s, song, from, cref, 32f) }
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
    ) {
        Cover(song, Modifier.fillMaxSize().trackCover(cref), 0.dp, 90.sp, quality = 800)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.45f to Color.Transparent,
                    0.75f to Color.Black.copy(alpha = 0.40f),
                    1f to Color.Black.copy(alpha = 0.85f)
                )
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Text(
                song.title,
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(song.artist, color = Color.White.copy(alpha = 0.7f), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CommunityHeader(s: AppState, songs: List<Song>) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "From the community",
            color = Color.White,
            fontFamily = PrismFont,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .bounceClick {
                    if (songs.isNotEmpty()) {
                        s.shuffle = true
                        s.play(songs.random(), songs)
                    }
                }
                .size(56.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(s.accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MoreHoriz, contentDescription = "Shuffle", tint = Color.Black, modifier = Modifier.size(26.dp))
        }
    }
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
    val moods = listOf("Energize", "Feel good", "Workout", "Relax", "Party", "Commute", "Focus", "Sleep")
    val history = s.history.toList()
    val recentIds = history.map { it.id }.toSet()
    val forgotten = s.liked.filter { it.id !in recentIds }
    val speedDial = s.topPlayed()
    val ytPlaylists = s.playlists.filter { it.id.startsWith("yt_") }
    LaunchedEffect(s.signedIn, Opts.offline, history.size > 0) {
        if (!Opts.offline) {
            s.loadYtHome()
            if (!s.signedIn) s.loadPicks()
        }
    }
    val ytQuick = s.ytHome.firstOrNull { it.title.contains("Quick picks", true) }?.songs
    val picks = if (s.signedIn) null else s.feeds["picks"]
    val bestQuick = ytQuick ?: picks ?: quick
    val community = (chill + workout).distinctBy { it.id }.take(20)
    Box(Modifier.fillMaxSize()) {
        HomeTopGlow(s, Modifier.align(Alignment.TopCenter))
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
                    items(moods) { c ->
                        Chip(c, false, s.accent) {
                            s.searchQuery = c + " music"
                            s.screen = Screen.Search
                        }
                    }
                    item {
                        Chip("Offline", Opts.offline, s.accent) {
                            Opts.offline = !Opts.offline
                            Opts.save()
                        }
                    }
                }
            }
            item { HomeSection(if (ytQuick == null && picks != null) "Picked for you" else "Quick picks") }
            item {
                if (bestQuick == null) LoadingRow(s) else QuickPickCarousel(bestQuick, s)
            }
            if (Opts.showMixes && community.isNotEmpty()) {
                item { CommunityHeader(s, community) }
                item { QuickPicksGrid(community, s) }
            }
            if (history.isNotEmpty() && Opts.showKeep) {
                item {
                    HomeSection("Recently Played") {
                        s.openPlaylist = null
                        s.libraryOpen = "History"
                        s.screen = Screen.Library
                    }
                }
                item { HomeShelf(history, s) }
            }
            if (speedDial.size >= 3 && Opts.showSpeed) {
                item {
                    HomeSection("Speed Dial") {
                        s.openPlaylist = null
                        s.libraryOpen = "My top 50"
                        s.screen = Screen.Library
                    }
                }
                item { HomeShelf(speedDial, s) }
            }
            // the other shelves of your YouTube Music Home (Listen again, and so on)
            s.ytHome.filter { !it.title.contains("Quick picks", true) }.take(6).forEach { sh ->
                item { HomeSection(sh.title) }
                item { HomeShelf(sh.songs, s) }
            }
            if (ytPlaylists.isNotEmpty()) {
                item { HomeSection("Your playlists") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) { items(ytPlaylists.take(12)) { p -> PlaylistCard(p, s) } }
                }
            }
            if (forgotten.size >= 3 && Opts.showForgotten) {
                item { HomeSection("Forgotten favorites") }
                item { HomeShelf(forgotten, s) }
            }
        }
    }
}

// ------------------------------------------------------------ home header picture

object ProfilePic {
    var url by mutableStateOf<String?>(null)
    var name by mutableStateOf<String?>(null)
}

@Composable
private fun ProfileAvatar(s: AppState) {
    val ctx = LocalContext.current
    LaunchedEffect(s.cookie) {
        if (s.signedIn) {
            if (ProfilePic.url == null) ProfilePic.url = YtAccount.profilePhoto(s.cookie)
        } else {
            ProfilePic.url = null
            ProfilePic.name = null
        }
    }
    val url = ProfilePic.url
    if (s.signedIn && url != null) {
        val req = remember(url) { ImageRequest.Builder(ctx).data(artUrl(url, 200)).crossfade(300).build() }
        AsyncImage(
            model = req,
            contentDescription = "Settings",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .bounceClick { s.screen = Screen.Settings }
                .size(56.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
        )
    } else {
        Box(
            Modifier
                .bounceClick { s.screen = Screen.Settings }
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AccountCircle, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(34.dp))
        }
    }
}

// where the home header logo and title are, so the welcome screen can fly into them
object HeaderPos {
    var logo by mutableStateOf<androidx.compose.ui.geometry.Rect?>(null)
    var title by mutableStateOf<androidx.compose.ui.geometry.Rect?>(null)
    var hide by mutableStateOf(false)
}
