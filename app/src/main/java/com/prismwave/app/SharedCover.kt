package com.prismwave.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

// Where the big cover is on screen. The lyrics page uses it to shrink the cover from that
// spot into its small header corner, and to fly it back when the lyrics close.
object SharedCover {
    var rect: Rect? = null
}

// 0 = the player is showing, 1 = the lyrics are showing. The player fades with it.
object LyricsMorph {
    var p by mutableStateOf(0f)
}

@Composable
fun Modifier.shareCover(): Modifier {
    DisposableEffect(Unit) { onDispose { SharedCover.rect = null } }
    return this.onGloballyPositioned { SharedCover.rect = it.boundsInRoot() }
}
