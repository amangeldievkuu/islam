package com.sunnahcompanion.app.trigger

import com.sunnahcompanion.app.core.common.ApplicationScope
import com.sunnahcompanion.app.core.datastore.AppPreferencesDataSource
import com.sunnahcompanion.app.trigger.driving.DrivingDetectionManager
import com.sunnahcompanion.app.trigger.geofence.GeofenceManager
import com.sunnahcompanion.app.trigger.schedule.ReminderScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Outcome of the last registration attempt, surfaced on the Settings screen. */
data class TriggerStatus(
    val geofenceActive: Boolean = false,
    val geofenceError: String? = null,
    val drivingActive: Boolean = false,
    val drivingError: String? = null,
)

/**
 * Reconciles OS registrations (geofence, activity transitions, reminder alarm) with the user's
 * settings and current permissions. Idempotent; runs on app start, on every relevant setting
 * change, after permission grants, and after reboot / app update (which clear registrations).
 */
@Singleton
class TriggerRegistrar @Inject constructor(
    private val preferences: AppPreferencesDataSource,
    private val geofenceManager: GeofenceManager,
    private val drivingManager: DrivingDetectionManager,
    private val reminderScheduler: ReminderScheduler,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow(TriggerStatus())
    val status: StateFlow<TriggerStatus> = _status.asStateFlow()

    /** Starts following settings for the life of the process. */
    fun start() {
        scope.launch {
            preferences.userPreferences
                .map { listOf(it.homeLocation, it.leavingHomeEnabled, it.drivingEnabled, it.remindersEnabled) }
                .distinctUntilChanged()
                .collect { sync() }
        }
    }

    /** For changes settings can't observe, such as a newly granted permission. */
    fun requestSync() {
        scope.launch { sync() }
    }

    suspend fun sync() = mutex.withLock {
        val prefs = preferences.userPreferences.first()

        val home = prefs.homeLocation
        val geofence = if (prefs.leavingHomeEnabled && home != null) {
            geofenceManager.register(home)
        } else {
            geofenceManager.unregister().map { null }
        }

        val driving = if (prefs.drivingEnabled) drivingManager.register() else drivingManager.unregister().map { null }

        reminderScheduler.reschedule(prefs.remindersEnabled, home)

        _status.value = TriggerStatus(
            geofenceActive = prefs.leavingHomeEnabled && home != null && geofence.isSuccess,
            geofenceError = geofence.exceptionOrNull()?.let(::describe),
            drivingActive = prefs.drivingEnabled && driving.isSuccess,
            drivingError = driving.exceptionOrNull()?.let(::describe),
        )
    }

    private fun describe(error: Throwable): String {
        val message = error.message.orEmpty()
        // GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE (1000): Location is off or restricted.
        return if ("1000" in message) "Turn on device Location (Google Location Accuracy) to use this." else message
    }
}
