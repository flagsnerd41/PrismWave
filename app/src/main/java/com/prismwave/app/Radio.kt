package com.prismwave.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * The same "radio" YouTube Music builds for a song: music only, songs that fit the taste of the song.
 * It is what the YouTube Music app plays after a song when you start it from a search.
 */
object YtRadio {
    private const val ORIGIN = "https://music.youtube.com"
    private const val CLIENT_VERSION = "1.20250310.01.00"
    private val http = OkHttpClient()

    // the cookie of the signed in account: with it YouTube Music tunes the radio to your own taste
    @Volatile var cookie: String = ""

    private fun auth(cookie: String): String? {
        val sid = Regex("(?:^|;\\s*)SAPISID=([^;]+)").find(cookie)?.groupValues?.get(1)
            ?: Regex("(?:^|;\\s*)__Secure-3PAPISID=([^;]+)").find(cookie)?.groupValues?.get(1) ?: return null
        val ts = System.currentTimeMillis() / 1000
        val digest = java.security.MessageDigest.getInstance("SHA-1").digest(("$ts $sid $ORIGIN").toByteArray())
        return "SAPISIDHASH " + ts + "_" + digest.joinToString("") { "%02x".format(it) }
    }
    private val clean = Regex("\\(.*?\\)|\\[.*?\\]")
    private val notWord = Regex("[^\\p{L}\\p{N} ]")
    private val spaces = Regex("\\s+")

    fun norm(t: String): String =
        t.lowercase().replace(clean, " ").replace(notWord, " ").replace(spaces, " ").trim()

    // true when the title is the same song again (live, remix, lyric video, another upload...)
    fun sameSong(seedTitle: String, other: String): Boolean {
        val a = norm(seedTitle)
        val b = norm(other)
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        return a.length >= 8 && b.startsWith("$a ")
    }

    private fun runsText(o: JSONObject?): String {
        val runs = o?.optJSONArray("runs") ?: return ""
        return (0 until runs.length()).joinToString("") { runs.optJSONObject(it)?.optString("text").orEmpty() }
    }

    private fun artistOf(r: JSONObject): String {
        val runs = r.optJSONObject("longBylineText")?.optJSONArray("runs") ?: r.optJSONObject("shortBylineText")?.optJSONArray("runs")
        if (runs == null) return "Unknown"
        val sb = StringBuilder()
        for (i in 0 until runs.length()) {
            val t = runs.optJSONObject(i)?.optString("text").orEmpty()
            if (t.contains("\u2022")) break
            sb.append(t)
        }
        return sb.toString().trim().removeSuffix(" - Topic").ifEmpty { "Unknown" }
    }

    private fun secondsOf(text: String): Int {
        val parts = text.trim().split(":").mapNotNull { it.toIntOrNull() }
        if (parts.isEmpty()) return 0
        return parts.fold(0) { acc, n -> acc * 60 + n }
    }

    private fun collect(node: Any?, out: MutableList<JSONObject>) {
        when (node) {
            is JSONObject -> {
                val keys = node.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = node.opt(k)
                    if (k == "playlistPanelVideoRenderer" && v is JSONObject) out.add(v) else collect(v, out)
                }
            }
            is JSONArray -> for (i in 0 until node.length()) collect(node.opt(i), out)
        }
    }

    // songs that YouTube Music plays after this video, without the song itself and without its other versions
    suspend fun next(videoId: String): List<Song> = withContext(Dispatchers.IO) {
        val client = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", CLIENT_VERSION)
            .put("hl", "en")
            .put("gl", "US")
        val body = JSONObject()
            .put("context", JSONObject().put("client", client))
            .put("videoId", videoId)
            .put("playlistId", "RDAMVM$videoId")
            .put("params", "wAEB")
            .put("isAudioOnly", true)
        val req = Request.Builder()
            .url("$ORIGIN/youtubei/v1/next?prettyPrint=false")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .header("Origin", ORIGIN)
            .header("X-Origin", ORIGIN)
            .header("Referer", "$ORIGIN/")
            .header("User-Agent", YtAccount.UA)
            .apply {
                val c = cookie
                val a = if (c.isNotEmpty()) auth(c) else null
                if (a != null) {
                    header("Cookie", c)
                    header("Authorization", a)
                    header("X-Goog-AuthUser", "0")
                }
            }
            .build()
        val json = http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return@withContext emptyList<Song>()
            JSONObject(r.body?.string() ?: "{}")
        }
        val panel = ArrayList<JSONObject>()
        collect(json, panel)
        var seedTitle = ""
        val songs = ArrayList<Song>()
        for (r in panel) {
            val vid = r.optString("videoId")
            if (vid.isEmpty()) continue
            val title = runsText(r.optJSONObject("title"))
            if (title.isEmpty()) continue
            if (vid == videoId) {
                seedTitle = title
                continue
            }
            val secs = secondsOf(runsText(r.optJSONObject("lengthText")))
            val thumbs = r.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            val thumb = if (thumbs != null && thumbs.length() > 0) thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") else null
            val url = "https://www.youtube.com/watch?v=$vid"
            songs.add(Song(url, title, artistOf(r), secs, url, thumb))
        }
        songs
            .filter { seedTitle.isEmpty() || !sameSong(seedTitle, it.title) }
            .filter { it.seconds == 0 || it.seconds in 60..900 }
            .distinctBy { it.url }
            .distinctBy { norm(it.title) + "|" + norm(it.artist) }
    }
}
