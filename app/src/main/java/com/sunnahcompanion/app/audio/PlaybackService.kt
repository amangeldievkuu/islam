package com.sunnahcompanion.app.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sunnahcompanion.app.R
import com.sunnahcompanion.app.core.database.seed.ContentSeeder
import com.sunnahcompanion.app.data.repository.ContentRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Hosts the player so recitation keeps going with the screen off or the app closed, with
 * standard media controls on the lock screen. In-app clients connect through [AudioPlayer].
 *
 * Notification "Play Audio" starts this service directly via [playItemIntent]. Since Android 16,
 * audio focus is refused to background apps, so the service enters the foreground *before*
 * the player requests focus (Media3 only does this itself on API 31-32).
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject lateinit var cacheDataSourceFactory: CacheDataSource.Factory

    @Inject lateinit var contentRepository: ContentRepository

    @Inject lateinit var contentSeeder: ContentSeeder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PLAY_ITEM) {
            startForegroundForPlayback()
            intent.getStringExtra(EXTRA_ITEM_ID)?.let(::playItem) ?: stopPlaceholder()
            return START_NOT_STICKY
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun playItem(itemId: String) {
        scope.launch {
            contentSeeder.ensureSeeded()
            val mediaItems = contentRepository.getItem(itemId)?.toMediaItems().orEmpty()
            val session = mediaSession
            if (mediaItems.isEmpty() || session == null) {
                Log.w(TAG, "No audio for $itemId")
                stopPlaceholder()
                return@launch
            }
            // Sessions are normally registered when a controller binds; nobody has bound on this
            // path, so register explicitly or Media3 won't manage the media notification.
            addSession(session)
            session.player.run {
                setMediaItems(mediaItems)
                prepare()
                play() // Media3 now replaces the placeholder with the media notification.
            }
        }
    }

    /**
     * Uses Media3's notification id, so the media-style notification posted once playback starts
     * replaces this placeholder in place.
     */
    private fun startForegroundForPlayback() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PLAYBACK,
                getString(R.string.channel_playback_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_PLAYBACK)
            .setSmallIcon(R.drawable.ic_stat_sunnah)
            .setColor(ContextCompat.getColor(this, R.color.notification_accent))
            .setContentTitle(getString(R.string.playback_preparing))
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        startForeground(
            DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
    }

    private fun stopPlaceholder() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (mediaSession?.player?.mediaItemCount == 0) stopSelf()
    }

    companion object {
        private const val TAG = "PlaybackService"
        private const val ACTION_PLAY_ITEM = "com.sunnahcompanion.app.action.PLAY_ITEM"
        private const val EXTRA_ITEM_ID = "item_id"
        private const val CHANNEL_PLAYBACK = "playback"

        /** For notification actions: starts playback of [itemId] as a foreground service. */
        fun playItemIntent(context: Context, itemId: String): PendingIntent = PendingIntent.getForegroundService(
            context,
            "play:$itemId".hashCode(),
            Intent(context, PlaybackService::class.java).setAction(ACTION_PLAY_ITEM).putExtra(EXTRA_ITEM_ID, itemId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
