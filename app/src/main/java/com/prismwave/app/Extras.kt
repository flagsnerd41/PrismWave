package com.prismwave.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

// hides the phone's status bar while the full player is open (swipe from the top to show it)
@Composable
fun StatusBarHider(hide: Boolean) {
    val view = LocalView.current
    DisposableEffect(hide) {
        var c: Context = view.context
        var act: Activity? = null
        while (c is ContextWrapper) {
            if (c is Activity) {
                act = c
                break
            }
            c = c.baseContext
        }
        val win = act?.window
        val ctl = if (win != null) WindowCompat.getInsetsController(win, view) else null
        if (ctl != null) {
            if (hide) {
                ctl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                ctl.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                ctl.show(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose { ctl?.show(WindowInsetsCompat.Type.statusBars()) }
    }
}

// opens Settings straight on a page, for example "changelog"
object SettingsNav {
    var start: String? = null

    fun take(): String? {
        val v = start
        start = null
        return v
    }
}

// the "What's new" button in the Home header, with a dot until it is opened
object WhatsNew {
    const val ID = "added-ui-lyrics-video"
    private var p: SharedPreferences? = null
    var seen by mutableStateOf(true)

    fun init(ctx: Context) {
        if (p != null) return
        val sp = ctx.applicationContext.getSharedPreferences("prismwave_extras", Context.MODE_PRIVATE)
        p = sp
        seen = sp.getString("seenNews", "") == ID
    }

    fun markSeen() {
        seen = true
        p?.edit()?.putString("seenNews", ID)?.apply()
    }
}

@Composable
fun WhatsNewButton(s: AppState) {
    WhatsNew.init(LocalContext.current)
    Box {
        RoundBtn(Icons.Rounded.NewReleases, 0.9f, {
            WhatsNew.markSeen()
            SettingsNav.start = "changelog"
            s.screen = Screen.Settings
        }, 46.dp)
        if (!WhatsNew.seen) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5A6E))
            )
        }
    }
}

// a playlist with 4 or more songs shows a 2 by 2 collage of its covers
@Composable
fun PlaylistArt(p: Playlist, first: Song?, size: Dp, radius: Dp) {
    val covers = p.songs.distinctBy { it.thumb ?: it.id }.take(4)
    if (covers.size >= 4) {
        Column(Modifier.size(size).clip(RoundedCornerShape(radius))) {
            Row(Modifier.weight(1f)) {
                Cover(covers[0], Modifier.weight(1f).fillMaxSize(), 0.dp, 14.sp, quality = 300)
                Cover(covers[1], Modifier.weight(1f).fillMaxSize(), 0.dp, 14.sp, quality = 300)
            }
            Row(Modifier.weight(1f)) {
                Cover(covers[2], Modifier.weight(1f).fillMaxSize(), 0.dp, 14.sp, quality = 300)
                Cover(covers[3], Modifier.weight(1f).fillMaxSize(), 0.dp, 14.sp, quality = 300)
            }
        }
    } else if (first != null) {
        Cover(first, Modifier.size(size), radius, 44.sp)
    } else {
        Box(Modifier.size(size).clip(RoundedCornerShape(radius)).background(CardBg2))
    }
}

// recent searches, kept on the phone
object SearchHistory {
    private var p: SharedPreferences? = null
    val items = mutableStateListOf<String>()

    fun init(ctx: Context) {
        if (p != null) return
        val sp = ctx.applicationContext.getSharedPreferences("prismwave_search", Context.MODE_PRIVATE)
        p = sp
        try {
            val arr = JSONArray(sp.getString("recent", "[]"))
            for (i in 0 until arr.length()) items.add(arr.getString(i))
        } catch (e: Exception) {
        }
    }

    private fun save() {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        p?.edit()?.putString("recent", arr.toString())?.apply()
    }

    fun add(q: String) {
        val t = q.trim()
        if (t.length < 2) return
        items.remove(t)
        items.add(0, t)
        while (items.size > 12) items.removeAt(items.size - 1)
        save()
    }

    fun remove(q: String) {
        items.remove(q)
        save()
    }

    fun clear() {
        items.clear()
        save()
    }
}

// live suggestions while you type
object Suggest {
    suspend fun fetch(q: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=" +
                URLEncoder.encode(q, "UTF-8")
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 4000
            c.readTimeout = 4000
            c.setRequestProperty("User-Agent", "Mozilla/5.0")
            try {
                if (c.responseCode != 200) {
                    emptyList<String>()
                } else {
                    val arr = JSONArray(c.inputStream.bufferedReader().readText())
                    val l = arr.getJSONArray(1)
                    (0 until l.length()).map { l.getString(it) }
                }
            } finally {
                c.disconnect()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// the blurred video behind the player is optional, because blurring a video every frame can lag
object VideoPrefs {
    private var p: SharedPreferences? = null
    var liveBackdrop by mutableStateOf(false)

    fun init(ctx: Context) {
        if (p != null) return
        val sp = ctx.applicationContext.getSharedPreferences("prismwave_video", Context.MODE_PRIVATE)
        p = sp
        liveBackdrop = sp.getBoolean("live", false)
    }

    fun set(v: Boolean) {
        liveBackdrop = v
        p?.edit()?.putBoolean("live", v)?.apply()
    }
}
