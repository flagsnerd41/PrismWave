package com.prismwave.app

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

// more options for the Behavior page
object BehaviorPrefs {
    var keepAwake by mutableStateOf(false)
    var shake by mutableStateOf(false)
    var shakeLevel by mutableStateOf(1)
    var autoRefresh by mutableStateOf(true)
    var buzz by mutableStateOf(false)
    private var loaded = false

    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        val p = ctx.applicationContext.getSharedPreferences("prismwave", 0)
        keepAwake = p.getBoolean("keepAwake", false)
        shake = p.getBoolean("shakeSkip", false)
        shakeLevel = p.getInt("shakeLevel", 1)
        autoRefresh = p.getBoolean("autoRefresh", true)
        buzz = p.getBoolean("buzzSong", false)
    }

    fun save(ctx: Context) {
        ctx.applicationContext.getSharedPreferences("prismwave", 0).edit()
            .putBoolean("keepAwake", keepAwake)
            .putBoolean("shakeSkip", shake)
            .putInt("shakeLevel", shakeLevel)
            .putBoolean("autoRefresh", autoRefresh)
            .putBoolean("buzzSong", buzz)
            .apply()
    }
}

// the things those options do
@Composable
fun BehaviorEffects(s: AppState) {
    val ctx = LocalContext.current
    val view = LocalView.current
    remember { BehaviorPrefs.init(ctx); 0 }

    // keep the screen on while a song plays
    val awake = BehaviorPrefs.keepAwake && s.playing
    DisposableEffect(awake) {
        view.keepScreenOn = awake
        onDispose { view.keepScreenOn = false }
    }

    // shake the phone to skip to the next song
    val shakeOn = BehaviorPrefs.shake
    val level = BehaviorPrefs.shakeLevel
    DisposableEffect(shakeOn, level) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val acc = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!shakeOn || sm == null || acc == null) {
            onDispose { }
        } else {
            val limit = when (level) {
                0 -> 15f
                2 -> 24f
                else -> 19f
            }
            var last = 0L
            val l = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) {
                    val x = e.values[0]
                    val y = e.values[1]
                    val z = e.values[2]
                    val g = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    val now = System.currentTimeMillis()
                    if (g > limit && now - last > 1200L) {
                        last = now
                        if (s.current != null) s.next()
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sm.unregisterListener(l) }
        }
    }

    // a small buzz when a new song starts
    val first = remember { BooleanArray(1) { true } }
    LaunchedEffect(s.current?.id) {
        if (first[0]) {
            first[0] = false
        } else if (BehaviorPrefs.buzz && s.current != null) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        }
    }
}
