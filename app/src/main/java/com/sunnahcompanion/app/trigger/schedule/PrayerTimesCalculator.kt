package com.sunnahcompanion.app.trigger.schedule

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.sunnahcompanion.app.core.datastore.HomeLocation
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import javax.inject.Inject

data class DailyPrayerTimes(
    val fajr: ZonedDateTime,
    val sunrise: ZonedDateTime,
    val asr: ZonedDateTime,
    val maghrib: ZonedDateTime,
    val isha: ZonedDateTime,
    /** True when no home location is set and fixed default times are used instead. */
    val isEstimated: Boolean,
)

/** Fully offline prayer times (Muslim World League method, standard Asr) via Adhan. */
class PrayerTimesCalculator @Inject constructor() {

    fun forDate(date: LocalDate, zone: ZoneId, location: HomeLocation?): DailyPrayerTimes {
        if (location == null) return fallback(date, zone)
        return runCatching {
            val times = PrayerTimes(
                Coordinates(location.latitude, location.longitude),
                DateComponents(date.year, date.monthValue, date.dayOfMonth),
                CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters,
            )
            DailyPrayerTimes(
                fajr = times.fajr.at(zone),
                sunrise = times.sunrise.at(zone),
                asr = times.asr.at(zone),
                maghrib = times.maghrib.at(zone),
                isha = times.isha.at(zone),
                isEstimated = false,
            )
        }.getOrElse { fallback(date, zone) } // Extreme latitudes can have no computable Fajr/Isha.
    }

    private fun Date.at(zone: ZoneId): ZonedDateTime = toInstant().atZone(zone)

    private fun fallback(date: LocalDate, zone: ZoneId) = DailyPrayerTimes(
        fajr = date.atTime(LocalTime.of(5, 30)).atZone(zone),
        sunrise = date.atTime(LocalTime.of(6, 45)).atZone(zone),
        asr = date.atTime(LocalTime.of(15, 45)).atZone(zone),
        maghrib = date.atTime(LocalTime.of(18, 15)).atZone(zone),
        isha = date.atTime(LocalTime.of(19, 45)).atZone(zone),
        isEstimated = true,
    )
}
