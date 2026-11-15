package com.prismwave.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// Fresh recommendations. They are built on your phone from what you play and like, never repeat songs
// you already know or saw in the last two weeks, and change every day (or when you tap Refresh).
object Fresh {
    val shelves = mutableStateListOf<YtShelf>()
    var quick by mutableStateOf<List<Song>>(emptyList())
    var busy by mutableStateOf(false)

    private var prefs: SharedPreferences? = null
    private val seen = LinkedHashMap<String, Long>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val genres = listOf(
        "indie pop", "jazz", "lofi hip hop", "classic rock", "r&b", "electronic", "acoustic", "k-pop",
        "latin pop", "soul", "synthwave", "folk", "afrobeats", "house", "alternative rock", "reggae"
    )

    private fun key(s: Song): String = s.id.substringAfter("v=")

    private fun dayKey(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }

    private fun init(ctx: Context) {
        if (prefs != null) return
        val p = ctx.applicationContext.getSharedPreferences("prismwave_fresh", Context.MODE_PRIVATE)
        prefs = p
        (p.getString("seen", "") ?: "").split(",").forEach { part ->
            val kv = part.split("|")
            if (kv.size == 2) seen[kv[0]] = kv[1].toLongOrNull() ?: 0L
        }
    }

    private fun songJson(s: Song): JSONObject = JSONObject()
        .put("id", s.id).put("t", s.title).put("a", s.artist).put("d", s.seconds).put("u", s.url).put("th", s.thumb ?: "")

    private fun jsonSong(o: JSONObject): Song =
        Song(o.getString("id"), o.getString("t"), o.getString("a"), o.getInt("d"), o.getString("u"), o.optString("th").ifEmpty { null })

    private fun save(day: Int) {
        val p = prefs ?: return
        try {
            val sh = JSONArray()
            shelves.forEach { x ->
                sh.put(JSONObject().put("title", x.title).put("songs", JSONArray(x.songs.map { songJson(it) })))
            }
            p.edit()
                .putInt("day", day)
                .putString("quick", JSONArray(quick.map { songJson(it) }).toString())
                .putString("shelves", sh.toString())
                .putString("seen", seen.entries.joinToString(",") { it.key + "|" + it.value })
                .apply()
        } catch (e: Exception) {
        }
    }

    private fun loadCache(day: Int): Boolean {
        val p = prefs ?: return false
        if (p.getInt("day", 0) != day) return false
        return try {
            val q = JSONArray(p.getString("quick", "[]") ?: "[]")
            val sh = JSONArray(p.getString("shelves", "[]") ?: "[]")
            val qs = (0 until q.length()).map { jsonSong(q.getJSONObject(it)) }
            val ss = (0 until sh.length()).map { i ->
                val o = sh.getJSONObject(i)
                val arr = o.getJSONArray("songs")
                YtShelf(o.getString("title"), (0 until arr.length()).map { jsonSong(arr.getJSONObject(it)) })
            }
            if (qs.size < 6) return false
            quick = qs
            shelves.clear()
            shelves.addAll(ss)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun refresh(ctx: Context, s: AppState, force: Boolean = false) {
        init(ctx)
        if (busy) return
        val day = dayKey()
        if (!force && quick.isNotEmpty()) return
        if (!force && loadCache(day)) return
        busy = true
        scope.launch {
            try {
                val built = withContext(Dispatchers.IO) { build(s, force) }
                if (built.first.size >= 6 || built.second.isNotEmpty()) {
                    quick = built.first
                    shelves.clear()
                    shelves.addAll(built.second)
                    save(day)
                }
            } catch (e: Exception) {
            }
            busy = false
        }
    }

    private suspend fun build(s: AppState, force: Boolean): Pair<List<Song>, List<YtShelf>> {
        val rng = if (force) Random(System.nanoTime()) else Random(dayKey().toLong() * 7919L)
        val now = System.currentTimeMillis()
        val knownIds = HashSet<String>()
        s.history.forEach { knownIds.add(key(it)) }
        s.liked.forEach { knownIds.add(key(it)) }
        s.topPlayed().forEach { knownIds.add(key(it)) }
        val recentSeen = seen.filter { now - it.value < 14L * 24 * 3600 * 1000 }.keys
        fun fresh(list: List<Song>): List<Song> =
            list.filter { key(it) !in knownIds && key(it) !in recentSeen }.distinctBy { key(it) }

        val pool = (s.liked.toList() + s.history.toList() + s.topPlayed()).distinctBy { it.id }
        val seeds = pool.shuffled(rng)
        val used = HashSet<String>()
        val shelves = ArrayList<YtShelf>()

        // the main picks: songs related to a few of your own songs
        val quickAll = ArrayList<Song>()
        for (sd in seeds.take(4)) {
            try {
                quickAll.addAll(Engine.related(sd.url).take(14))
            } catch (e: Exception) {
            }
        }
        val quickList = fresh(quickAll).shuffled(rng).take(20)
        quickList.forEach { used.add(key(it)) }

        // "because you played ..."
        for (sd in seeds.drop(4).take(2)) {
            try {
                val r = fresh(Engine.related(sd.url)).filter { key(it) !in used }.shuffled(rng).take(14)
                if (r.size >= 4) {
                    shelves.add(YtShelf("Because you played " + sd.title, r))
                    r.forEach { used.add(key(it)) }
                }
            } catch (e: Exception) {
            }
        }

        // more from one of your top artists
        val artist = s.topArtists().map { it.first }.shuffled(rng).firstOrNull()
        if (artist != null) {
            try {
                val r = fresh(Engine.search(artist + " songs")).filter { key(it) !in used }.take(14)
                if (r.size >= 4) {
                    shelves.add(YtShelf("More from " + artist, r))
                    r.forEach { used.add(key(it)) }
                }
            } catch (e: Exception) {
            }
        }

        // something different, a new style every day
        for (g in genres.shuffled(rng).take(2)) {
            try {
                val r = fresh(Engine.search(g + " songs")).filter { key(it) !in used }.shuffled(rng).take(14)
                if (r.size >= 4) {
                    shelves.add(YtShelf("Discover " + g, r))
                    r.forEach { used.add(key(it)) }
                }
            } catch (e: Exception) {
            }
        }

        // the mood of the day
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val (mTitle, mQuery) = when {
            hour < 6 -> "Late night" to "late night chill songs"
            hour < 11 -> "Morning energy" to "morning feel good songs"
            hour < 17 -> "Afternoon focus" to "focus songs"
            hour < 22 -> "Evening vibes" to "evening relax songs"
            else -> "Wind down" to "calm night songs"
        }
        try {
            val r = fresh(Engine.search(mQuery)).filter { key(it) !in used }.shuffled(rng).take(14)
            if (r.size >= 4) {
                shelves.add(YtShelf(mTitle, r))
                r.forEach { used.add(key(it)) }
            }
        } catch (e: Exception) {
        }

        // remember what was shown, so it does not come back
        used.forEach { seen[it] = now }
        while (seen.size > 800) seen.remove(seen.keys.first())
        return Pair(quickList, shelves)
    }
}

@Composable
fun FreshEffect(s: AppState) {
    val ctx = LocalContext.current
    LaunchedEffect(s.signedIn, Opts.offline, s.history.size > 0) {
        if (!Opts.offline) Fresh.refresh(ctx, s)
    }
}

// the extra shelves on Home, with a Refresh button
@Composable
fun FreshHome(s: AppState) {
    val ctx = LocalContext.current
    if (Opts.offline) return
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Chip(if (Fresh.busy) "Finding new songs..." else "Refresh picks", false, s.accent) {
                Fresh.refresh(ctx, s, true)
            }
        }
        Fresh.shelves.toList().forEach { sh ->
            Text(
                sh.title,
                color = Color.White,
                fontFamily = PrismFont,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 12.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) { itemsIndexed(sh.songs.take(15)) { i, sg -> RecentCard(sg, s, sh.songs, Modifier.appear(i)) } }
        }
    }
}
