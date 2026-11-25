package com.prismwave.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// How blurry the backdrop of the Immersive player is. 0 = sharp.
object ImmersivePrefs {
    var blur by mutableStateOf(16f)
    private var loaded = false

    fun init(ctx: Context) {
        if (loaded) return
        loaded = true
        blur = ctx.getSharedPreferences("prismwave", 0).getFloat("immersiveBlur", 16f).coerceIn(0f, 60f)
    }

    fun save(ctx: Context) {
        ctx.getSharedPreferences("prismwave", 0).edit().putFloat("immersiveBlur", blur).apply()
    }
}

// Music found on this phone. Songs under 1 minute are ignored (ringtones, voice notes, sounds).
object LocalFiles {
    val songs = mutableStateListOf<Song>()
    var scanning by mutableStateOf(false)
    var scanned by mutableStateOf(false)

    fun permission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    fun allowed(ctx: Context): Boolean =
        ctx.checkSelfPermission(permission()) == PackageManager.PERMISSION_GRANTED

    private fun label(mime: String?): String {
        val m = (mime ?: "").lowercase()
        return when {
            m.contains("flac") -> "FLAC"
            m.contains("mpeg") || m.contains("mp3") -> "MP3"
            m.contains("mp4") || m.contains("m4a") || m.contains("aac") -> "AAC"
            m.contains("ogg") || m.contains("opus") -> "Ogg"
            m.contains("wav") -> "WAV"
            else -> "Audio"
        }
    }

    suspend fun scan(ctx: Context) {
        if (scanning || !allowed(ctx)) return
        scanning = true
        try {
            val found = withContext(Dispatchers.IO) {
                val out = ArrayList<Song>()
                val tags = HashMap<String, String>()
                val proj = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.MIME_TYPE
                )
                val sel = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " + MediaStore.Audio.Media.DURATION + " >= 60000"
                ctx.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, sel, null,
                    MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC"
                )?.use { c ->
                    val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val iMime = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                    while (c.moveToNext()) {
                        val id = c.getLong(iId)
                        val uri = android.content.ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()
                        val artist = c.getString(iArtist)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown artist"
                        out.add(
                            Song(
                                "local:" + id,
                                c.getString(iTitle) ?: "Unknown",
                                artist,
                                (c.getLong(iDur) / 1000L).toInt(),
                                uri,
                                "content://media/external/audio/albumart/" + c.getLong(iAlbum)
                            )
                        )
                        tags[uri] = label(c.getString(iMime)) + " · Local file"
                    }
                }
                Pair(out, tags)
            }
            songs.clear()
            songs.addAll(found.first)
            for ((k, v) in found.second) StreamMeta.byUrl[k] = v
            scanned = true
        } catch (e: Exception) {
        } finally {
            scanning = false
        }
    }
}

// scans once in the background when the Library opens, if the permission was already given
@Composable
fun LocalFilesAuto() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        if (!LocalFiles.scanned && LocalFiles.allowed(ctx)) LocalFiles.scan(ctx)
    }
}

// permission + scan button at the top of the Local Files page
@Composable
fun LocalFilesBar() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ok by remember { mutableStateOf(LocalFiles.allowed(ctx)) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        ok = granted
        if (granted) scope.launch { LocalFiles.scan(ctx) }
    }
    LaunchedEffect(ok) {
        if (ok && !LocalFiles.scanned) LocalFiles.scan(ctx)
    }
    Row(
        Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable {
                if (ok) scope.launch { LocalFiles.scan(ctx) } else ask.launch(LocalFiles.permission())
            }
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Sync, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Text(
            when {
                !ok -> "Allow access to scan music"
                LocalFiles.scanning -> "Scanning..."
                else -> "Scan again (" + LocalFiles.songs.size + " songs)"
            },
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}
