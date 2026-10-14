package com.sunnahcompanion.app.trigger.schedule

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

enum class ReminderType { MORNING_ADHKAR, EVENING_ADHKAR, BEFORE_SLEEP, FRIDAY_KAHF }

data class ScheduledReminder(val type: ReminderType, val at: ZonedDateTime)

/** Pure scheduling rules for time-based reminders, anchored to the day's prayer times. */
object ReminderPlanner {

    /** A reminder still fires if the device was asleep through its exact time. */
    val GRACE: Duration = Duration.ofHours(2)

    private val LATEST_SLEEP_REMINDER: LocalTime = LocalTime.of(23, 30)

    fun remindersOn(date: LocalDate, times: DailyPrayerTimes): List<ScheduledReminder> = buildList {
        add(ScheduledReminder(ReminderType.MORNING_ADHKAR, times.fajr.plusMinutes(20)))
        add(ScheduledReminder(ReminderType.EVENING_ADHKAR, times.asr.plusMinutes(20)))
        val latest = date.atTime(LATEST_SLEEP_REMINDER).atZone(times.isha.zone)
        val sleep = minOf(times.isha.plusMinutes(60), latest)
        add(ScheduledReminder(ReminderType.BEFORE_SLEEP, maxOf(sleep, times.isha.plusMinutes(10))))
        if (date.dayOfWeek == DayOfWeek.FRIDAY) {
            add(ScheduledReminder(ReminderType.FRIDAY_KAHF, times.sunrise.plusHours(3)))
        }
    }.sortedBy { it.at }

    /** Reminders whose time has passed (within [GRACE]) and that haven't fired today. */
    fun due(
        now: ZonedDateTime,
        times: DailyPrayerTimes,
        deliveredOn: (ReminderType) -> Long?,
    ): List<ScheduledReminder> {
        val today = now.toLocalDate()
        return remindersOn(today, times).filter { reminder ->
            !reminder.at.isAfter(now) &&
                Duration.between(reminder.at, now) < GRACE &&
                deliveredOn(reminder.type) != today.toEpochDay()
        }
    }

    /** The first reminder strictly after [now], searching up to a week ahead. */
    fun next(now: ZonedDateTime, timesFor: (LocalDate) -> DailyPrayerTimes): ScheduledReminder? =
        (0L..7L).asSequence()
            .map { now.toLocalDate().plusDays(it) }
            .flatMap { date -> remindersOn(date, timesFor(date)) }
            .firstOrNull { it.at.isAfter(now) }
}
