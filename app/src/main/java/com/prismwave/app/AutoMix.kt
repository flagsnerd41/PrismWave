package com.prismwave.app

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteOrder
import kotlin.math.sqrt

// Smart mix point: listens to the last minute of a cached song, finds where the
// quiet outro begins, and tells the player how many seconds before the end the
// transition should start. Songs that are not cached yet use the normal settings.
object AutoMix {
    private var p: SharedPreferences? = null
    var on by mutableStateOf(false)
    private val lead = HashMap<String, Float>()
    private val busy = HashSet<String>()

    fun init(ctx: Context) {
        if (p != null) return
        val sp = ctx.applicationContext.getSharedPreferences("prismwave_automix", Context.MODE_PRIVATE)
        p = sp
        on = sp.getBoolean("on", false)
    }

    fun turn(v: Boolean) {
        on = v
        p?.edit()?.putBoolean("on", v)?.apply()
    }

    fun leadFor(id: String): Float? = synchronized(lead) { lead[id] }

    fun smart(id: String?): Boolean = on && id != null && leadFor(id) != null

    suspend fun analyze(ctx: Context, id: String) {
        init(ctx)
        if (!on || leadFor(id) != null) return
        val mine = synchronized(busy) { busy.add(id) }
        if (!mine) return
        try {
            val f = File(ctx.cacheDir, "songs/" + id.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".audio")
            val v = withContext(Dispatchers.Default) { measure(f) }
            if (v != null) synchronized(lead) { lead[id] = v }
        } catch (e: Exception) {
        } finally {
            synchronized(busy) { busy.remove(id) }
        }
    }

    private fun median(l: List<Float>): Float {
        if (l.isEmpty()) return 0f
        val s = l.sorted()
        return s[s.size / 2]
    }

    // seconds before the end where the outro starts, or null if it cannot be measured
    private fun measure(f: File): Float? {
        if (!f.exists()) return null
        val ex = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            ex.setDataSource(f.absolutePath)
            var idx = -1
            var fmt: MediaFormat? = null
            for (i in 0 until ex.trackCount) {
                val tf = ex.getTrackFormat(i)
                if ((tf.getString(MediaFormat.KEY_MIME) ?: "").startsWith("audio/")) {
                    idx = i
                    fmt = tf
                    break
                }
            }
            if (idx < 0 || fmt == null) return null
            if (!fmt.containsKey(MediaFormat.KEY_DURATION)) return null
            val durS = fmt.getLong(MediaFormat.KEY_DURATION) / 1_000_000f
            if (durS < 40f) return null
            ex.selectTrack(idx)
            ex.seekTo(((durS - 70f).coerceAtLeast(0f) * 1_000_000f).toLong(), MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: return null
            val c = MediaCodec.createDecoderByType(mime)
            codec = c
            c.configure(fmt, null, null, 0)
            c.start()
            var rate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var ch = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var winLen = rate * ch / 2
            val info = MediaCodec.BufferInfo()
            val energies = ArrayList<Float>()
            var acc = 0.0
            var cnt = 0
            var inDone = false
            var outDone = false
            val deadline = System.currentTimeMillis() + 20_000
            while (!outDone && System.currentTimeMillis() < deadline) {
                if (!inDone) {
                    val ii = c.dequeueInputBuffer(5000)
                    if (ii >= 0) {
                        val buf = c.getInputBuffer(ii)
                        val n = if (buf != null) ex.readSampleData(buf, 0) else -1
                        if (n < 0) {
                            c.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inDone = true
                        } else {
                            c.queueInputBuffer(ii, 0, n, ex.sampleTime, 0)
                            ex.advance()
                        }
                    }
                }
                val oi = c.dequeueOutputBuffer(info, 5000)
                if (oi >= 0) {
                    val ob = c.getOutputBuffer(oi)
                    if (ob != null && info.size > 0) {
                        ob.position(info.offset)
                        ob.limit(info.offset + info.size)
                        val sb = ob.order(ByteOrder.nativeOrder()).asShortBuffer()
                        while (sb.hasRemaining()) {
                            val v = sb.get() / 32768.0
                            acc += v * v
                            cnt++
                            if (cnt >= winLen) {
                                energies.add(sqrt(acc / cnt).toFloat())
                                acc = 0.0
                                cnt = 0
                            }
                        }
                    }
                    c.releaseOutputBuffer(oi, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outDone = true
                } else if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val of = c.outputFormat
                    rate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    ch = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    winLen = rate * ch / 2
                }
            }
            // every window is half a second; the last one ends at the end of the song
            val n = energies.size
            if (n < 24) return null
            val refEnd = (n - 50).coerceAtLeast(n / 2)
            val ref = median(energies.subList(0, refEnd))
            if (ref < 0.003f) return null
            var j = n - 1
            while (j >= 0 && energies[j] < ref * 0.7f) j--
            if (j < 0) return null
            val outro = (n - 1 - j) * 0.5f
            return if (outro < 3f) 4f else outro.coerceAtMost(15f)
        } catch (e: Exception) {
            return null
        } finally {
            try { codec?.stop() } catch (e: Exception) { }
            try { codec?.release() } catch (e: Exception) { }
            try { ex.release() } catch (e: Exception) { }
        }
    }
}
