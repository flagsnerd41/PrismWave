package com.prismwave.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

// Where the big cover is on screen. The lyrics page uses it to shrink the cover from that
// spot into its small header corner, and to fly it back when the lyrics close.
// Several places can show a cover (the player and the half lyrics view), the one with the
// highest priority is used.
object SharedCover {
    private val spots = HashMap<Any, Pair<Int, Rect>>()

    val rect: Rect?
        get() = synchronized(spots) { spots.values.maxByOrNull { it.first }?.second }

    fun set(owner: Any, priority: Int, r: Rect) {
        synchronized(spots) { spots[owner] = Pair(priority, r) }
    }

    fun clear(owner: Any) {
        synchronized(spots) { spots.remove(owner) }
    }
}

// 0 = the player is showing, 1 = the lyrics are showing. The player fades with it.
object LyricsMorph {
    var p by mutableStateOf(0f)
}

@Composable
fun Modifier.shareCover(priority: Int = 0): Modifier {
    val owner = remember { Any() }
    DisposableEffect(Unit) { onDispose { SharedCover.clear(owner) } }
    return this.onGloballyPositioned { SharedCover.set(owner, priority, it.boundsInRoot()) }
}
