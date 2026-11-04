package com.prismwave.app

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

// Lets the notification, the lock screen and headset buttons use the app's own next / previous.
object PlayerBridge {
    @Volatile var onNext: (() -> Unit)? = null
    @Volatile var onPrev: (() -> Unit)? = null
}

// Connects the bridge to the running app (called from the always-present VideoWarmup).
@Composable
fun PlayerBridgeHook(s: AppState) {
    remember(s) {
        PlayerBridge.onNext = { s.next() }
        PlayerBridge.onPrev = { s.prev() }
        0
    }
}

// Always offers Next and Previous (even before the next song is lined up) and sends them to the app.
@OptIn(UnstableApi::class)
class PrismPlayer(player: Player) : ForwardingPlayer(player) {
    override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands().buildUpon()
            .add(Player.COMMAND_SEEK_TO_NEXT)
            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .build()

    override fun isCommandAvailable(command: Int): Boolean = when (command) {
        Player.COMMAND_SEEK_TO_NEXT,
        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_PREVIOUS,
        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
        else -> super.isCommandAvailable(command)
    }

    override fun hasNextMediaItem(): Boolean = true

    override fun hasPreviousMediaItem(): Boolean = true

    override fun seekToNext() {
        val f = PlayerBridge.onNext
        if (f != null) f() else super.seekToNext()
    }

    override fun seekToNextMediaItem() {
        val f = PlayerBridge.onNext
        if (f != null) f() else super.seekToNextMediaItem()
    }

    override fun seekToPrevious() {
        val f = PlayerBridge.onPrev
        if (f != null) f() else super.seekToPrevious()
    }

    override fun seekToPreviousMediaItem() {
        val f = PlayerBridge.onPrev
        if (f != null) f() else super.seekToPreviousMediaItem()
    }
}
