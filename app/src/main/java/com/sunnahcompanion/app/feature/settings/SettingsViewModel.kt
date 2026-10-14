package com.sunnahcompanion.app.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sunnahcompanion.app.core.common.AppPermissions
import com.sunnahcompanion.app.core.datastore.AppPreferencesDataSource
import com.sunnahcompanion.app.core.datastore.UserPreferences
import com.sunnahcompanion.app.core.time.TimeProvider
import com.sunnahcompanion.app.trigger.TriggerRegistrar
import com.sunnahcompanion.app.trigger.TriggerStatus
import com.sunnahcompanion.app.trigger.driving.PairedDevice
import com.sunnahcompanion.app.trigger.driving.PairedDevices
import com.sunnahcompanion.app.trigger.geofence.CurrentLocationProvider
import com.sunnahcompanion.app.trigger.schedule.ReminderPlanner
import com.sunnahcompanion.app.trigger.schedule.ReminderScheduler
import com.sunnahcompanion.app.trigger.schedule.ReminderType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class PermissionsState(
    val notifications: Boolean = false,
    val fineLocation: Boolean = false,
    val backgroundLocation: Boolean = false,
    val activityRecognition: Boolean = false,
    val bluetoothConnect: Boolean = false,
)

@Immutable
data class SettingsUiState(
    val preferences: UserPreferences? = null,
    val permissions: PermissionsState = PermissionsState(),
    val status: TriggerStatus = TriggerStatus(),
    /** Today's reminder times, keyed by type, for the reminders summary. */
    val reminderTimes: ImmutableMap<ReminderType, ZonedDateTime> = emptyMap<ReminderType, ZonedDateTime>().toImmutableMap(),
    val reminderTimesEstimated: Boolean = true,
    val isLocating: Boolean = false,
    val pairedDevices: ImmutableList<PairedDevice> = persistentListOf(),
    val message: SettingsMessage? = null,
)

enum class SettingsMessage { LOCATION_UNAVAILABLE, HOME_SAVED, PERMISSION_DENIED }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferencesDataSource,
    private val permissions: AppPermissions,
    private val registrar: TriggerRegistrar,
    private val reminderScheduler: ReminderScheduler,
    private val locationProvider: CurrentLocationProvider,
    private val pairedDevices: PairedDevices,
    private val time: TimeProvider,
) : ViewModel() {

    private val permissionState = MutableStateFlow(readPermissions())
    private val transient = MutableStateFlow(TransientState())

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.userPreferences,
        permissionState,
        registrar.status,
        transient,
    ) { prefs, perms, status, transient ->
        val today = time.today()
        val times = reminderScheduler.timesFor(today, prefs.homeLocation)
        SettingsUiState(
            preferences = prefs,
            permissions = perms,
            status = status,
            reminderTimes = ReminderPlanner.remindersOn(today, times).associate { it.type to it.at }.toImmutableMap(),
            reminderTimesEstimated = times.isEstimated,
            isLocating = transient.isLocating,
            pairedDevices = transient.pairedDevices,
            message = transient.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /**
     * Call on resume and after any permission result: grants, and system state such as
     * Location being switched on, can change while the user is outside the app.
     */
    fun refreshPermissions() {
        val updated = readPermissions()
        val status = registrar.status.value
        val hasError = status.geofenceError != null || status.drivingError != null
        if (updated != permissionState.value || hasError) {
            permissionState.value = updated
            registrar.requestSync()
        }
    }

    fun onPermissionDenied() = transient.update { it.copy(message = SettingsMessage.PERMISSION_DENIED) }

    fun onMessageShown() = transient.update { it.copy(message = null) }

    fun setLeavingHomeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setLeavingHomeEnabled(enabled)
            if (enabled && preferences.userPreferences.first().homeLocation == null) saveCurrentLocationAsHome()
        }
    }

    fun onUseCurrentLocation() {
        viewModelScope.launch { saveCurrentLocationAsHome() }
    }

    fun setDrivingEnabled(enabled: Boolean) = launch { preferences.setDrivingEnabled(enabled) }

    fun setRemindersEnabled(enabled: Boolean) = launch { preferences.setRemindersEnabled(enabled) }

    fun setLearningMode(enabled: Boolean) = launch { preferences.setLearningMode(enabled) }

    fun setArabicTextScale(scale: Float) = launch { preferences.setArabicTextScale(scale) }

    fun setDynamicColor(enabled: Boolean) = launch { preferences.setDynamicColor(enabled) }

    fun loadPairedDevices() = transient.update { it.copy(pairedDevices = pairedDevices.list().toImmutableList()) }

    fun setCarDevices(addresses: Set<String>) = launch { preferences.setCarDeviceAddresses(addresses) }

    private suspend fun saveCurrentLocationAsHome() {
        transient.update { it.copy(isLocating = true) }
        val location = runCatching { locationProvider.current() }.getOrNull()
        if (location != null) preferences.setHomeLocation(location)
        transient.update {
            it.copy(
                isLocating = false,
                message = if (location != null) SettingsMessage.HOME_SAVED else SettingsMessage.LOCATION_UNAVAILABLE,
            )
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun readPermissions() = PermissionsState(
        notifications = permissions.hasNotifications,
        fineLocation = permissions.hasFineLocation,
        backgroundLocation = permissions.hasBackgroundLocation,
        activityRecognition = permissions.hasActivityRecognition,
        bluetoothConnect = permissions.hasBluetoothConnect,
    )

    private data class TransientState(
        val isLocating: Boolean = false,
        val pairedDevices: ImmutableList<PairedDevice> = persistentListOf(),
        val message: SettingsMessage? = null,
    )
}
