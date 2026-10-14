package com.sunnahcompanion.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sunnahcompanion.app.R
import com.sunnahcompanion.app.audio.PlaybackService
import com.sunnahcompanion.app.core.database.model.LifeSituation
import com.sunnahcompanion.app.core.database.model.TriggerSource
import com.sunnahcompanion.app.data.model.ContentItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the dua notifications. Every prompt is fully readable on the lock screen
 * (VISIBILITY_PUBLIC: the text is not sensitive) and its actions are broadcasts handled by
 * [NotificationActionReceiver], so "Mark as Read" works without unlocking or opening the app.
 */
@Singleton
class DuaNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_CONTEXTUAL,
                    context.getString(R.string.channel_contextual_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.channel_contextual_description)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                },
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    context.getString(R.string.channel_reminders_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = context.getString(R.string.channel_reminders_description)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                },
            ),
        )
    }

    fun canNotify(): Boolean =
        manager.areNotificationsEnabled() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** A dua prompt with "Mark as Read" and, when audio exists, "Play Audio". */
    fun showDua(item: ContentItem, source: TriggerSource, channel: String = CHANNEL_CONTEXTUAL) {
        if (!canNotify()) return
        val body = buildString {
            append(item.arabicText)
            append("\n\n")
            append(item.transliteration)
            append("\n\n")
            append(item.translation)
        }
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_sunnah)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(item.title)
            .setContentText(item.transliteration)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(
                if (channel == CHANNEL_CONTEXTUAL) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT,
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(AppIntents.openItem(context, item.id))
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_stat_sunnah,
                context.getString(R.string.action_mark_as_read),
                actionIntent(NotificationActionReceiver.ACTION_MARK_READ, item.id, source),
            )
        if (item.hasAudio) {
            builder.addAction(
                R.drawable.ic_stat_sunnah,
                context.getString(R.string.action_play_audio),
                PlaybackService.playItemIntent(context, item.id),
            )
        }
        notify(notificationId(item.id), builder)
    }

    /** A reminder that opens a whole category, e.g. "Morning Adhkar". */
    fun showCategoryReminder(category: LifeSituation, title: String, text: String) {
        if (!canNotify()) return
        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_sunnah)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(AppIntents.openCategory(context, category))
            .setAutoCancel(true)
        notify(category.name.hashCode(), builder)
    }

    /** Replaces the prompt with a short-lived, silent confirmation after "Mark as Read". */
    fun showMarkedAsRead(itemId: String, title: String) {
        if (!canNotify()) return
        val builder = NotificationCompat.Builder(context, CHANNEL_CONTEXTUAL)
            .setSmallIcon(R.drawable.ic_stat_sunnah)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(context.getString(R.string.notification_marked_title, title))
            .setContentText(context.getString(R.string.notification_marked_text))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setTimeoutAfter(CONFIRMATION_TIMEOUT_MS)
            .setContentIntent(AppIntents.openItem(context, itemId))
            .setAutoCancel(true)
        notify(notificationId(itemId), builder)
    }

    fun cancel(itemId: String) = manager.cancel(notificationId(itemId))

    private fun notify(id: Int, builder: NotificationCompat.Builder) {
        try {
            manager.notify(id, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post; nothing to show.
        }
    }

    private fun actionIntent(action: String, itemId: String, source: TriggerSource): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            "$action:$itemId".hashCode(),
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(action)
                .putExtra(NotificationActionReceiver.EXTRA_ITEM_ID, itemId)
                .putExtra(NotificationActionReceiver.EXTRA_SOURCE, source.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val CHANNEL_CONTEXTUAL = "contextual_duas"
        const val CHANNEL_REMINDERS = "daily_reminders"
        private const val CONFIRMATION_TIMEOUT_MS = 4_000L

        fun notificationId(itemId: String): Int = itemId.hashCode()
    }
}
