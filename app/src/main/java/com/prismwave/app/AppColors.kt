package com.prismwave.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

// Two ways to color the app:
// "Color palette": the accent color you pick stays.
// "Follow music theme": the app takes the colors of the cover that is playing.
object AppColors {
    private var p: SharedPreferences? = null
    var palette by mutableStateOf(Gold)
    var ver by mutableStateOf(0)

    fun init(ctx: Context) {
        if (p != null) return
        val sp = ctx.applicationContext.getSharedPreferences("prismwave_extras", Context.MODE_PRIVATE)
        p = sp
        palette = Color(sp.getInt("paletteAccent", Gold.toArgb()))
    }

    // picking a color means you want the palette mode
    fun pick(c: Color) {
        palette = c
        p?.edit()?.putInt("paletteAccent", c.toArgb())?.apply()
        Opts.livingBg = false
        Opts.save()
        ver++
    }

    fun setFollow(follow: Boolean) {
        Opts.livingBg = follow
        Opts.save()
        ver++
    }
}
