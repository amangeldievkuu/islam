package com.sunnahcompanion.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sunnahcompanion.app.core.common.ApplicationScope
import com.sunnahcompanion.app.core.common.launchAsync
import com.sunnahcompanion.app.core.database.model.TriggerSource
import com.sunnahcompanion.app.core.database.seed.ContentSeeder
import com.sunnahcompanion.app.data.repository.ContentRepository
import com.sunnahcompanion.app.data.repository.HabitRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/**
 * Handles "Mark as Read" straight from the lock screen, without opening the app.
 * ("Play Audio" goes directly to [com.sunnahcompanion.app.audio.PlaybackService].)
 */
@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var habitRepository: HabitRepository

    @Inject lateinit var contentRepository: ContentRepository

    @Inject lateinit var contentSeeder: ContentSeeder

    @Inject lateinit var notifier: DuaNotifier

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID) ?: return
        // Log the context that prompted the read (geofence, driving...), not just "notification".
        val source = intent.getStringExtra(EXTRA_SOURCE)
            ?.let { name -> TriggerSource.entries.firstOrNull { it.name == name } }
            ?: TriggerSource.NOTIFICATION
        when (intent.action) {
            ACTION_MARK_READ -> launchAsync(scope) {
                contentSeeder.ensureSeeded()
                habitRepository.markCompleted(itemId, source)
                val title = contentRepository.getItem(itemId)?.title
                if (title != null) notifier.showMarkedAsRead(itemId, title) else notifier.cancel(itemId)
            }
        }
    }

    companion object {
        const val ACTION_MARK_READ = "com.sunnahcompanion.app.action.MARK_READ"
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_SOURCE = "source"
    }
}
