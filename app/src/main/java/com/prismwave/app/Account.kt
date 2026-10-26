package com.prismwave.app

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class RemotePlaylist(val browseId: String, val name: String)

object YtAccount {
    const val UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
    private const val ORIGIN = "https://music.youtube.com"
    private const val CLIENT_VERSION = "1.20250310.01.00"
    private val http = OkHttpClient()

    class AuthException : Exception("Session expired, sign in again")

    private fun cookieValue(cookie: String, name: String): String? =
        Regex("(?:^|;\\s*)" + Regex.escape(name) + "=([^;]+)").find(cookie)?.groupValues?.get(1)

    private fun authHeader(sapisid: String): String {
        val ts = System.currentTimeMillis() / 1000
        val digest = MessageDigest.getInstance("SHA-1").digest((ts.toString() + " " + sapisid + " " + ORIGIN).toByteArray())
        val hash = digest.joinToString("") { "%02x".format(it) }
        return "SAPISIDHASH " + ts + "_" + hash
    }

    private fun browse(cookie: String, browseId: String?, continuation: String?): JSONObject {
        val sid = cookieValue(cookie, "SAPISID") ?: cookieValue(cookie, "__Secure-3PAPISID") ?: throw AuthException()
        val client = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", CLIENT_VERSION)
            .put("hl", "en")
            .put("gl", "US")
        val body = JSONObject().put("context", JSONObject().put("client", client))
        var url = ORIGIN + "/youtubei/v1/browse?prettyPrint=false"
        if (continuation != null) {
            val t = URLEncoder.encode(continuation, "UTF-8")
            url += "&ctoken=" + t + "&continuation=" + t + "&type=next"
        } else {
            body.put("browseId", browseId)
        }
        val req = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .header("Cookie", cookie)
            .header("Authorization", authHeader(sid))
            .header("X-Goog-AuthUser", "0")
            .header("Origin", ORIGIN)
            .header("X-Origin", ORIGIN)
            .header("Referer", "$ORIGIN/")
            .header("User-Agent", UA)
            .build()
        http.newCall(req).execute().use { r ->
            if (r.code == 401 || r.code == 403) throw AuthException()
            if (!r.isSuccessful) throw IOException("YouTube returned " + r.code)
            return JSONObject(r.body?.string() ?: "{}")
        }
    }

    // Visits every JSON object that sits under a key, anywhere in the tree.
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

    private val durRegex = Regex("^\\d{1,2}(:\\d{2}){1,2}$")

    private fun findDurationSecs(node: Any?): Int {
        when (node) {
            is JSONObject -> {
                val keys = node.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = node.opt(k)
                    if (v is String && (k == "text" || k == "simpleText") && durRegex.matches(v.trim())) {
                        return seconds(v)
                    }
                    val r = findDurationSecs(v)
                    if (r > 0) return r
                }
            }
            is JSONArray -> for (i in 0 until node.length()) {
                val r = findDurationSecs(node.opt(i))
                if (r > 0) return r
            }
        }
        return 0
    }

    private fun parseSong(r: JSONObject): Song? {
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
        val artist = col(1).ifEmpty { "Unknown" }
        val dur = r.optJSONArray("fixedColumns")?.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
            ?.optJSONObject("text")
        val thumb = lastThumb(
            r.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        )
        val url = "https://www.youtube.com/watch?v=" + videoId
        var secs = seconds(firstRunText(dur))
        if (secs <= 0) secs = findDurationSecs(r)
        return Song(url, title, artist, secs, url, thumb)
    }

    private fun findContinuation(json: JSONObject): String? {
        var token: String? = null
        walk(json) { key, v ->
            if (token == null) {
                if (key == "nextContinuationData") token = v.optString("continuation").ifEmpty { null }
                if (key == "continuationCommand") token = v.optString("token").ifEmpty { null }
            }
        }
        return token
    }

    suspend fun profilePhoto(cookie: String): String? = withContext(Dispatchers.IO) {
        try {
            val sid = cookieValue(cookie, "SAPISID") ?: cookieValue(cookie, "__Secure-3PAPISID") ?: return@withContext null
            val client = JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", CLIENT_VERSION)
                .put("hl", "en")
                .put("gl", "US")
            val body = JSONObject().put("context", JSONObject().put("client", client))
            val req = Request.Builder()
                .url(ORIGIN + "/youtubei/v1/account/account_menu?prettyPrint=false")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .header("Cookie", cookie)
                .header("Authorization", authHeader(sid))
                .header("X-Goog-AuthUser", "0")
                .header("Origin", ORIGIN)
                .header("X-Origin", ORIGIN)
                .header("Referer", "$ORIGIN/")
                .header("User-Agent", UA)
                .build()
            val text = http.newCall(req).execute().use { r ->
                if (!r.isSuccessful) null else r.body?.string()
            }
            if (text == null) return@withContext null
            var found: String? = null
            walk(JSONObject(text)) { key, v ->
                if (found == null && key == "accountPhoto") found = lastThumb(v.optJSONArray("thumbnails"))
            }
            found
        } catch (e: Exception) {
            null
        }
    }

    suspend fun playlists(cookie: String): List<RemotePlaylist> = withContext(Dispatchers.IO) {
        val json = browse(cookie, "FEmusic_liked_playlists", null)
        val out = ArrayList<RemotePlaylist>()
        walk(json) { key, v ->
            if (key == "musicTwoRowItemRenderer") {
                val id = v.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                    ?.optString("browseId").orEmpty()
                val title = firstRunText(v.optJSONObject("title"))
                if (id.startsWith("VL") && id != "VLLM" && title.isNotEmpty()) out.add(RemotePlaylist(id, title))
            }
        }
        out.distinctBy { it.browseId }
    }

    suspend fun songs(cookie: String, browseId: String): List<Song> = withContext(Dispatchers.IO) {
        val out = ArrayList<Song>()
        var json = browse(cookie, browseId, null)
        var pages = 0
        while (true) {
            val shelves = ArrayList<JSONObject>()
            walk(json) { key, v ->
                if (key == "musicPlaylistShelfRenderer" || key == "musicPlaylistShelfContinuation") shelves.add(v)
            }
            val roots: List<Any> = if (shelves.isNotEmpty()) shelves else listOf(json)
            for (root in roots) {
                walk(root) { key, v ->
                    if (key == "musicResponsiveListItemRenderer") parseSong(v)?.let { out.add(it) }
                }
            }
            val token = findContinuation(json)
            pages++
            if (token == null || pages >= 12) break
            json = browse(cookie, null, token)
        }
        out.distinctBy { it.id }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(s: AppState) {
    BackHandler { s.showLogin = false }
    var info by remember { mutableStateOf("Loading...") }
    Column(
        Modifier
            .fillMaxSize()
            .background(Night)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundBtn(Icons.Rounded.Close, 0.9f, { s.showLogin = false })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Sign in to YouTube Music", color = Color.White, fontWeight = FontWeight.Bold)
                Text(info, color = TextDim, fontSize = 12.sp, maxLines = 1)
            }
            Box(
                Modifier
                    .bounceClick {
                        val c = CookieManager.getInstance().getCookie("https://music.youtube.com") ?: ""
                        if (c.contains("SAPISID")) s.onLogin(c) else info = "Not signed in yet, finish the sign-in first"
                    }
                    .clip(RoundedCornerShape(50))
                    .background(s.accent)
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) { Text("Done", color = Color.Black, fontWeight = FontWeight.SemiBold) }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            info = (url ?: "").removePrefix("https://").take(60)
                            if (url != null && url.startsWith("https://music.youtube.com")) {
                                val c = CookieManager.getInstance().getCookie("https://music.youtube.com") ?: ""
                                if (c.contains("SAPISID")) {
                                    CookieManager.getInstance().flush()
                                    s.onLogin(c)
                                }
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                info = "Page error: " + (error?.description ?: "unknown")
                            }
                        }
                    }
                    loadUrl(
                        "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true" +
                            "&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue" +
                            "%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F"
                    )
                }
            }
        )
    }
}
