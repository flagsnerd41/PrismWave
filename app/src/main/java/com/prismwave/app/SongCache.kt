package com.prismwave.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Simple, reliable song cache: each song is saved as a real file and played from there next time.
object SongCache {
    private val busy = HashSet<String>()

    private fun dir(ctx: Context): File = File(ctx.cacheDir, "songs").apply { mkdirs() }
    private fun name(id: String): String = id.replace(Regex("[^A-Za-z0-9_-]"), "_")
    private fun file(ctx: Context, id: String): File = File(dir(ctx), name(id) + ".audio")

    fun has(ctx: Context, id: String): Boolean {
        val f = file(ctx, id)
        return f.exists() && f.length() > 50_000
    }

    fun uri(ctx: Context, id: String): String? {
        if (!has(ctx, id)) return null
        val f = file(ctx, id)
        f.setLastModified(System.currentTimeMillis())
        return Uri.fromFile(f).toString()
    }

    fun remove(ctx: Context, id: String) {
        file(ctx, id).delete()
    }

    fun keys(ctx: Context): List<String> {
        val list = dir(ctx).listFiles { f -> f.name.endsWith(".audio") } ?: return emptyList()
        return list.sortedByDescending { it.lastModified() }.map { it.name.removeSuffix(".audio") }
    }

    fun usedMb(ctx: Context, refresh: Int = 0): Long {
        val total = dir(ctx).listFiles()?.sumOf { it.length() } ?: 0L
        return total / (1024L * 1024L)
    }

    fun clear(ctx: Context) {
        dir(ctx).listFiles()?.forEach { it.delete() }
    }

    private fun trim(ctx: Context) {
        val limit = Opts.cacheMb * 1024L * 1024L
        val files = dir(ctx).listFiles { f -> f.name.endsWith(".audio") }?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (f in files) {
            if (total <= limit) break
            total -= f.length()
            f.delete()
        }
    }

    private fun download(ctx: Context, id: String, url: String): Boolean {
        val tmp = File(dir(ctx), name(id) + ".part")
        try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 10000
            c.readTimeout = 20000
            c.setRequestProperty("Range", "bytes=0-")
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
            if (c.responseCode !in 200..299) {
                c.disconnect()
                return false
            }
            c.inputStream.use { inp -> tmp.outputStream().use { out -> inp.copyTo(out, 64 * 1024) } }
            c.disconnect()
            if (tmp.length() < 50_000) {
                tmp.delete()
                return false
            }
            val dest = file(ctx, id)
            dest.delete()
            if (!tmp.renameTo(dest)) {
                tmp.delete()
                return false
            }
            return true
        } catch (e: Exception) {
            tmp.delete()
            return false
        }
    }

    suspend fun fetch(ctx: Context, id: String, url: String): Boolean = withContext(Dispatchers.IO) {
        if (has(ctx, id)) return@withContext true
        val mine = synchronized(busy) { busy.add(id) }
        if (!mine) return@withContext false
        try {
            val ok = download(ctx, id, url)
            if (ok) trim(ctx)
            ok
        } finally {
            synchronized(busy) { busy.remove(id) }
        }
    }
}
