package com.prismwave.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File

data class UpdateInfo(val code: Int, val notes: String, val url: String, val size: Long)

data class NewsItem(val id: Int, val title: String, val message: String)

// Looks at the newest GitHub release and at news.json, and downloads + installs updates.
object Updater {
    var info by mutableStateOf<UpdateInfo?>(null)
    var news by mutableStateOf<NewsItem?>(null)
    var progress by mutableStateOf(-1f)
    var message by mutableStateOf("")
    var dismissed by mutableStateOf(false)
    var newsDismissed by mutableStateOf(false)
    private var checked = false
    private val http = OkHttpClient()

    private const val RELEASE_URL = "https://api.github.com/repos/flagsnerd41/PrismWave/releases/latest"
    private const val NEWS_URL = "https://raw.githubusercontent.com/flagsnerd41/PrismWave/main/news.json"

    fun currentCode(ctx: Context): Int = try {
        PackageInfoCompat.getLongVersionCode(ctx.packageManager.getPackageInfo(ctx.packageName, 0)).toInt()
    } catch (e: Exception) {
        0
    }

    private fun get(url: String): String {
        val req = Request.Builder().url(url).header("User-Agent", "PrismWave").header("Accept", "application/vnd.github+json").build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) throw Exception("HTTP " + r.code)
            return r.body?.string() ?: ""
        }
    }

    suspend fun check(ctx: Context) {
        if (checked) return
        checked = true
        withContext(Dispatchers.IO) {
            try {
                val o = JSONObject(get(RELEASE_URL))
                val tag = o.optString("tag_name")
                // build N from GitHub has version code N + 100 (see app/build.gradle.kts)
                val code = (tag.removePrefix("build-").toIntOrNull() ?: 0) + 100
                if (code > currentCode(ctx)) {
                    val assets = o.optJSONArray("assets")
                    var url = ""
                    var size = 0L
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val a = assets.getJSONObject(i)
                            if (a.optString("name").endsWith(".apk")) {
                                url = a.optString("browser_download_url")
                                size = a.optLong("size")
                                break
                            }
                        }
                    }
                    if (url.isNotEmpty()) info = UpdateInfo(code, o.optString("body").trim(), url, size)
                }
            } catch (e: Exception) {
            }
            try {
                val n = JSONObject(get(NEWS_URL))
                val id = n.optInt("id", 0)
                val seen = ctx.getSharedPreferences("prismwave", Context.MODE_PRIVATE).getInt("newsSeen", 0)
                if (id > seen && n.optString("message").isNotBlank()) {
                    news = NewsItem(id, n.optString("title", "News from the dev"), n.optString("message"))
                }
            } catch (e: Exception) {
            }
        }
    }

    suspend fun download(ctx: Context, i: UpdateInfo): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(ctx.cacheDir, "updates")
            dir.mkdirs()
            dir.listFiles()?.forEach { it.delete() }
            val f = File(dir, "PrismWave-" + i.code + ".apk")
            val req = Request.Builder().url(i.url).header("User-Agent", "PrismWave").build()
            http.newCall(req).execute().use { r ->
                if (!r.isSuccessful) throw Exception("HTTP " + r.code)
                val body = r.body ?: throw Exception("empty")
                val total = if (body.contentLength() > 0) body.contentLength() else i.size
                var done = 0L
                var lastUi = 0L
                body.byteStream().use { input ->
                    f.outputStream().use { out ->
                        val buf = ByteArray(32 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            val now = System.currentTimeMillis()
                            if (total > 0 && now - lastUi > 120) {
                                progress = (done.toFloat() / total).coerceIn(0f, 1f)
                                lastUi = now
                            }
                        }
                    }
                }
            }
            f
        } catch (e: Exception) {
            message = "Download failed, check your connection"
            null
        }
    }

    fun canInstall(ctx: Context): Boolean = ctx.packageManager.canRequestPackageInstalls()

    fun askPermission(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + ctx.packageName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun install(ctx: Context, f: File) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }
}

// Shows "Update available" or "News from the dev" on top of the app.
@Composable
fun UpdateHost(accent: Color) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        delay(4000)
        Updater.check(ctx)
    }
    val info = Updater.info
    val news = Updater.news
    val showUpdate = info != null && !Updater.dismissed
    if (showUpdate && info != null) {
        val busy = Updater.progress >= 0f
        PopupCard(
            accent = accent,
            title = "Update available",
            sub = "Build " + (info.code - 100),
            body = info.notes.ifBlank { "Improvements and fixes." },
            progress = Updater.progress,
            message = Updater.message,
            primary = if (busy) "Downloading..." else "Update",
            onPrimary = {
                if (!busy) {
                    if (!Updater.canInstall(ctx)) {
                        Updater.message = "Allow installs for PrismWave, then tap Update again"
                        Updater.askPermission(ctx)
                    } else {
                        Updater.message = ""
                        Updater.progress = 0f
                        scope.launch {
                            val f = Updater.download(ctx, info)
                            Updater.progress = -1f
                            if (f != null) Updater.install(ctx, f)
                        }
                    }
                }
            },
            secondary = if (busy) null else "Later",
            onSecondary = { Updater.dismissed = true }
        )
    } else if (news != null && !Updater.newsDismissed) {
        PopupCard(
            accent = accent,
            title = news.title,
            sub = "",
            body = news.message,
            progress = -1f,
            message = "",
            primary = "Got it",
            onPrimary = {
                ctx.getSharedPreferences("prismwave", Context.MODE_PRIVATE).edit().putInt("newsSeen", news.id).apply()
                Updater.newsDismissed = true
            },
            secondary = null,
            onSecondary = {}
        )
    }
}

@Composable
private fun PopupCard(
    accent: Color,
    title: String,
    sub: String,
    body: String,
    progress: Float,
    message: String,
    primary: String,
    onPrimary: () -> Unit,
    secondary: String?,
    onSecondary: () -> Unit
) {
    BackHandler { if (secondary != null) onSecondary() else if (progress < 0f) onPrimary() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(26.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF1C1B24))
                .padding(22.dp)
        ) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (sub.isNotEmpty()) Text(sub, color = accent, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                Text(body, color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp)
            }
            if (progress >= 0f) {
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(progress = { progress }, color = accent, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text((progress * 100).toInt().toString() + "%", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
            if (message.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(message, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (secondary != null) {
                    TextButton(onClick = onSecondary) { Text(secondary, color = Color.White.copy(alpha = 0.7f)) }
                    Spacer(Modifier.width(8.dp))
                }
                Button(
                    onClick = onPrimary,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                ) { Text(primary, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
