package com.sunnahcompanion.app.core.time

import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/** Single source of "now" and "today", derived from the injected [Clock]. */
class TimeProvider @Inject constructor(private val clock: Clock) {

    fun nowMillis(): Long = clock.millis()

    fun nowInstant(): Instant = Instant.fromEpochMilliseconds(clock.millis())

    fun now(): ZonedDateTime = ZonedDateTime.now(clock)

    fun today(): LocalDate = LocalDate.now(clock)

    /** Emits today's date immediately and again at each local midnight. */
    fun observeToday(): Flow<LocalDate> = flow {
        while (true) {
            val now = now()
            emit(now.toLocalDate())
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L))
        }
    }.distinctUntilChanged()
}
