package com.sunnahcompanion.app.audio

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.sunnahcompanion.app.data.model.ContentItem

internal const val RECITER = "Mishary Rashid Alafasy"

/** One media item per audio URL; ids are `itemId#index` so playback maps back to content. */
internal fun ContentItem.toMediaItems(): List<MediaItem> {
    val urls = audioUrls
    return urls.mapIndexed { index, url ->
        MediaItem.Builder()
            .setMediaId("$id#$index")
            .setUri(url.toUri())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(RECITER)
                    .setTrackNumber(index + 1)
                    .setTotalTrackCount(urls.size)
                    .build(),
            )
            .build()
    }
}
