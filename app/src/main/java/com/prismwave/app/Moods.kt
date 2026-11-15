package com.prismwave.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// Mood & Genres from YouTube Music: colored categories with a cover, and the playlists inside each one.
// It works without signing in.
object MoodApi {
    class Cat(val name: String, val params: String, val color: Color)
    class Item(val title: String, val sub: String, val thumb: String?, val browseId: String?, val song: Song?)
    class Shelf(val title: String, val items: List<Item>)

    val cats = mutableStateListOf<Cat>()
    val covers = mutableStateMapOf<String, String>()
    var loading by mutableStateOf(false)
    var failed by mutableStateOf(false)

    private const val ORIGIN = "https://music.youtube.com"
    private val http = OkHttpClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(3)
    private val shelfCache = java.util.concurrent.ConcurrentHashMap<String, List<Shelf>>()

    private fun browse(browseId: String, params: String?): JSONObject {
        val client = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", "1.20250310.01.00")
            .put("hl", "en")
            .put("gl", "US")
        val body = JSONObject().put("context", JSONObject().put("client", client)).put("browseId", browseId)
        if (params != null) body.put("params", params)
        val req = Request.Builder()
            .url(ORIGIN + "/youtubei/v1/browse?prettyPrint=false")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .header("Origin", ORIGIN)
            .header("X-Origin", ORIGIN)
            .header("Referer", "$ORIGIN/")
            .header("User-Agent", YtAccount.UA)
            .build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw IOException("YouTube returned " + r.code)
            return JSONObject(r.body?.string() ?: "{}")
        }
    }

    private fun walk(node: Any?, visit: (String, JSONObject) -> Unit) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = node.opt(k)
                    if (v is JSONObject) visit(k, v)
                    walk(v, visit)
                }
            }
            is JSONArray -> for (i in 0 until node.length()) walk(node.opt(i), visit)
        }
    }

    private fun firstRunText(o: JSONObject?): String =
        o?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()

    private fun runsText(o: JSONObject?): String {
        val runs = o?.optJSONArray("runs") ?: return ""
        return (0 until runs.length()).joinToString("") { runs.optJSONObject(it)?.optString("text").orEmpty() }
    }

    private fun findString(node: Any?, key: String): String {
        when (node) {
            is JSONObject -> {
                val own = node.optString(key, "")
                if (own.isNotEmpty() && node.opt(key) is String) return own
                val keys = node.keys()
                while (keys.hasNext()) {
                    val r = findString(node.opt(keys.next()), key)
                    if (r.isNotEmpty()) return r
                }
            }
            is JSONArray -> for (i in 0 until node.length()) {
                val r = findString(node.opt(i), key)
                if (r.isNotEmpty()) return r
            }
        }
        return ""
    }

    private fun lastThumb(thumbs: JSONArray?): String? {
        if (thumbs == null || thumbs.length() == 0) return null
        return thumbs.optJSONObject(thumbs.length() - 1)?.optString("url")?.ifEmpty { null }
    }

    private fun seconds(text: String): Int {
        val parts = text.trim().split(":").mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty()) return 0
        return parts.fold(0) { acc, n -> acc * 60 + n }
    }

    private fun parseRow(r: JSONObject): Song? {
        var videoId = r.optJSONObject("playlistItemData")?.optString("videoId").orEmpty()
        if (videoId.isEmpty()) videoId = findString(r, "videoId")
        if (videoId.isEmpty()) return null
        val flex = r.optJSONArray("flexColumns")
        fun col(i: Int): String = runsText(
            flex?.optJSONObject(i)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
        )
        val title = col(0)
        if (title.isEmpty()) return null
        val artist = col(1).split(" \u2022 ").firstOrNull { it.isNotBlank() }?.trim() ?: "Unknown"
        val dur = r.optJSONArray("fixedColumns")?.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
            ?.optJSONObject("text")
        val thumb = lastThumb(
            r.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        )
        val url = "https://www.youtube.com/watch?v=" + videoId
        return Song(url, title, artist, seconds(firstRunText(dur)), url, thumb)
    }

    private fun parseTwoRow(r: JSONObject): Item? {
        val title = firstRunText(r.optJSONObject("title"))
        if (title.isEmpty()) return null
        val sub = runsText(r.optJSONObject("subtitle"))
        val thumb = lastThumb(
            r.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        )
        val nav = r.optJSONObject("navigationEndpoint")
        val bid = nav?.optJSONObject("browseEndpoint")?.optString("browseId").orEmpty()
        if (bid.startsWith("VL") || bid.startsWith("MPRE")) return Item(title, sub, thumb, bid, null)
        val vid = nav?.optJSONObject("watchEndpoint")?.optString("videoId").orEmpty()
        if (vid.isNotEmpty()) {
            val url = "https://www.youtube.com/watch?v=" + vid
            val artist = sub.split(" \u2022 ").firstOrNull { it.isNotBlank() && it != "Song" && it != "Video" }?.trim() ?: "Unknown"
            return Item(title, sub, thumb, null, Song(url, title, artist, 0, url, thumb))
        }
        return null
    }

    // the list of categories (Sad, Rock, Feel good, Sleep, Latin...)
    fun ensure() {
        if (cats.isNotEmpty() || loading) return
        loading = true
        failed = false
        scope.launch {
            try {
                val json = browse("FEmusic_moods_and_genres", null)
                val out = ArrayList<Cat>()
                walk(json) { key, v ->
                    if (key == "musicNavigationButtonRenderer") {
                        val name = runsText(v.optJSONObject("buttonText"))
                        val params = v.optJSONObject("clickCommand")?.optJSONObject("browseEndpoint")
                            ?.optString("params").orEmpty()
                        val argb = v.optJSONObject("solid")?.optLong("leftStripeColor", 0xFF3A3A3AL) ?: 0xFF3A3A3AL
                        if (name.isNotEmpty() && params.isNotEmpty()) {
                            out.add(Cat(name, params, Color(argb.toInt() or 0xFF000000.toInt())))
                        }
                    }
                }
                val uniq = out.distinctBy { it.params }
                cats.clear()
                cats.addAll(uniq)
                if (uniq.isEmpty()) failed = true
            } catch (e: Exception) {
                failed = true
            }
            loading = false
        }
    }

    // what is inside a category: shelves of playlists (and sometimes songs)
    fun category(c: Cat): List<Shelf> {
        shelfCache[c.params]?.let { return it }
        val json = browse("FEmusic_moods_and_genres_category", c.params)
        val shelves = ArrayList<Shelf>()
        walk(json) { key, v ->
            if (key == "musicCarouselShelfRenderer" || key == "gridRenderer") {
                val header = v.optJSONObject("header")
                var title = firstRunText(header?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")?.optJSONObject("title"))
                if (title.isEmpty()) title = firstRunText(header?.optJSONObject("gridHeaderRenderer")?.optJSONObject("title"))
                val arr = v.optJSONArray("contents") ?: v.optJSONArray("items")
                val items = ArrayList<Item>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val el = arr.optJSONObject(i) ?: continue
                        el.optJSONObject("musicTwoRowItemRenderer")?.let { parseTwoRow(it) }?.let { items.add(it) }
                        el.optJSONObject("musicResponsiveListItemRenderer")?.let { parseRow(it) }?.let { sg ->
                            items.add(Item(sg.title, sg.artist, sg.thumb, null, sg))
                        }
                    }
                }
                if (items.isNotEmpty()) shelves.add(Shelf(title.ifEmpty { c.name }, items))
            }
        }
        shelfCache[c.params] = shelves
        return shelves
    }

    fun loadCover(c: Cat) {
        if (covers.containsKey(c.params)) return
        scope.launch {
            gate.withPermit {
                if (covers.containsKey(c.params)) return@withPermit
                try {
                    val sh = category(c)
                    val t = sh.firstNotNullOfOrNull { x -> x.items.firstOrNull { it.thumb != null }?.thumb }
                    if (t != null) covers[c.params] = t
                } catch (e: Exception) {
                }
            }
        }
    }

    fun songs(browseId: String): List<Song> {
        val json = browse(browseId, null)
        val out = ArrayList<Song>()
        walk(json) { key, v ->
            if (key == "musicResponsiveListItemRenderer") parseRow(v)?.let { out.add(it) }
        }
        return out.distinctBy { it.id }
    }
}

object MoodNav {
    var cat by mutableStateOf<MoodApi.Cat?>(null)
    var playlist by mutableStateOf<MoodApi.Item?>(null)
}

@Composable
private fun MoodCard(c: MoodApi.Cat, modifier: Modifier) {
    val ctx = LocalContext.current
    LaunchedEffect(c.params) { MoodApi.loadCover(c) }
    val cover = MoodApi.covers[c.params]
    Box(
        modifier
            .height(112.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.horizontalGradient(listOf(lerp(c.color, Night, 0.45f), c.color)))
            .softClick { MoodNav.cat = c }
    ) {
        Text(
            c.name,
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 18.dp).fillMaxWidth(0.5f)
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .size(86.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.14f))
        ) {
            if (cover != null) {
                val req = remember(cover) { ImageRequest.Builder(ctx).data(artUrl(cover, 300)).crossfade(250).build() }
                AsyncImage(model = req, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

// two columns of category cards, used in the Search page
fun LazyListScope.moodGridItems(s: AppState) {
    itemsIndexed(MoodApi.cats.toList().chunked(2)) { ri, pair ->
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            pair.forEach { MoodCard(it, Modifier.weight(1f).appear(ri.coerceAtMost(6))) }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun MoodItemCard(item: MoodApi.Item, from: List<Song>, s: AppState, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    Column(
        modifier
            .softClick {
                val sg = item.song
                if (sg != null) s.play(sg, from) else MoodNav.playlist = item
            }
            .width(150.dp)
    ) {
        Box(Modifier.size(150.dp).clip(RoundedCornerShape(22.dp)).background(CardBg2)) {
            val t = item.thumb
            if (t != null) {
                val req = remember(t) { ImageRequest.Builder(ctx).data(artUrl(t, 400)).crossfade(250).build() }
                AsyncImage(model = req, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.sub, color = TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MoodCategoryPage(s: AppState, cat: MoodApi.Cat) {
    var shelves by remember(cat.params) { mutableStateOf<List<MoodApi.Shelf>?>(null) }
    var attempt by remember(cat.params) { mutableStateOf(0) }
    var err by remember(cat.params) { mutableStateOf(false) }
    LaunchedEffect(cat.params, attempt) {
        err = false
        try {
            shelves = withContext(Dispatchers.IO) { MoodApi.category(cat) }
        } catch (e: Exception) {
            err = true
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(lerp(Night, cat.color, 0.50f), Night)))
    ) {
        LazyColumn(contentPadding = PaddingValues(top = statusTop(), bottom = 220.dp)) {
            item {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundBtn(Icons.Rounded.ArrowBack, 0.9f, { MoodNav.cat = null })
                    Spacer(Modifier.width(14.dp))
                    Text(cat.name, color = Color.White, fontFamily = PrismFont, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
            }
            val list = shelves
            if (list == null && !err) item { LoadingRow(s) }
            if (err) {
                item {
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        Text("Couldn't load this category", color = TextDim)
                        Spacer(Modifier.height(10.dp))
                        Chip("Try again", false, s.accent) { attempt++ }
                    }
                }
            }
            if (list != null && list.isEmpty()) {
                item { Text("Nothing here right now", color = TextDim, modifier = Modifier.padding(horizontal = 24.dp)) }
            }
            list?.forEach { sh ->
                item {
                    Text(
                        sh.title,
                        color = Color.White,
                        fontFamily = PrismFont,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp)
                    )
                }
                item {
                    val songsOnly = sh.items.mapNotNull { it.song }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) { items(sh.items) { MoodItemCard(it, songsOnly, s) } }
                }
            }
        }
        AnimatedVisibility(
            visible = MoodNav.playlist != null,
            enter = fadeIn(tween(200)) + slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 3 },
            exit = fadeOut(tween(160)) + slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 3 }
        ) {
            val item = remember { mutableStateOf<MoodApi.Item?>(null) }
            if (MoodNav.playlist != null) item.value = MoodNav.playlist
            item.value?.let { MoodPlaylistPage(s, it) }
        }
    }
}

@Composable
private fun MoodPlaylistPage(s: AppState, item: MoodApi.Item) {
    val ctx = LocalContext.current
    var songs by remember(item.browseId) { mutableStateOf<List<Song>?>(null) }
    var err by remember(item.browseId) { mutableStateOf(false) }
    LaunchedEffect(item.browseId) {
        err = false
        try {
            songs = withContext(Dispatchers.IO) { MoodApi.songs(item.browseId ?: "") }
        } catch (e: Exception) {
            err = true
        }
    }
    Box(Modifier.fillMaxSize().background(Night)) {
        LazyColumn(contentPadding = PaddingValues(top = statusTop(), bottom = 220.dp)) {
            item {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundBtn(Icons.Rounded.ArrowBack, 0.9f, { MoodNav.playlist = null })
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(220.dp).clip(RoundedCornerShape(26.dp)).background(CardBg2)) {
                        val t = item.thumb
                        if (t != null) {
                            val req = remember(t) { ImageRequest.Builder(ctx).data(artUrl(t, 600)).crossfade(250).build() }
                            AsyncImage(model = req, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        item.title,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.sub.isNotEmpty()) Text(item.sub, color = TextDim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            val list = songs
            if (list != null && list.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .bounceClick { s.play(list[0], list) },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(6.dp))
                            Text("Play", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            Modifier
                                .bounceClick {
                                    s.shuffle = true
                                    s.play(list.random(), list)
                                }
                                .size(54.dp)
                                .clip(RoundedCornerShape(27.dp))
                                .background(Color.White.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color.White) }
                    }
                }
                items(list) { SongRow(it, s, list) }
            }
            if (list == null && !err) item { LoadingRow(s) }
            if (err) item { Text("Couldn't load this playlist", color = TextDim, modifier = Modifier.padding(24.dp)) }
        }
    }
}

// the category page, drawn over the app (the bottom bar stays on top of it)
@Composable
fun MoodsHost(s: AppState) {
    val cat = MoodNav.cat
    val last = remember { mutableStateOf<MoodApi.Cat?>(null) }
    if (cat != null) last.value = cat
    AnimatedVisibility(
        visible = cat != null,
        enter = fadeIn(tween(220)) + slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 4 },
        exit = fadeOut(tween(180)) + slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { it / 4 }
    ) {
        last.value?.let { MoodCategoryPage(s, it) }
    }
}
