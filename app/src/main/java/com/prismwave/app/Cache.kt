package com.prismwave.app

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

// Songs you stream are kept on the phone so they replay instantly and work offline.
@OptIn(UnstableApi::class)
object AudioCache {
    private var cache: SimpleCache? = null

    @Synchronized
    fun get(ctx: Context): SimpleCache {
        cache?.let { return it }
        val app = ctx.applicationContext
        val mb = app.getSharedPreferences("prismwave_opts", Context.MODE_PRIVATE).getInt("cacheMb", 512)
        val c = SimpleCache(
            File(app.cacheDir, "audio"),
            LeastRecentlyUsedCacheEvictor(mb * 1024L * 1024L),
            StandaloneDatabaseProvider(app)
        )
        cache = c
        return c
    }

    fun keys(ctx: Context): List<String> {
        val c = get(ctx)
        return c.keys.filter { c.getCachedBytes(it, 0, C.LENGTH_UNSET.toLong()) > 0 }
    }

    fun isFull(ctx: Context, key: String): Boolean {
        return try {
            val c = get(ctx)
            val len = ContentMetadata.getContentLength(c.getContentMetadata(key))
            len > 0 && c.isCached(key, 0, len)
        } catch (e: Exception) {
            false
        }
    }

    fun remove(ctx: Context, key: String) {
        try {
            get(ctx).removeResource(key)
        } catch (e: Exception) {
        }
    }

    fun clear(ctx: Context) {
        try {
            val c = get(ctx)
            c.keys.toList().forEach { c.removeResource(it) }
        } catch (e: Exception) {
        }
    }

    fun usedMb(ctx: Context, refresh: Int = 0): Long = get(ctx).cacheSpace / (1024L * 1024L)
}
