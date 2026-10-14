package com.sunnahcompanion.app.trigger.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.sunnahcompanion.app.R
import com.sunnahcompanion.app.core.database.model.LifeSituation
import com.sunnahcompanion.app.core.database.model.TriggerSource
import com.sunnahcompanion.app.core.datastore.AppPreferencesDataSource
import com.sunnahcompanion.app.core.datastore.HomeLocation
import com.sunnahcompanion.app.core.time.TimeProvider
import com.sunnahcompanion.app.data.model.ContentIds
import com.sunnahcompanion.app.data.repository.ContentRepository
import com.sunnahcompanion.app.data.repository.HabitRepository
import com.sunnahcompanion.app.notification.DuaNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Keeps exactly one inexact, Doze-tolerant alarm set for the next reminder. Inexact alarms need
 * no special permission; a few minutes of drift is fine for "after Fajr" style reminders.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: AppPreferencesDataSource,
    private val calculator: PrayerTimesCalculator,
    private val contentRepository: ContentRepository,
    private val habitRepository: HabitRepository,
    private val notifier: DuaNotifier,
    private val time: TimeProvider,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    private val pendingIntent: PendingIntent
        get() = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ReminderAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun timesFor(date: LocalDate, home: HomeLocation?): DailyPrayerTimes =
        calculator.forDate(date, time.now().zone, home)

    fun reschedule(enabled: Boolean, home: HomeLocation?) {
        alarmManager.cancel(pendingIntent)
        if (!enabled) return
        val next = ReminderPlanner.next(time.now()) { timesFor(it, home) } ?: return
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.at.toInstant().toEpochMilli(), pendingIntent)
    }

    /** Called when the alarm fires: posts whatever is due, then arms the next alarm. */
    suspend fun onAlarm() {
        val prefs = preferences.userPreferences.first()
        if (prefs.remindersEnabled) {
            val now = time.now()
            val deliveredOn = ReminderType.entries.associateWith { preferences.reminderDeliveredOn(it.name) }
            val due = ReminderPlanner.due(now, timesFor(now.toLocalDate(), prefs.homeLocation), deliveredOn::get)
            due.forEach { reminder ->
                deliver(reminder.type)
                preferences.setReminderDeliveredOn(reminder.type.name, now.toLocalDate().toEpochDay())
            }
        }
        reschedule(prefs.remindersEnabled, prefs.homeLocation)
    }

    private suspend fun deliver(type: ReminderType) {
        when (type) {
            ReminderType.MORNING_ADHKAR -> notifier.showCategoryReminder(
                LifeSituation.MORNING,
                context.getString(R.string.reminder_morning_title),
                context.getString(R.string.reminder_morning_text),
            )

            ReminderType.EVENING_ADHKAR -> notifier.showCategoryReminder(
                LifeSituation.EVENING,
                context.getString(R.string.reminder_evening_title),
                context.getString(R.string.reminder_evening_text),
            )

            ReminderType.BEFORE_SLEEP -> remindItem(ContentIds.AL_MULK)
            ReminderType.FRIDAY_KAHF -> remindItem(ContentIds.AL_KAHF)
        }
    }

    private suspend fun remindItem(itemId: String) {
        if (habitRepository.isCompletedToday(itemId)) return
        val item = contentRepository.getItem(itemId) ?: return
        notifier.showDua(item, TriggerSource.SCHEDULED, DuaNotifier.CHANNEL_REMINDERS)
    }
}
