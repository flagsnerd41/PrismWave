package com.prismwave.app

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp

// Muffle and volume ramps that run inside the audio itself (not in Android's equalizer,
// which many phones ignore). The app sets a ramp, the processor below plays it.
object AudioShape {
    @Volatile var muffleFrom = 0f
    @Volatile var muffleTo = 0f
    @Volatile var muffleMs = 1L
    @Volatile var muffleSeq = 0
    @Volatile var gainFrom = 1f
    @Volatile var gainTo = 1f
    @Volatile var gainMs = 1L
    @Volatile var gainSeq = 0

    fun rampMuffle(a: Float, b: Float, ms: Long) {
        muffleFrom = a
        muffleTo = b
        muffleMs = if (ms < 1L) 1L else ms
        muffleSeq++
    }

    fun rampGain(a: Float, b: Float, ms: Long) {
        gainFrom = a
        gainTo = b
        gainMs = if (ms < 1L) 1L else ms
        gainSeq++
    }

    fun reset() {
        rampMuffle(0f, 0f, 1L)
        rampGain(1f, 1f, 1L)
    }
}

@OptIn(UnstableApi::class)
class AudioShapeProcessor : BaseAudioProcessor() {
    private var rate = 44100
    private var ch = 2
    private var y1 = FloatArray(2)
    private var y2 = FloatArray(2)
    private var mSeen = -1
    private var gSeen = -1
    private var mCur = 0f
    private var mTgt = 0f
    private var mStep = 0f
    private var mRem = 0L
    private var gCur = 1f
    private var gTgt = 1f
    private var gStep = 0f
    private var gRem = 0L
    private var coef = 1f
    private var coefAge = 0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT ||
            inputAudioFormat.channelCount < 1 || inputAudioFormat.channelCount > 8
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        rate = inputAudioFormat.sampleRate
        ch = inputAudioFormat.channelCount
        y1 = FloatArray(ch)
        y2 = FloatArray(ch)
        return inputAudioFormat
    }

    private fun coefFor(m: Float): Float {
        // the sound gets duller smoothly over the whole length, from 20000 Hz down to about 400 Hz
        val fc = (20000.0 * Math.pow(0.02, m.toDouble())).toFloat().coerceIn(300f, 20000f)
        val a = 1f - exp(-2.0 * PI * fc / rate).toFloat()
        return if (a < 0.001f) 0.001f else if (a > 1f) 1f else a
    }

    private fun sat(v: Float): Short =
        (if (v > 32767f) 32767f else if (v < -32768f) -32768f else v).toInt().toShort()

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val out = replaceOutputBuffer(size).order(ByteOrder.nativeOrder())
        inputBuffer.order(ByteOrder.nativeOrder())

        val ms = AudioShape.muffleSeq
        if (ms != mSeen) {
            mSeen = ms
            mCur = AudioShape.muffleFrom
            mTgt = AudioShape.muffleTo
            mRem = maxOf(1L, AudioShape.muffleMs * rate / 1000L)
            mStep = (mTgt - mCur) / mRem
        }
        val gs = AudioShape.gainSeq
        if (gs != gSeen) {
            gSeen = gs
            gCur = AudioShape.gainFrom
            gTgt = AudioShape.gainTo
            gRem = maxOf(1L, AudioShape.gainMs * rate / 1000L)
            gStep = (gTgt - gCur) / gRem
        }

        var pos = inputBuffer.position()
        val end = inputBuffer.limit()
        val frameBytes = 2 * ch
        while (pos + frameBytes <= end) {
            if (mRem > 0L) {
                mCur += mStep
                mRem--
                if (mRem == 0L) mCur = mTgt
            }
            if (gRem > 0L) {
                gCur += gStep
                gRem--
                if (gRem == 0L) gCur = gTgt
            }
            if (coefAge <= 0) {
                coefAge = 64
                coef = coefFor(mCur)
            }
            coefAge--
            val muffling = mCur > 0.004f
            for (c in 0 until ch) {
                var x = inputBuffer.getShort(pos).toFloat()
                pos += 2
                if (muffling) {
                    y1[c] += coef * (x - y1[c])
                    y2[c] += coef * (y1[c] - y2[c])
                    x = y2[c]
                } else {
                    y1[c] = x
                    y2[c] = x
                }
                out.putShort(sat(x * gCur))
            }
        }
        inputBuffer.position(end)
        out.flip()
    }
}
