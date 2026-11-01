package com.prismwave.app

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// ---------------------------------------------------------------- settings of one "sound"

data class FxSettings(
    val name: String,
    val reverb: Int = 0,
    val muffle: Float = 0f,
    val bass: Float = 0f,
    val speed: Float = 1f,
    val pitch: Float = 1f,
    val crickets: Float = 0f,
    val rain: Float = 0f,
    val fire: Float = 0f,
    val ocean: Float = 0f,
    val wind: Float = 0f
) {
    fun toJson(): JSONObject = JSONObject()
        .put("name", name)
        .put("reverb", reverb)
        .put("muffle", muffle.toDouble())
        .put("bass", bass.toDouble())
        .put("speed", speed.toDouble())
        .put("pitch", pitch.toDouble())
        .put("crickets", crickets.toDouble())
        .put("rain", rain.toDouble())
        .put("fire", fire.toDouble())
        .put("ocean", ocean.toDouble())
        .put("wind", wind.toDouble())
}

fun fxFromJson(o: JSONObject): FxSettings = FxSettings(
    name = o.optString("name", "My sound").ifBlank { "My sound" }.take(24),
    reverb = o.optInt("reverb", 0).coerceIn(0, 6),
    muffle = o.optDouble("muffle", 0.0).toFloat().coerceIn(0f, 1f),
    bass = o.optDouble("bass", 0.0).toFloat().coerceIn(0f, 1f),
    speed = o.optDouble("speed", 1.0).toFloat().coerceIn(0.5f, 1.5f),
    pitch = o.optDouble("pitch", 1.0).toFloat().coerceIn(0.5f, 1.5f),
    crickets = o.optDouble("crickets", 0.0).toFloat().coerceIn(0f, 1f),
    rain = o.optDouble("rain", 0.0).toFloat().coerceIn(0f, 1f),
    fire = o.optDouble("fire", 0.0).toFloat().coerceIn(0f, 1f),
    ocean = o.optDouble("ocean", 0.0).toFloat().coerceIn(0f, 1f),
    wind = o.optDouble("wind", 0.0).toFloat().coerceIn(0f, 1f)
)

// ---------------------------------------------------------------- saved options + presets

object StudioPrefs {
    private var p: SharedPreferences? = null
    var starOn by mutableStateOf(true)
    var lightsOn by mutableStateOf(true)
    var apiKey by mutableStateOf("")
    var model by mutableStateOf("claude-haiku-5-5")
    var presets by mutableStateOf<List<FxSettings>>(emptyList())
    var lastStar = 0L

    fun init(ctx: Context) {
        if (p != null) return
        val sp = ctx.applicationContext.getSharedPreferences("prismwave_studio", Context.MODE_PRIVATE)
        p = sp
        starOn = sp.getBoolean("starOn", true)
        lightsOn = sp.getBoolean("lightsOn", true)
        apiKey = sp.getString("apiKey", "") ?: ""
        model = sp.getString("model", "claude-haiku-5-5") ?: "claude-haiku-5-5"
        try {
            val arr = JSONArray(sp.getString("presets", "[]") ?: "[]")
            presets = (0 until arr.length()).map { fxFromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            presets = emptyList()
        }
    }

    fun save() {
        p?.edit()
            ?.putBoolean("starOn", starOn)
            ?.putBoolean("lightsOn", lightsOn)
            ?.putString("apiKey", apiKey.trim())
            ?.putString("model", model.trim().ifEmpty { "claude-haiku-5-5" })
            ?.apply()
    }

    private fun savePresets() {
        p?.edit()?.putString("presets", JSONArray(presets.map { it.toJson() }).toString())?.apply()
    }

    fun addPreset(f: FxSettings) {
        presets = (listOf(f) + presets.filter { it.name != f.name }).take(12)
        savePresets()
    }

    fun removePreset(name: String) {
        presets = presets.filter { it.name != name }
        savePresets()
    }
}

// ---------------------------------------------------------------- the live sound controls

object SoundFx {
    var reverb by mutableStateOf(0)
    var muffle by mutableStateOf(0f)
    var bass by mutableStateOf(0f)
    var speed by mutableStateOf(1f)
    var pitch by mutableStateOf(1f)
    var crickets by mutableStateOf(0f)
    var rain by mutableStateOf(0f)
    var fire by mutableStateOf(0f)
    var ocean by mutableStateOf(0f)
    var wind by mutableStateOf(0f)
    var lastName by mutableStateOf("")
    private var muffleOn = false
    private var bassOn = false

    fun load(f: FxSettings) {
        reverb = f.reverb
        muffle = f.muffle
        bass = f.bass
        speed = f.speed
        pitch = f.pitch
        crickets = f.crickets
        rain = f.rain
        fire = f.fire
        ocean = f.ocean
        wind = f.wind
        lastName = f.name
    }

    fun snapshot(name: String): FxSettings =
        FxSettings(name, reverb, muffle, bass, speed, pitch, crickets, rain, fire, ocean, wind)

    fun reset(s: AppState) {
        load(FxSettings(""))
        apply(s)
    }

    fun apply(s: AppState) {
        reapply()
        s.tempo = speed
        s.pitch = pitch
        s.applyTempoPitch()
        Ambient.set(crickets, rain, fire, ocean, wind)
    }

    // The audio effects are rebuilt whenever Android gives the player a new audio session, so this runs again then.
    fun reapply() {
        try {
            val e = EqHolder.eq
            if (e != null) {
                val n = e.numberOfBands.toInt()
                val rng = e.bandLevelRange
                val lo = rng[0].toInt()
                val hi = rng[1].toInt()
                val saved = Opts.eqLevels.split(",").mapNotNull { it.toIntOrNull() }
                if (muffle > 0.01f) {
                    for (b in 0 until n) {
                        val f = if (n > 1) b / (n - 1f) else 0f
                        val base = if (b < saved.size) saved[b] else 0
                        val cut = (muffle * (0.15f + 0.95f * f * f)).coerceIn(0f, 1f)
                        val lv = (base + (lo - base) * cut).toInt().coerceIn(lo, hi)
                        try { e.setBandLevel(b.toShort(), lv.toShort()) } catch (ex: Exception) { }
                    }
                    try { e.enabled = true } catch (ex: Exception) { }
                    muffleOn = true
                } else if (muffleOn) {
                    for (b in 0 until n) {
                        val base = if (b < saved.size) saved[b] else 0
                        try { e.setBandLevel(b.toShort(), base.coerceIn(lo, hi).toShort()) } catch (ex: Exception) { }
                    }
                    try { e.enabled = Opts.eqOn } catch (ex: Exception) { }
                    muffleOn = false
                }
            }
        } catch (ex: Exception) { }
        try {
            val b = EqHolder.bass
            if (b != null) {
                if (bass > 0.01f) {
                    b.setStrength((bass * 1000f).toInt().toShort())
                    try { b.enabled = true } catch (ex: Exception) { }
                } else if (bassOn) {
                    b.setStrength(Opts.bassBoost.toShort())
                    try { b.enabled = Opts.bassBoost > 0 } catch (ex: Exception) { }
                }
                bassOn = bass > 0.01f
            }
        } catch (ex: Exception) { }
        try {
            val r = EqHolder.reverb
            if (r != null) {
                if (reverb in 1..6) {
                    r.preset = reverb.toShort()
                    try { r.enabled = true } catch (ex: Exception) { }
                } else {
                    try { r.enabled = false } catch (ex: Exception) { }
                }
            }
        } catch (ex: Exception) { }
    }
}

// ---------------------------------------------------------------- ambient sounds made in code (no audio files)

object Ambient {
    @Volatile private var vC = 0f
    @Volatile private var vR = 0f
    @Volatile private var vF = 0f
    @Volatile private var vO = 0f
    @Volatile private var vW = 0f
    @Volatile private var paused = true
    @Volatile private var gen = 0
    @Volatile private var running = false

    fun set(c: Float, r: Float, f: Float, o: Float, w: Float) {
        vC = c
        vR = r
        vF = f
        vO = o
        vW = w
        refresh()
    }

    fun setPlaying(p: Boolean) {
        paused = !p
        refresh()
    }

    private fun any(): Boolean = vC > 0.01f || vR > 0.01f || vF > 0.01f || vO > 0.01f || vW > 0.01f

    @Synchronized
    private fun refresh() {
        if (any() && !paused) {
            if (!running) {
                running = true
                gen++
                val g = gen
                val t = Thread { loop(g) }
                t.isDaemon = true
                t.start()
            }
        } else {
            running = false
        }
    }

    private fun loop(g: Int) {
        val rate = 22050
        val minBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track: AudioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, 4096) * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            running = false
            return
        }
        try {
            track.play()
            val rnd = Random(3)
            val buf = ShortArray(1024)
            var n = 0L
            var lpR = 0f
            var lpO = 0f
            var lpW = 0f
            var brown = 0f
            var crack = 0f
            val cf = doubleArrayOf(4200.0, 4650.0, 5100.0)
            val per = doubleArrayOf(0.43, 0.57, 0.71)
            while (running && gen == g) {
                for (i in buf.indices) {
                    val sec = n.toDouble() / rate
                    n++
                    var mix = 0f
                    val nz = rnd.nextFloat() * 2f - 1f
                    if (vC > 0.01f) {
                        var cs = 0f
                        for (k in 0..2) {
                            val ph = sec % per[k]
                            if (ph < 0.105) {
                                val pp = ph % 0.035
                                if (pp < 0.018) {
                                    val env = sin(PI * pp / 0.018)
                                    cs += (sin(2.0 * PI * cf[k] * sec) * env).toFloat()
                                }
                            }
                        }
                        mix += cs * 0.2f * vC
                    }
                    if (vR > 0.01f) {
                        lpR += 0.25f * (nz - lpR)
                        mix += (nz - lpR) * 0.45f * vR
                    }
                    if (vF > 0.01f) {
                        brown = (brown + 0.02f * nz) * 0.995f
                        if (rnd.nextInt(3000) == 0) crack = 0.9f
                        crack *= 0.9985f
                        mix += (brown * 2.2f + nz * crack * 0.5f) * vF
                    }
                    if (vO > 0.01f) {
                        lpO += 0.015f * (nz - lpO)
                        val sw = 0.5f + 0.5f * sin(2.0 * PI * 0.11 * sec).toFloat()
                        mix += lpO * 5f * (0.25f + 0.75f * sw) * vO
                    }
                    if (vW > 0.01f) {
                        lpW += 0.03f * (nz - lpW)
                        val gst = 0.5f + 0.5f * sin(2.0 * PI * 0.07 * sec + 1.3).toFloat()
                        mix += lpW * 4f * (0.3f + 0.7f * gst) * vW
                    }
                    val v = (mix * 0.7f).coerceIn(-1f, 1f)
                    buf[i] = (v * 22000f).toInt().toShort()
                }
                track.write(buf, 0, buf.size)
            }
        } catch (e: Exception) {
        } finally {
            try { track.stop() } catch (e: Exception) { }
            try { track.release() } catch (e: Exception) { }
        }
    }
}

// ---------------------------------------------------------------- the AI that understands the sentence

data class AiResult(val fx: FxSettings, val note: String)

object SoundAi {
    private const val SYSTEM =
        "You control the sound effects of a music player. The user describes a place, a mood or a sound. " +
            "Reply with ONLY one JSON object and nothing else. Fields: " +
            "name (short title), " +
            "reverb (integer 0-6: 0 none, 1 small room, 2 room, 3 big room, 4 hall, 5 stadium or arena, 6 echo plate), " +
            "muffle (0-1, 1 = heard through a wall), bass (0-1), " +
            "speed (0.6-1.4, 1 = normal), pitch (0.7-1.3, 1 = normal), " +
            "crickets, rain, fire, ocean, wind (each 0-1 ambient sound volume, 0 = off), " +
            "note (one short friendly sentence about what you did). " +
            "Only change what the request implies and leave everything else neutral " +
            "(reverb 0, muffle 0, bass 0, speed 1, pitch 1, ambient 0)."

    suspend fun solve(prompt: String): AiResult {
        val key = StudioPrefs.apiKey.trim()
        if (key.isNotEmpty()) {
            try {
                return withContext(Dispatchers.IO) { askApi(prompt, key) }
            } catch (e: Exception) {
                return AiResult(
                    local(prompt),
                    "The AI could not answer (" + (e.message ?: "error").take(40) + "), so I used offline mode."
                )
            }
        }
        return AiResult(local(prompt), "Offline mode. Add an API key below to let the AI understand any sentence.")
    }

    private fun askApi(prompt: String, key: String): AiResult {
        val conn = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 12000
            conn.readTimeout = 25000
            conn.doOutput = true
            conn.setRequestProperty("content-type", "application/json")
            conn.setRequestProperty("x-api-key", key)
            conn.setRequestProperty("anthropic-version", "2023-06-01")
            val body = JSONObject()
                .put("model", StudioPrefs.model.trim().ifEmpty { "claude-haiku-5-5" })
                .put("max_tokens", 400)
                .put("system", SYSTEM)
                .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw Exception("HTTP " + code)
            val content = JSONObject(text).getJSONArray("content")
            var out = ""
            for (i in 0 until content.length()) {
                val b = content.getJSONObject(i)
                if (b.optString("type") == "text") out += b.optString("text")
            }
            val a = out.indexOf('{')
            val z = out.lastIndexOf('}')
            if (a < 0 || z <= a) throw Exception("bad answer")
            val o = JSONObject(out.substring(a, z + 1))
            val fx = fxFromJson(o)
            return AiResult(fx, o.optString("note", "Done.").ifBlank { "Done." })
        } finally {
            conn.disconnect()
        }
    }

    // works without internet: keywords for the most common wishes
    fun local(prompt: String): FxSettings {
        val q = prompt.lowercase()
        fun has(vararg w: String): Boolean = w.any { q.contains(it) }
        var reverb = 0
        var muffle = 0f
        var bass = 0f
        var speed = 1f
        var pitch = 1f
        var cr = 0f
        var rain = 0f
        var fire = 0f
        var ocean = 0f
        var wind = 0f
        if (has("stadium", "arena", "concert")) { reverb = 5; bass = 0.3f }
        if (has("hall", "church", "cathedral")) reverb = 4
        if (has("bathroom", "shower")) reverb = 2
        if (has("cave", "tunnel", "echo")) reverb = 6
        if (has("room") && reverb == 0) reverb = 1
        if (has("space", "float")) { reverb = 6; muffle = 0.2f }
        if (has("muffl", "through a wall", "neighbor", "neighbour")) muffle = 0.8f
        if (has("underwater", "under water")) { muffle = 0.9f; ocean = 0.25f }
        if (has("radio", "vintage")) muffle = maxOf(muffle, 0.5f)
        if (has("sad", "cry", "lonely", "melanchol", "depress")) {
            speed = 0.93f
            if (reverb == 0) reverb = 4
        }
        if (has("slow")) speed = 0.85f
        if (has("fast", "speed up", "nightcore")) { speed = 1.2f; pitch = 1.15f }
        if (has("chipmunk")) { speed = 1.15f; pitch = 1.3f }
        if (has("bass", "boom", "subwoofer", "car")) bass = 0.8f
        if (has("cricket", "night")) cr = 0.5f
        if (has("rain")) rain = 0.6f
        if (has("fire", "campfire", "fireplace", "cozy")) fire = 0.5f
        if (has("ocean", "sea", "beach", "wave")) ocean = maxOf(ocean, 0.5f)
        if (has("wind", "storm")) wind = 0.5f
        return FxSettings(prompt.trim().take(22).ifEmpty { "My sound" }, reverb, muffle, bass, speed, pitch, cr, rain, fire, ocean, wind)
    }
}
