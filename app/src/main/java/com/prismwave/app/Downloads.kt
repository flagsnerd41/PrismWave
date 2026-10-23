package com.prismwave.app

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/** Saves songs on the phone so they play without internet (Library > Offline). */
object DownloadStore {
    private val http = OkHttpClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private const val CHUNK = 1_000_000L
    private var dir: File? = null

    val songs = mutableStateListOf<Song>()

    /** song id -> 0..1 while a download is queued or running */
    val progress = mutableStateMapOf<String, Float>()

    fun ensure(ctx: Context) {
        if (dir != null) return
        val d = File(ctx.applicationContext.filesDir, "downloads")
        d.mkdirs()
        dir = d
        try {
            val f = File(d, "index.json")
            if (f.exists()) {
                val arr = JSONArray(f.readText())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val sg = Song(
                        o.getString("id"), o.getString("title"), o.getString("artist"),
                        o.optInt("seconds"), o.getString("url"), o.optString("thumb").ifEmpty { null }
                    )
                    if (fileFor(sg).exists()) songs.add(sg)
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun hash(id: String): String =
        MessageDigest.getInstance("SHA-1").digest(id.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun fileFor(song: Song): File = File(dir, hash(song.id) + ".m4a")

    fun isDownloaded(song: Song): Boolean = songs.any { it.id == song.id }

    fun localUri(song: Song): String? {
        val f = fileFor(song)
        return if (dir != null && isDownloaded(song) && f.exists()) Uri.fromFile(f).toString() else null
    }

    private fun saveIndex() {
        val snap = songs.toList()
        val d = dir ?: return
        scope.launch(Dispatchers.IO) {
            val arr = JSONArray()
            snap.forEach {
                arr.put(
                    JSONObject().put("id", it.id).put("title", it.title).put("artist", it.artist)
                        .put("seconds", it.seconds).put("url", it.url).put("thumb", it.thumb ?: "")
                )
            }
            File(d, "index.json").writeText(arr.toString())
        }
    }

    private fun toast(ctx: Context, t: String) =
        Toast.makeText(ctx.applicationContext, t, Toast.LENGTH_SHORT).show()

    private suspend fun downloadOne(song: Song): Boolean {
        if (isDownloaded(song)) {
            progress.remove(song.id)
            return true
        }
        progress[song.id] = 0f
        val finalFile = fileFor(song)
        val tmp = File(dir, hash(song.id) + ".part")
        return try {
            val url = Engine.streamUrl(song.url)
            withContext(Dispatchers.IO) {
                var start = 0L
                var total = -1L
                var done = false
                tmp.outputStream().use { out ->
                    while (!done) {
                        val req = Request.Builder()
                            .url(url)
                            .header("User-Agent", YtAccount.UA)
                            .header("Range", "bytes=" + start + "-" + (start + CHUNK - 1))
                            .build()
                        http.newCall(req).execute().use { r ->
                            if (r.code == 200) {
                                if (start > 0) throw IOException("bad range")
                                val b = r.body ?: throw IOException("empty")
                                total = b.contentLength()
                                val buf = ByteArray(64 * 1024)
                                var read = 0L
                                b.byteStream().use { inp ->
                                    while (true) {
                                        val n = inp.read(buf)
                                        if (n < 0) break
                                        out.write(buf, 0, n)
                                        read += n
                                        if (total > 0) progress[song.id] = (read.toFloat() / total).coerceIn(0f, 1f)
                                    }
                                }
                                done = true
                                return@use
                            }
                            if (r.code != 206) throw IOException("HTTP " + r.code)
                            val cr = r.header("Content-Range") ?: ""
                            total = cr.substringAfter("/", "").toLongOrNull() ?: throw IOException("no length")
                            val bytes = (r.body ?: throw IOException("empty")).bytes()
                            if (bytes.isEmpty()) throw IOException("empty chunk")
                            out.write(bytes)
                            start += bytes.size
                            progress[song.id] = (start.toFloat() / total).coerceIn(0f, 1f)
                            if (start >= total) done = true
                        }
                    }
                }
            }
            tmp.renameTo(finalFile)
            songs.add(song)
            saveIndex()
            progress.remove(song.id)
            true
        } catch (e: Exception) {
            tmp.delete()
            progress.remove(song.id)
            false
        }
    }

    fun start(ctx: Context, song: Song) {
        ensure(ctx)
        if (isDownloaded(song) || progress.containsKey(song.id)) return
        scope.launch {
            if (downloadOne(song)) toast(ctx, "Downloaded") else toast(ctx, "Download failed: " + song.title.take(24))
        }
    }

    fun startAll(ctx: Context, list: List<Song>) {
        ensure(ctx)
        val todo = list.filter { !isDownloaded(it) && !progress.containsKey(it.id) }
        if (todo.isEmpty()) return
        todo.forEach { progress[it.id] = 0f }
        toast(ctx, "Downloading " + todo.size + " songs")
        scope.launch {
            var failed = 0
            for (sg in todo) {
                if (!downloadOne(sg)) failed++
            }
            toast(ctx, if (failed == 0) "All downloaded" else "Done, " + failed + " failed")
        }
    }

    fun remove(ctx: Context, song: Song) {
        ensure(ctx)
        fileFor(song).delete()
        songs.removeAll { it.id == song.id }
        saveIndex()
    }
}
