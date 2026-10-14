package com.sunnahcompanion.app.trigger

import android.util.Log
import com.sunnahcompanion.app.core.database.dao.HabitLogDao
import com.sunnahcompanion.app.core.database.model.TriggerSource
import com.sunnahcompanion.app.core.database.seed.ContentSeeder
import com.sunnahcompanion.app.core.datastore.AppPreferencesDataSource
import com.sunnahcompanion.app.core.time.TimeProvider
import com.sunnahcompanion.app.data.model.ContentIds
import com.sunnahcompanion.app.data.repository.ContentRepository
import com.sunnahcompanion.app.notification.DuaNotifier
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Turns raw sensor events into (at most one) well-timed dua notification. */
@Singleton
class ContextualTriggerHandler @Inject constructor(
    private val seeder: ContentSeeder,
    private val preferences: AppPreferencesDataSource,
    private val habitLogDao: HabitLogDao,
    private val contentRepository: ContentRepository,
    private val notifier: DuaNotifier,
    private val time: TimeProvider,
) {
    // Serialises prompts so near-simultaneous triggers can't both pass the cooldown check.
    private val mutex = Mutex()

    suspend fun onLeftHome() {
        seeder.ensureSeeded()
        if (!preferences.userPreferences.first().leavingHomeEnabled) return
        prompt(ContentIds.LEAVING_HOME, TriggerSource.GEOFENCE)
    }

    suspend fun onDrivingStarted(source: TriggerSource) {
        seeder.ensureSeeded()
        if (!preferences.userPreferences.first().drivingEnabled) return
        prompt(ContentIds.RIDING_VEHICLE, source)
    }

    private suspend fun prompt(itemId: String, source: TriggerSource) = mutex.withLock {
        val now = time.nowMillis()
        val allowed = TriggerPolicy.shouldPrompt(
            nowMillis = now,
            lastLoggedAt = habitLogDao.getLastLoggedAt(itemId)?.toEpochMilliseconds(),
            lastPromptAt = preferences.lastPromptAt(itemId),
        )
        if (!allowed) {
            Log.d(TAG, "Skipping $itemId from $source: read or prompted recently")
            return@withLock
        }
        val item = contentRepository.getItem(itemId) ?: return@withLock
        notifier.showDua(item, source)
        preferences.setLastPromptAt(itemId, now)
    }

    private companion object {
        const val TAG = "ContextualTrigger"
    }
}
