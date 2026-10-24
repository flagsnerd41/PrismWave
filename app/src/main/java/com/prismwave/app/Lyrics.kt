package com.prismwave.app

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// ms = -1 means the lyrics are not synced (plain text only)
class LyricLine(val ms: Long, val text: String)

object Lyrics {
    private val cache = HashMap<String, List<LyricLine>>()
    private val timeTag = Regex("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]")

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

    private fun parse(lrc: String): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        for (raw in lrc.lines()) {
            val tags = timeTag.findAll(raw).toList()
            if (tags.isEmpty()) continue
            val text = raw.replace(timeTag, "").trim()
            for (m in tags) {
                val t = ((m.groupValues[1].toLong() * 60 + m.groupValues[2].toDouble()) * 1000).toLong()
                out.add(LyricLine(t, text))
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

    suspend fun fetch(song: Song): List<LyricLine> = withContext(Dispatchers.IO) {
        cache[song.id]?.let { return@withContext it }
        val title = song.title.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "").trim().ifEmpty { song.title }
        val artist = song.artist.replace(" - Topic", "").trim()
        var result: List<LyricLine> = emptyList()
        try {
            val q = "track_name=" + enc(title) + "&artist_name=" + enc(artist) +
                (if (song.seconds > 1) "&duration=" + song.seconds else "")
            val body = get("https://lrclib.net/api/get?" + q)
            if (body != null) result = fromObj(JSONObject(body))
            if (result.isEmpty()) {
                val body2 = get("https://lrclib.net/api/search?q=" + enc(artist + " " + title))
                if (body2 != null) {
                    val arr = JSONArray(body2)
                    for (i in 0 until arr.length()) {
                        val r = fromObj(arr.getJSONObject(i))
                        if (r.isNotEmpty()) {
                            result = r
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
        cache[song.id] = result
        result
    }
}
