package com.prismwave.app

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import kotlin.math.PI
import kotlin.math.sin

// Lives in the same process as the playback service, so the app can control it directly.
object EqHolder {
    var eq: Equalizer? = null
    var bass: BassBoost? = null

    fun attach(id: Int) {
        if (id == 0) return
        try { eq?.release() } catch (e: Exception) { }
        try { bass?.release() } catch (e: Exception) { }
        eq = try { Equalizer(0, id) } catch (e: Exception) { null }
        bass = try { BassBoost(0, id) } catch (e: Exception) { null }
        val e = eq
        if (e != null) {
            val saved = Opts.eqLevels.split(",").mapNotNull { it.toIntOrNull() }
            for (b in 0 until e.numberOfBands.toInt()) {
                if (b < saved.size) {
                    try { e.setBandLevel(b.toShort(), saved[b].toShort()) } catch (ex: Exception) { }
                }
            }
            try { e.enabled = Opts.eqOn } catch (ex: Exception) { }
        }
        bass?.let {
            try {
                it.setStrength(Opts.bassBoost.toShort())
                it.enabled = Opts.bassBoost > 0
            } catch (ex: Exception) { }
        }
    }

    fun setEnabled(on: Boolean) {
        try { eq?.enabled = on } catch (e: Exception) { }
    }

    fun setBand(b: Int, level: Int) {
        try { eq?.setBandLevel(b.toShort(), level.toShort()) } catch (e: Exception) { }
    }

    fun saveLevels() {
        val e = eq ?: return
        try {
            Opts.eqLevels = (0 until e.numberOfBands.toInt()).joinToString(",") { e.getBandLevel(it.toShort()).toString() }
            Opts.save()
        } catch (ex: Exception) { }
    }

    fun setBass(v: Int) {
        Opts.bassBoost = v
        try {
            bass?.setStrength(v.toShort())
            bass?.enabled = v > 0
        } catch (e: Exception) { }
    }

    fun preset(name: String, n: Int, lo: Int, hi: Int): List<Float> = (0 until n).map { b ->
        val f = if (n > 1) b / (n - 1f) else 0f
        val v = when (name) {
            "Bass" -> hi * 0.75f * (1f - f) * (1f - f)
            "Treble" -> hi * 0.75f * f * f
            "Vocal" -> hi * 0.6f * sin(PI.toFloat() * f)
            else -> 0f
        }
        v.coerceIn(lo.toFloat(), hi.toFloat())
    }
}
