package com.prismwave.app

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Settings of the 8D effect. "live" copies are read by the audio thread.
object EightD {
    private var p: SharedPreferences? = null
    var on by mutableStateOf(false)
    var speed by mutableStateOf(0.1f)
    @Volatile var liveOn = false
    @Volatile var liveSpeed = 0.1f

    fun init(ctx: Context) {
        if (p != null) return
        val sp = ctx.applicationContext.getSharedPreferences("prismwave_8d", Context.MODE_PRIVATE)
        p = sp
        on = sp.getBoolean("on", false)
        speed = sp.getFloat("speed", 0.1f).coerceIn(0.04f, 0.4f)
        liveOn = on
        liveSpeed = speed
    }

    fun turn(v: Boolean) {
        on = v
        liveOn = v
        save()
    }

    fun changeSpeed(v: Float) {
        speed = v
        liveSpeed = v
    }

    fun save() {
        p?.edit()?.putBoolean("on", on)?.putFloat("speed", speed)?.apply()
    }
}

// The sound slowly circles around your head: left and right pan, and the
// back half of the circle gets softer and duller. Works on stereo 16-bit audio.
@OptIn(UnstableApi::class)
class EightDProcessor : BaseAudioProcessor() {
    private var rate = 44100
    private var cosP = 1.0
    private var sinP = 0.0
    private var amt = 0f
    private var lp = 0f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount != 2) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        rate = inputAudioFormat.sampleRate
        return inputAudioFormat
    }

    override fun onFlush() {
        lp = 0f
    }

    private fun sat(v: Float): Short =
        (if (v > 32767f) 32767f else if (v < -32768f) -32768f else v).toInt().toShort()

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val out = replaceOutputBuffer(size).order(ByteOrder.nativeOrder())
        inputBuffer.order(ByteOrder.nativeOrder())
        val target = if (EightD.liveOn) 1f else 0f
        val w = 2.0 * PI * EightD.liveSpeed / rate
        val cw = cos(w)
        val sw = sin(w)
        var pos = inputBuffer.position()
        val end = inputBuffer.limit()
        while (pos + 3 < end) {
            val l = inputBuffer.getShort(pos).toInt()
            val r = inputBuffer.getShort(pos + 2).toInt()
            pos += 4
            amt += (target - amt) * 0.0004f
            if (target == 0f && amt < 0.002f) {
                amt = 0f
                out.putShort(l.toShort())
                out.putShort(r.toShort())
                continue
            }
            val nc = cosP * cw - sinP * sw
            sinP = sinP * cw + cosP * sw
            cosP = nc
            val m = (l + r) * 0.5f
            val side = sinP.toFloat()
            val back = if (cosP < 0.0) (-cosP).toFloat() else 0f
            lp += 0.25f * (m - lp)
            val k = back * 0.7f
            val s = (m * (1f - k) + lp * k) * (1f - 0.2f * back) * 1.2f
            val gl = sqrt((1f - side) * 0.5f)
            val gr = sqrt((1f + side) * 0.5f)
            val wl = s * gl
            val wr = s * gr
            out.putShort(sat(l * (1f - amt) + wl * amt))
            out.putShort(sat(r * (1f - amt) + wr * amt))
        }
        val n = sqrt(cosP * cosP + sinP * sinP)
        if (n > 0.0) {
            cosP /= n
            sinP /= n
        }
        inputBuffer.position(end)
        out.flip()
    }
}
