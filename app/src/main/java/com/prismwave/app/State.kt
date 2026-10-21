package com.prismwave.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class Screen { Home, Search, Library, Settings }

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String,
    val seconds: Int,
    val color: Color
)

// Sample data. Replaced by the real YouTube Music engine in the next wave.
val SampleSongs = listOf(
    Song(1, "Midnight Drive", "Nova Lane", "Neon Roads", 214, Color(0xFFB5651D)),
    Song(2, "Golden Hour", "Sela Moon", "Warm Light", 188, Color(0xFFC9A227)),
    Song(3, "Paper Planes", "The Quiet Fields", "Small Wonders", 241, Color(0xFF3E7C6B)),
    Song(4, "Static Bloom", "Orbit Club", "Signal", 203, Color(0xFF6B4C9A)),
    Song(5, "Slow Tide", "Marlo Reyes", "Salt & Sand", 265, Color(0xFF2F6690)),
    Song(6, "Lantern", "Ivy Hart", "After Dark", 197, Color(0xFFA23E48)),
    Song(7, "Highway Rain", "Nova Lane", "Neon Roads", 232, Color(0xFF4A5759)),
    Song(8, "Honey & Smoke", "Sela Moon", "Warm Light", 176, Color(0xFF9C6644)),
    Song(9, "Daydream Club", "Orbit Club", "Signal", 219, Color(0xFF357266)),
    Song(10, "Northern Lights", "Marlo Reyes", "Salt & Sand", 281, Color(0xFF3A5A8C))
)

class AppState {
    var screen by mutableStateOf(Screen.Home)
    var current by mutableStateOf<Song?>(null)
    var playing by mutableStateOf(false)
    var position by mutableStateOf(0f)
    var showPlayer by mutableStateOf(false)
    var glass by mutableStateOf(0.75f)
    var batterySaver by mutableStateOf(false)
    var accent by mutableStateOf(Gold)
    var shuffle by mutableStateOf(false)
    var repeat by mutableStateOf(false)
    var crossfade by mutableStateOf(0f)
    var sleepLeft by mutableStateOf(0)
    var playCount by mutableStateOf(0)
    var listenSeconds by mutableStateOf(0)
    val liked = mutableStateListOf<Int>()

    fun glassAlpha(): Float = if (batterySaver) 1f else glass

    fun play(song: Song) {
        current = song
        position = 0f
        playing = true
        playCount++
    }

    fun toggle() {
        if (current != null) playing = !playing
    }

    fun next() {
        val cur = current ?: return
        val i = SampleSongs.indexOfFirst { it.id == cur.id }
        val n = if (shuffle) SampleSongs.random() else SampleSongs[(i + 1) % SampleSongs.size]
        play(n)
    }

    fun prev() {
        val cur = current ?: return
        if (position > 3f) {
            position = 0f
            return
        }
        val i = SampleSongs.indexOfFirst { it.id == cur.id }
        play(SampleSongs[(i - 1 + SampleSongs.size) % SampleSongs.size])
    }

    fun toggleLike(song: Song) {
        if (song.id in liked) liked.remove(song.id) else liked.add(song.id)
    }

    fun tick() {
        if (playing) {
            position += 1f
            listenSeconds++
            val cur = current
            if (cur != null && position >= cur.seconds) {
                if (repeat) position = 0f else next()
            }
        }
        if (sleepLeft > 0) {
            sleepLeft--
            if (sleepLeft == 0) playing = false
        }
    }
}
