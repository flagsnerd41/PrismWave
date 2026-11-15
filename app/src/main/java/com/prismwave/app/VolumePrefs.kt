package com.prismwave.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Show or hide the volume slider: under the controls in the player, and in the three dots menu.
object VolPrefs {
    var show by mutableStateOf(true)
    var showMenu by mutableStateOf(true)
    private var p: SharedPreferences? = null

    fun init(ctx: Context) {
        if (p != null) return
        val sp = ctx.applicationContext.getSharedPreferences("prismwave_volume", Context.MODE_PRIVATE)
        p = sp
        show = sp.getBoolean("show", true)
        showMenu = sp.getBoolean("showMenu", true)
    }

    fun save() {
        p?.edit()?.putBoolean("show", show)?.putBoolean("showMenu", showMenu)?.apply()
    }
}
