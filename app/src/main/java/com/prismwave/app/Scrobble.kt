package com.prismwave.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** Last.fm and Libre.fm speak the same API, so one class covers both. */
class FmService(val id: String, val name: String, val apiUrl: String) {
    var enabled by mutableStateOf(false)
    var key by mutableStateOf("")
    var secret by mutableStateOf("")
    var user by mutableStateOf("")
    var session by mutableStateOf("")
    val connected: Boolean get() = session.isNotEmpty()
}

object Scrobbler {
    private var prefs: SharedPreferences? = null
    private val http = OkHttpClient()

    val lastfm = FmService("lastfm", "Last.fm", "https://ws.audioscrobbler.com/2.0/")
    val librefm = FmService("librefm", "Libre.fm", "https://libre.fm/2.0/")
    var lbEnabled by mutableStateOf(false)
    var lbToken by mutableStateOf("")
    var lbUser by mutableStateOf("")

    fun init(ctx: Context) {
        if (prefs != null) return
        val p = ctx.getSharedPreferences("prismwave_scrobble", Context.MODE_PRIVATE)
        prefs = p
        for (f in listOf(lastfm, librefm)) {
            f.enabled = p.getBoolean(f.id + "_on", false)
            f.key = p.getString(f.id + "_key", "") ?: ""
            f.secret = p.getString(f.id + "_secret", "") ?: ""
            f.user = p.getString(f.id + "_user", "") ?: ""
            f.session = p.getString(f.id + "_session", "") ?: ""
        }
        lbEnabled = p.getBoolean("lb_on", false)
        lbToken = p.getString("lb_token", "") ?: ""
        lbUser = p.getString("lb_user", "") ?: ""
    }

    fun save() {
        val e = prefs?.edit() ?: return
        for (f in listOf(lastfm, librefm)) {
            e.putBoolean(f.id + "_on", f.enabled)
            e.putString(f.id + "_key", f.key)
            e.putString(f.id + "_secret", f.secret)
            e.putString(f.id + "_user", f.user)
            e.putString(f.id + "_session", f.session)
        }
        e.putBoolean("lb_on", lbEnabled)
        e.putString("lb_token", lbToken)
        e.putString("lb_user", lbUser)
        e.apply()
    }

    // ---------- Last.fm / Libre.fm ----------
    private fun sign(params: Map<String, String>, secret: String): String {
        val sb = StringBuilder()
        params.toSortedMap().forEach { (k, v) ->
            if (k != "format" && k != "callback") sb.append(k).append(v)
        }
        sb.append(secret)
        val digest = MessageDigest.getInstance("MD5").digest(sb.toString().toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun fmCall(f: FmService, params: MutableMap<String, String>): JSONObject {
        params["api_key"] = f.key
        params["api_sig"] = sign(params, f.secret)
        params["format"] = "json"
        val body = FormBody.Builder()
        params.forEach { (k, v) -> body.add(k, v) }
        val req = Request.Builder()
            .url(f.apiUrl)
            .post(body.build())
            .header("User-Agent", "PrismWave/1.0")
            .build()
        val text = http.newCall(req).execute().use { it.body?.string() ?: "{}" }
        return JSONObject(text)
    }

    suspend fun fmLogin(f: FmService, username: String, password: String): String = withContext(Dispatchers.IO) {
        try {
            if (f.key.isEmpty() || f.secret.isEmpty()) {
                "Enter the API key and secret first"
            } else {
                val j = fmCall(
                    f,
                    mutableMapOf("method" to "auth.getMobileSession", "username" to username, "password" to password)
                )
                val sess = j.optJSONObject("session")
                if (sess != null) {
                    f.session = sess.optString("key")
                    f.user = sess.optString("name", username)
                    f.enabled = true
                    save()
                    "Connected as " + f.user
                } else {
                    j.optString("message", "Login failed")
                }
            }
        } catch (e: Exception) {
            "Login failed: " + (e.message ?: "error").take(60)
        }
    }

    fun fmLogout(f: FmService) {
        f.session = ""
        f.user = ""
        f.enabled = false
        save()
    }

    // ---------- ListenBrainz ----------
    suspend fun lbConnect(token: String): String = withContext(Dispatchers.IO) {
        try {
            val t = token.trim()
            val req = Request.Builder()
                .url("https://api.listenbrainz.org/1/validate-token")
                .header("Authorization", "Token " + t)
                .build()
            val text = http.newCall(req).execute().use { it.body?.string() ?: "{}" }
            val j = JSONObject(text)
            if (j.optBoolean("valid")) {
                lbToken = t
                lbUser = j.optString("user_name")
                lbEnabled = true
                save()
                "Connected as " + lbUser
            } else {
                "That token is not valid"
            }
        } catch (e: Exception) {
            "Could not connect: " + (e.message ?: "error").take(60)
        }
    }

    fun lbDisconnect() {
        lbToken = ""
        lbUser = ""
        lbEnabled = false
        save()
    }

    private fun lbSubmit(type: String, song: Song, ts: Long?) {
        val extra = JSONObject()
            .put("media_player", "PrismWave")
            .put("submission_client", "PrismWave")
        if (song.seconds > 0) extra.put("duration", song.seconds)
        val meta = JSONObject()
            .put("artist_name", song.artist)
            .put("track_name", song.title)
            .put("additional_info", extra)
        val item = JSONObject().put("track_metadata", meta)
        if (ts != null) item.put("listened_at", ts)
        val body = JSONObject().put("listen_type", type).put("payload", JSONArray().put(item))
        val req = Request.Builder()
            .url("https://api.listenbrainz.org/1/submit-listens")
            .header("Authorization", "Token " + lbToken)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { }
    }

    // ---------- called while playing ----------
    suspend fun nowPlaying(song: Song) {
        withContext(Dispatchers.IO) {
            for (f in listOf(lastfm, librefm)) {
                if (f.enabled && f.connected) {
                    try {
                        fmCall(
                            f,
                            mutableMapOf(
                                "method" to "track.updateNowPlaying",
                                "sk" to f.session,
                                "artist" to song.artist,
                                "track" to song.title
                            )
                        )
                    } catch (e: Exception) {
                    }
                }
            }
            if (lbEnabled && lbToken.isNotEmpty()) {
                try {
                    lbSubmit("playing_now", song, null)
                } catch (e: Exception) {
                }
            }
        }
    }

    suspend fun scrobble(song: Song, startedAt: Long) {
        withContext(Dispatchers.IO) {
            for (f in listOf(lastfm, librefm)) {
                if (f.enabled && f.connected) {
                    try {
                        fmCall(
                            f,
                            mutableMapOf(
                                "method" to "track.scrobble",
                                "sk" to f.session,
                                "artist" to song.artist,
                                "track" to song.title,
                                "timestamp" to startedAt.toString()
                            )
                        )
                    } catch (e: Exception) {
                    }
                }
            }
            if (lbEnabled && lbToken.isNotEmpty()) {
                try {
                    lbSubmit("single", song, startedAt)
                } catch (e: Exception) {
                }
            }
        }
    }
}

/** Sends "now playing" when a song starts and a scrobble after half the song (or 4 minutes). */
@Composable
fun ScrobbleWatcher(s: AppState) {
    val ctx = LocalContext.current
    Scrobbler.init(ctx.applicationContext)
    val cur = s.current
    LaunchedEffect(cur?.id) {
        if (cur == null) return@LaunchedEffect
        val startedAt = System.currentTimeMillis() / 1000
        Scrobbler.nowPlaying(cur)
        var played = 0
        while (true) {
            delay(1000)
            if (s.playing) played++
            val dur = if (cur.seconds > 0) cur.seconds else 240
            if (dur >= 30 && (played >= dur / 2 || played >= 240)) {
                Scrobbler.scrobble(cur, startedAt)
                break
            }
        }
    }
}
