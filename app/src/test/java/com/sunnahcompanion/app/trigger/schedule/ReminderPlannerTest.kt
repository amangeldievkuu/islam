package com.sunnahcompanion.app.trigger.schedule

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlannerTest {
    private val zone = ZoneId.of("Asia/Bishkek")
    private val thursday = LocalDate.of(2026, 10, 15)
    private val friday = thursday.plusDays(1)

    private fun times(date: LocalDate, isha: LocalTime = LocalTime.of(19, 30)) = DailyPrayerTimes(
        fajr = date.atTime(5, 20).atZone(zone),
        sunrise = date.atTime(6, 50).atZone(zone),
        asr = date.atTime(15, 40).atZone(zone),
        maghrib = date.atTime(18, 10).atZone(zone),
        isha = date.atTime(isha).atZone(zone),
        isEstimated = false,
    )

    private fun at(date: LocalDate, h: Int, m: Int): ZonedDateTime = date.atTime(h, m).atZone(zone)

    @Test
    fun remindersAreAnchoredToPrayerTimes() {
        val reminders = ReminderPlanner.remindersOn(thursday, times(thursday)).associate { it.type to it.at }
        assertEquals(at(thursday, 5, 40), reminders[ReminderType.MORNING_ADHKAR])
        assertEquals(at(thursday, 16, 0), reminders[ReminderType.EVENING_ADHKAR])
        assertEquals(at(thursday, 20, 30), reminders[ReminderType.BEFORE_SLEEP])
        assertTrue(ReminderType.FRIDAY_KAHF !in reminders)
    }

    @Test
    fun kahfOnlyOnFriday() {
        val reminders = ReminderPlanner.remindersOn(friday, times(friday)).associate { it.type to it.at }
        assertEquals(at(friday, 9, 50), reminders[ReminderType.FRIDAY_KAHF])
    }

    @Test
    fun lateSummerIsha_sleepReminderStaysBeforeMidnight() {
        val reminders = ReminderPlanner.remindersOn(thursday, times(thursday, isha = LocalTime.of(23, 0)))
        assertEquals(at(thursday, 23, 30), reminders.single { it.type == ReminderType.BEFORE_SLEEP }.at)
    }

    @Test
    fun dueRespectsGraceAndOncePerDay() {
        val now = at(thursday, 16, 30)
        val due = ReminderPlanner.due(now, times(thursday)) { null }.map { it.type }
        assertEquals(listOf(ReminderType.EVENING_ADHKAR), due) // Morning is past the 2h grace.

        val delivered = ReminderPlanner.due(now, times(thursday)) { thursday.toEpochDay() }
        assertTrue(delivered.isEmpty())
    }

    @Test
    fun nextRollsOverToTomorrow() {
        val next = ReminderPlanner.next(at(thursday, 22, 0)) { times(it) }
        assertEquals(ReminderType.MORNING_ADHKAR, next?.type)
        assertEquals(at(friday, 5, 40), next?.at)
    }
}
