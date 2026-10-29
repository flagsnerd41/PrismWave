package com.prismwave.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject

/**
 * Discord "Listening to PrismWave" status. Unofficial: it logs in with your own Discord
 * account (like other music apps do), so use an alt account if you worry about that.
 * The login token stays on this phone only.
 */
object DiscordRpc {
    var enabled by mutableStateOf(false)
    var loginOpen by mutableStateOf(false)
    var appId by mutableStateOf("")
    var status by mutableStateOf("")
    private var token by mutableStateOf("")

    private var sp: SharedPreferences? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient()
    private var ws: WebSocket? = null
    private var beat: Job? = null
    private var seq = -1
    private var ready = false
    private var started = false
    private var authFailed = false
    private var lastSong: Song? = null
    private var lastPos = 0f
    private var lastPlaying = false
    private val assetCache = HashMap<String, String>()

    fun hasToken(): Boolean = token.isNotEmpty()

    fun init(ctx: Context) {
        if (sp != null) return
        val p = ctx.getSharedPreferences("prismwave_discord", Context.MODE_PRIVATE)
        sp = p
        token = p.getString("token", "") ?: ""
        enabled = p.getBoolean("enabled", false) && token.isNotEmpty()
        appId = p.getString("appId", "") ?: ""
    }

    /** Follows the current song and play state, also while the app is in the background. */
    fun start(s: AppState) {
        if (started) return
        started = true
        scope.launch {
            snapshotFlow { Triple(s.current, s.playing, enabled) }.collect { (song, playing, on) ->
                lastSong = song
                lastPos = s.position
                lastPlaying = playing
                if (!on || token.isEmpty()) {
                    disconnect()
                } else {
                    connect()
                    push()
                }
            }
        }
    }

    fun setEnabled(ctx: Context, on: Boolean) {
        init(ctx)
        enabled = on && token.isNotEmpty()
        sp?.edit()?.putBoolean("enabled", enabled)?.apply()
        if (!enabled) disconnect() else connect()
    }

    fun setAppId(ctx: Context, v: String) {
        init(ctx)
        appId = v
        assetCache.clear()
        sp?.edit()?.putString("appId", v)?.apply()
    }

    fun onToken(ctx: Context, t: String) {
        init(ctx)
        token = t
        authFailed = false
        enabled = true
        loginOpen = false
        status = "Signed in"
        sp?.edit()?.putString("token", t)?.putBoolean("enabled", true)?.apply()
        scope.launch { connect() }
    }

    fun logout(ctx: Context) {
        init(ctx)
        disconnect()
        token = ""
        enabled = false
        status = ""
        sp?.edit()?.remove("token")?.putBoolean("enabled", false)?.apply()
    }

    private fun heartbeatJson(): String =
        JSONObject().put("op", 1).put("d", if (seq < 0) JSONObject.NULL else seq).toString()

    private fun connect() {
        if (ws != null || token.isEmpty() || authFailed) return
        status = "Connecting..."
        ready = false
        seq = -1
        val req = Request.Builder().url("wss://gateway.discord.gg/?v=10&encoding=json").build()
        ws = client.newWebSocket(req, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val o = JSONObject(text)
                    if (!o.isNull("s")) seq = o.optInt("s", seq)
                    when (o.optInt("op", -1)) {
                        10 -> {
                            val iv = o.getJSONObject("d").getLong("heartbeat_interval")
                            startBeat(webSocket, iv)
                            identify(webSocket)
                        }
                        1 -> webSocket.send(heartbeatJson())
                        9 -> {
                            status = "Discord refused the login. Sign in again"
                            webSocket.close(1000, null)
                        }
                        0 -> if (o.optString("t") == "READY") {
                            ready = true
                            status = "Connected"
                            scope.launch { push() }
                        }
                    }
                } catch (e: Exception) {
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (code == 4004) {
                    authFailed = true
                    status = "Discord rejected the login. Sign out and sign in again"
                }
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                dropped(webSocket)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                dropped(webSocket)
            }
        })
    }

    private fun dropped(webSocket: WebSocket) {
        if (ws !== webSocket) return
        ws = null
        ready = false
        beat?.cancel()
        if (enabled && token.isNotEmpty() && !authFailed) {
            if (status == "Connected" || status == "Connecting...") status = "Disconnected, retrying..."
            scope.launch {
                delay(8000)
                if (enabled && ws == null) connect()
            }
        }
    }

    private fun startBeat(webSocket: WebSocket, interval: Long) {
        beat?.cancel()
        beat = scope.launch {
            delay((interval * Math.random()).toLong())
            while (isActive) {
                webSocket.send(heartbeatJson())
                delay(interval)
            }
        }
    }

    private fun identify(webSocket: WebSocket) {
        val props = JSONObject().put("os", "Android").put("browser", "Discord Android").put("device", "Android")
        val presence = JSONObject()
            .put("status", "online").put("since", 0).put("activities", JSONArray()).put("afk", false)
        val d = JSONObject()
            .put("token", token).put("properties", props).put("compress", false).put("presence", presence)
        webSocket.send(JSONObject().put("op", 2).put("d", d).toString())
    }

    private fun disconnect() {
        beat?.cancel()
        val w = ws
        ws = null
        ready = false
        try {
            w?.close(1000, null)
        } catch (e: Exception) {
        }
        if (!enabled) status = ""
    }

    private fun push() {
        if (!ready) return
        val sg = lastSong
        val acts = JSONArray()
        if (sg != null && lastPlaying) acts.put(buildActivity(sg))
        val d = JSONObject().put("since", 0).put("activities", acts).put("status", "online").put("afk", false)
        ws?.send(JSONObject().put("op", 3).put("d", d).toString())
    }

    private fun buildActivity(sg: Song): JSONObject {
        val start = System.currentTimeMillis() - (lastPos * 1000).toLong()
        val a = JSONObject()
            .put("name", "PrismWave").put("type", 2)
            .put("details", sg.title).put("state", sg.artist)
        val ts = JSONObject().put("start", start)
        if (sg.seconds > 1) ts.put("end", start + sg.seconds * 1000L)
        a.put("timestamps", ts)
        if (appId.isNotBlank()) {
            a.put("application_id", appId.trim())
            val img = resolveImage(sg)
            if (img != null) a.put("assets", JSONObject().put("large_image", img).put("large_text", sg.title))
        }
        return a
    }

    // Cover art needs your own Discord application id (free, discord.com/developers)
    private fun resolveImage(sg: Song): String? {
        val thumb = sg.thumb ?: return null
        val url = artUrl(thumb, 512)
        assetCache[url]?.let { return it }
        return try {
            val body = JSONObject().put("urls", JSONArray().put(url)).toString()
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("https://discord.com/api/v9/applications/" + appId.trim() + "/external-assets")
                .header("Authorization", token)
                .post(body)
                .build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) {
                    null
                } else {
                    val arr = JSONArray(r.body?.string() ?: "[]")
                    val p = arr.optJSONObject(0)?.optString("external_asset_path", "")
                    if (p.isNullOrEmpty()) null else ("mp:" + p).also { assetCache[url] = it }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

private const val DISCORD_JS =
    "(function(){try{var f=document.createElement('iframe');document.body.appendChild(f);" +
        "var t=f.contentWindow.localStorage.getItem('token');f.remove();return t?t:'';}catch(e){return '';}})()"

/** Opens the Discord sign-in in its own plain Android screen (not inside Compose). */
@Composable
fun DiscordLogin() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        DiscordRpc.loginOpen = false
        ctx.startActivity(android.content.Intent(ctx, DiscordLoginActivity::class.java))
    }
}

/** Starts the Discord status once, from anywhere that is always on screen. */
@Composable
fun DiscordBridge(s: AppState) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        DiscordRpc.init(ctx.applicationContext)
        DiscordRpc.start(s)
    }
}
