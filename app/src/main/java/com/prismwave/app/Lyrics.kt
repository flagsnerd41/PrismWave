package com.prismwave.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// ms = -1 means the lyrics are not synced (plain text only)
internal class LyricLine(val ms: Long, val text: String, val words: List<ArchiveTimedWord>? = null)

// look of the lyrics page and the sync fix for each song
object LyricsPrefs {
    private var p: SharedPreferences? = null
    var size by mutableStateOf(1)
    var center by mutableStateOf(false)

    fun init(ctx: Context) {
        if (p != null) return
        val sp = ctx.applicationContext.getSharedPreferences("prismwave_lyrics", Context.MODE_PRIVATE)
        p = sp
        size = sp.getInt("size", 1).coerceIn(0, 2)
        center = sp.getBoolean("center", false)
    }

    fun save() {
        p?.edit()?.putInt("size", size)?.putBoolean("center", center)?.apply()
    }

    fun offset(id: String): Long = p?.getLong("off_" + id, 0L) ?: 0L

    fun setOffset(id: String, v: Long) {
        p?.edit()?.putLong("off_" + id, v)?.apply()
    }
}

object Lyrics {
    private val cache = HashMap<String, List<LyricLine>>()
    private val timeTag = Regex("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]")
    private var dir: File? = null

    // lyrics are also kept on the phone, so they show at once and work offline
    fun init(ctx: Context) {
        if (dir != null) return
        val d = File(ctx.applicationContext.cacheDir, "lyrics")
        d.mkdirs()
        dir = d
    }

    private fun fileFor(id: String): File? =
        dir?.let { File(it, id.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json") }

    private fun load(id: String): List<LyricLine>? {
        val f = fileFor(id) ?: return null
        if (!f.exists()) return null
        return try {
            val arr = JSONArray(f.readText())
            val out = ArrayList<LyricLine>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONArray(i)
                val words = if (o.length() > 2) {
                    val wordArray = o.optJSONArray(2)
                    if (wordArray == null) null else (0 until wordArray.length()).mapNotNull { index ->
                        val w = wordArray.optJSONArray(index) ?: return@mapNotNull null
                        if (w.length() < 3) return@mapNotNull null
                        ArchiveTimedWord(w.optString(0), w.optLong(1), w.optLong(2))
                    }.takeIf { it.isNotEmpty() }
                } else null
                out.add(LyricLine(o.getLong(0), o.getString(1), words))
            }
            if (out.isEmpty()) null else out
        } catch (e: Exception) {
            null
        }
    }

    private fun save(id: String, lines: List<LyricLine>) {
        val f = fileFor(id) ?: return
        try {
            val arr = JSONArray()
            for (l in lines) {
                val item = JSONArray().put(l.ms).put(l.text)
                l.words?.let { words ->
                    val wordArray = JSONArray()
                    words.forEach { word -> wordArray.put(JSONArray().put(word.text).put(word.startMs).put(word.endMs)) }
                    item.put(wordArray)
                }
                arr.put(item)
            }
            f.writeText(arr.toString())
        } catch (e: Exception) {
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun get(url: String): String? {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "PrismWave/1.0")
        return try {
            if (c.responseCode == 200) c.inputStream.bufferedReader().readText() else null
        } finally {
            c.disconnect()
        }
    }

    private val wordTimeTag = Regex("<(\\d+):(\\d+(?:\\.\\d+)?)>")
    
    private fun timestampMs(match: MatchResult): Long =
        ((match.groupValues[1].toLong() * 60 + match.groupValues[2].toDouble()) * 1000).toLong()

    // Supports normal LRC plus enhanced LRC word markers. Word timings are only
    // created when the provider actually supplies timestamp markers.
    private fun parse(lrc: String): List<LyricLine> {
        val sourceLines = lrc.lines()
        val out = ArrayList<LyricLine>()
        for ((lineIndex, raw) in sourceLines.withIndex()) {
            val lineTags = timeTag.findAll(raw).toList()
            if (lineTags.isEmpty()) continue
            val lineText = raw.replace(timeTag, "").trim()
            if (lineText.isEmpty()) continue
            val wordTags = wordTimeTag.findAll(lineText).toList()
            val baseLineStart = timestampMs(lineTags.first())
            val followingLineStart = sourceLines.drop(lineIndex + 1)
                .firstNotNullOfOrNull { nextLine -> timeTag.find(nextLine)?.let(::timestampMs) }
                ?.takeIf { it > baseLineStart }
            val parsedWords = if (wordTags.isEmpty()) null else {
                wordTags.mapIndexedNotNull { index, tag ->
                    val startIndex = tag.range.last + 1
                    val endIndex = wordTags.getOrNull(index + 1)?.range?.first ?: lineText.length
                    val prefix = if (index == 0) lineText.substring(0, tag.range.first) else ""
                    val tokenText = prefix + lineText.substring(startIndex, endIndex)
                    if (tokenText.isEmpty()) return@mapIndexedNotNull null
                    val start = timestampMs(tag)
                    val nextStart = wordTags.getOrNull(index + 1)?.let(::timestampMs)
                        ?: followingLineStart
                    ArchiveTimedWord(
                        text = tokenText,
                        startMs = start,
                        endMs = (nextStart ?: (start + 650L)).coerceAtLeast(start + 1L),
                    )
                }.takeIf { it.isNotEmpty() }
            }
            lineTags.forEach { lineTag ->
                val lineStart = timestampMs(lineTag)
                val offset = lineStart - baseLineStart
                val words = parsedWords?.map { word ->
                    word.copy(startMs = (word.startMs + offset).coerceAtLeast(0L),
                        endMs = (word.endMs + offset).coerceAtLeast(word.startMs + offset + 1L))
                }
                out.add(LyricLine(lineStart, lineText.replace(wordTimeTag, ""), words))
            }
        }
        return out.sortedBy { it.ms }
    }

    private fun fromObj(o: JSONObject): List<LyricLine> {
        val synced = o.optString("syncedLyrics", "")
        if (synced.isNotBlank() && synced != "null") {
            val p = parse(synced)
            if (p.isNotEmpty()) return p
        }
        val plain = o.optString("plainLyrics", "")
        if (plain.isNotBlank() && plain != "null") return plain.lines().map { LyricLine(-1L, it.trim()) }
        return emptyList()
    }

    private val junk = Regex(
        "(?i)\\b(official( music)? (video|audio)|lyrics?( video)?|music video|visuali[sz]er|remaster(ed)?( \\d{4})?|hd|hq|4k)\\b"
    )
    private val featPart = Regex("(?i)\\s+(feat\\.?|ft\\.?)\\s+.*$")

    private fun clean(raw: String): String {
        var t = raw.replace(Regex("\\(.*?\\)|\\[.*?\\]"), " ")
        t = t.replace(junk, " ").replace(featPart, "")
        t = t.replace(Regex("\\s+"), " ").trim().trim('-', '|', ' ')
        return t
    }

    // titles and artists to try, YouTube uploads are often named "Artist - Song"
    private fun candidates(song: Song): List<Pair<String, String>> {
        val artist = song.artist.replace(" - Topic", "").replace(Regex("(?i)vevo$"), "").trim()
        val title = clean(song.title).ifEmpty { song.title }
        val out = ArrayList<Pair<String, String>>()
        out.add(Pair(title, artist))
        val i = title.indexOf(" - ")
        if (i > 0) {
            val left = title.substring(0, i).trim()
            val right = title.substring(i + 3).trim()
            if (left.isNotEmpty() && right.isNotEmpty()) {
                out.add(Pair(right, left))
                out.add(Pair(left, right))
            }
        }
        return out.distinct()
    }

    internal suspend fun fetch(song: Song): List<LyricLine> = withContext(Dispatchers.IO) {
        cache[song.id]?.let { return@withContext it }
        load(song.id)?.let {
            cache[song.id] = it
            return@withContext it
        }
        val cands = candidates(song)
        var best: List<LyricLine> = emptyList()
        try {
            for ((t, a) in cands) {
                val q = "track_name=" + enc(t) + "&artist_name=" + enc(a) +
                    (if (song.seconds > 1) "&duration=" + song.seconds else "")
                val body = get("https://lrclib.net/api/get?" + q) ?: continue
                val r = fromObj(JSONObject(body))
                if (r.isNotEmpty()) {
                    best = r
                    if (r[0].ms >= 0) break
                }
            }
            if (best.isEmpty() || best[0].ms < 0) {
                val queries = ArrayList<String>()
                for ((t, a) in cands) queries.add("q=" + enc(a + " " + t))
                queries.add("track_name=" + enc(cands[0].first))
                for (qq in queries) {
                    val body = get("https://lrclib.net/api/search?" + qq) ?: continue
                    val arr = JSONArray(body)
                    var pick: List<LyricLine> = emptyList()
                    var pickScore = -1
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val r = fromObj(o)
                        if (r.isEmpty()) continue
                        val synced = r[0].ms >= 0
                        val d = o.optDouble("duration", -1.0)
                        val close = d > 0 && song.seconds > 1 && kotlin.math.abs(d - song.seconds) <= 4
                        val score = (if (synced) 2 else 0) + (if (close) 3 else 0)
                        if (score > pickScore) {
                            pickScore = score
                            pick = r
                        }
                    }
                    if (pick.isNotEmpty() && (best.isEmpty() || (pick[0].ms >= 0 && best[0].ms < 0))) best = pick
                    if (best.isNotEmpty() && best[0].ms >= 0) break
                }
            }
        } catch (e: Exception) {
        }
        if (best.isNotEmpty()) cache[song.id] = best
        if (best.isNotEmpty()) save(song.id, best)
        best
    }
}
