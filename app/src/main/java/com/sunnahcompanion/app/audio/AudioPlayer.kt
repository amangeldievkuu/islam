package com.sunnahcompanion.app.audio

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sunnahcompanion.app.data.model.ContentItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class PlaybackState(
    val itemId: String? = null,
    val isPlaying: Boolean = false,
    /** Index into the item's playlist (one entry per ayah for multi-ayah items). */
    val index: Int = 0,
)

/** App-wide entry point to [PlaybackService], shared by the UI and notification actions. */
@Singleton
class AudioPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val mutex = Mutex()
    private var controller: MediaController? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish(player)
    }

    suspend fun play(item: ContentItem) = withContext(Dispatchers.Main) {
        val mediaItems = item.toMediaItems()
        if (mediaItems.isEmpty()) return@withContext
        val player = connect()
        player.setMediaItems(mediaItems)
        player.prepare()
        player.play()
    }

    /** Connects early so playback started elsewhere (e.g. a notification) is reflected in the UI. */
    suspend fun ensureConnected() {
        withContext(Dispatchers.Main) { connect() }
    }

    suspend fun pause() = withContext(Dispatchers.Main) { controller?.pause() }

    suspend fun resume() = withContext(Dispatchers.Main) { controller?.play() }

    private suspend fun connect(): MediaController = mutex.withLock {
        controller?.takeIf { it.isConnected } ?: run {
            controller?.release()
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            MediaController.Builder(context, token).buildAsync().await().also {
                it.addListener(listener)
                controller = it
                publish(it)
            }
        }
    }

    private fun publish(player: Player) {
        val mediaId = player.currentMediaItem?.mediaId
        _state.value = PlaybackState(
            itemId = mediaId?.substringBefore('#'),
            isPlaying = player.isPlaying,
            index = player.currentMediaItemIndex,
        )
    }
}
