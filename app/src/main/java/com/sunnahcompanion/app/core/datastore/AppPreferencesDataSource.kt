package com.sunnahcompanion.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class HomeLocation(val latitude: Double, val longitude: Double)

data class UserPreferences(
    val homeLocation: HomeLocation?,
    val leavingHomeEnabled: Boolean,
    val drivingEnabled: Boolean,
    val carDeviceAddresses: Set<String>,
    val remindersEnabled: Boolean,
    val learningMode: Boolean,
    val arabicTextScale: Float,
    val dynamicColor: Boolean,
)

/** Live Tasbih counter, persisted so a count survives process death. */
data class TasbihPreferences(
    val dhikrId: String?,
    val target: Int?,
    val count: Int,
    val rounds: Int,
)

/** Small key-value app state backed by Preferences DataStore. */
@Singleton
class AppPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    /** Version of the bundled seed last written to Room; 0 means never seeded. */
    val seedVersion: Flow<Int> = dataStore.data.map { it[Keys.SEED_VERSION] ?: 0 }

    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val lat = prefs[Keys.HOME_LAT]
        val lng = prefs[Keys.HOME_LNG]
        UserPreferences(
            homeLocation = if (lat != null && lng != null) HomeLocation(lat, lng) else null,
            leavingHomeEnabled = prefs[Keys.LEAVING_HOME_ENABLED] ?: false,
            drivingEnabled = prefs[Keys.DRIVING_ENABLED] ?: false,
            carDeviceAddresses = prefs[Keys.CAR_DEVICES] ?: emptySet(),
            remindersEnabled = prefs[Keys.REMINDERS_ENABLED] ?: false,
            learningMode = prefs[Keys.LEARNING_MODE] ?: false,
            arabicTextScale = prefs[Keys.ARABIC_SCALE] ?: 1f,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
        )
    }

    val tasbih: Flow<TasbihPreferences> = dataStore.data.map { prefs ->
        TasbihPreferences(
            dhikrId = prefs[Keys.TASBIH_DHIKR],
            target = prefs[Keys.TASBIH_TARGET],
            count = prefs[Keys.TASBIH_COUNT] ?: 0,
            rounds = prefs[Keys.TASBIH_ROUNDS] ?: 0,
        )
    }

    suspend fun setSeedVersion(version: Int) = edit { it[Keys.SEED_VERSION] = version }

    suspend fun setHomeLocation(location: HomeLocation) = edit {
        it[Keys.HOME_LAT] = location.latitude
        it[Keys.HOME_LNG] = location.longitude
    }

    suspend fun setLeavingHomeEnabled(enabled: Boolean) = edit { it[Keys.LEAVING_HOME_ENABLED] = enabled }

    suspend fun setDrivingEnabled(enabled: Boolean) = edit { it[Keys.DRIVING_ENABLED] = enabled }

    suspend fun setCarDeviceAddresses(addresses: Set<String>) = edit { it[Keys.CAR_DEVICES] = addresses }

    suspend fun setRemindersEnabled(enabled: Boolean) = edit { it[Keys.REMINDERS_ENABLED] = enabled }

    suspend fun setLearningMode(enabled: Boolean) = edit { it[Keys.LEARNING_MODE] = enabled }

    suspend fun setArabicTextScale(scale: Float) = edit { it[Keys.ARABIC_SCALE] = scale }

    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }

    suspend fun setTasbih(state: TasbihPreferences) = edit { prefs ->
        state.dhikrId?.let { prefs[Keys.TASBIH_DHIKR] = it }
        state.target?.let { prefs[Keys.TASBIH_TARGET] = it }
        prefs[Keys.TASBIH_COUNT] = state.count
        prefs[Keys.TASBIH_ROUNDS] = state.rounds
    }

    /** Epoch millis a contextual prompt for [itemId] was last shown, used as a cooldown. */
    suspend fun lastPromptAt(itemId: String): Long? = dataStore.data.first()[promptKey(itemId)]

    suspend fun setLastPromptAt(itemId: String, millis: Long) = edit { it[promptKey(itemId)] = millis }

    /** Epoch day a scheduled reminder last fired, so each fires at most once a day. */
    suspend fun reminderDeliveredOn(reminder: String): Long? = dataStore.data.first()[reminderKey(reminder)]

    suspend fun setReminderDeliveredOn(reminder: String, epochDay: Long) =
        edit { it[reminderKey(reminder)] = epochDay }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private fun promptKey(itemId: String) = longPreferencesKey("last_prompt_$itemId")

    private fun reminderKey(reminder: String) = longPreferencesKey("reminder_delivered_$reminder")

    private object Keys {
        val SEED_VERSION = intPreferencesKey("seed_version")
        val HOME_LAT = doublePreferencesKey("home_lat")
        val HOME_LNG = doublePreferencesKey("home_lng")
        val LEAVING_HOME_ENABLED = booleanPreferencesKey("leaving_home_enabled")
        val DRIVING_ENABLED = booleanPreferencesKey("driving_enabled")
        val CAR_DEVICES = stringSetPreferencesKey("car_bluetooth_devices")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val LEARNING_MODE = booleanPreferencesKey("learning_mode")
        val ARABIC_SCALE = floatPreferencesKey("arabic_text_scale")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val TASBIH_DHIKR = stringPreferencesKey("tasbih_dhikr")
        val TASBIH_TARGET = intPreferencesKey("tasbih_target")
        val TASBIH_COUNT = intPreferencesKey("tasbih_count")
        val TASBIH_ROUNDS = intPreferencesKey("tasbih_rounds")
    }
}
