package com.sunnahcompanion.app.data.repository

import androidx.room.withTransaction
import com.sunnahcompanion.app.core.database.SunnahDatabase
import com.sunnahcompanion.app.core.database.dao.ActiveHabitDao
import com.sunnahcompanion.app.core.database.dao.HabitLogDao
import com.sunnahcompanion.app.core.database.dao.SunnahItemDao
import com.sunnahcompanion.app.core.database.model.HabitStatus
import com.sunnahcompanion.app.core.database.model.TriggerSource
import com.sunnahcompanion.app.core.time.TimeProvider
import com.sunnahcompanion.app.data.model.ActivationResult
import com.sunnahcompanion.app.data.model.DayProgress
import com.sunnahcompanion.app.data.model.HabitItem
import com.sunnahcompanion.app.data.model.HabitMasteryPolicy
import com.sunnahcompanion.app.data.model.ItemProgress
import com.sunnahcompanion.app.data.model.asModel
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.toKotlinDuration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/** "Active 3" habits and daily history. All writes target the user's current local date. */
@Singleton
class HabitRepository @Inject constructor(
    private val database: SunnahDatabase,
    private val habitLogDao: HabitLogDao,
    private val activeHabitDao: ActiveHabitDao,
    private val sunnahItemDao: SunnahItemDao,
    private val time: TimeProvider,
) {
    fun observeActiveHabits(date: LocalDate): Flow<List<HabitItem>> =
        activeHabitDao.observeActiveWithLog(date).map { rows -> rows.map { it.asModel() } }

    fun observeMasteredHabits(date: LocalDate): Flow<List<HabitItem>> =
        activeHabitDao.observeWithLog(date, HabitStatus.MASTERED).map { rows -> rows.map { it.asModel() } }

    /** The 7 days ending on [endDate], oldest first. Totals use today's active set. */
    fun observeWeek(endDate: LocalDate): Flow<List<DayProgress>> {
        val start = endDate.minusDays(6)
        return combine(
            habitLogDao.observeDailyCompletions(start, endDate),
            activeHabitDao.observeActiveCount(),
        ) { completions, total ->
            val byDate = completions.associate { it.date to it.completedCount }
            (0L..6L).map { offset ->
                val date = start.plusDays(offset)
                DayProgress(date = date, completed = byDate[date] ?: 0, total = total)
            }
        }
    }

    fun observeProgress(itemId: String, date: LocalDate): Flow<ItemProgress?> =
        habitLogDao.observeLog(date, itemId).map { log ->
            log?.let { ItemProgress(it.completedCount, it.targetCount, it.isCompleted) }
        }

    fun observeIsActive(itemId: String): Flow<Boolean> =
        activeHabitDao.observe(itemId).map { it?.status == HabitStatus.ACTIVE }

    fun observeHasFreeSlot(): Flow<Boolean> =
        activeHabitDao.observeActiveCount().map { it < HabitMasteryPolicy.MAX_ACTIVE }

    suspend fun markCompleted(itemId: String, source: TriggerSource) = database.withTransaction {
        val today = time.today()
        habitLogDao.markCompleted(today, itemId, targetFor(itemId), source, time.nowInstant())
        graduateIfMastered(itemId, today)
    }

    suspend fun increment(itemId: String, delta: Int, source: TriggerSource) = database.withTransaction {
        val today = time.today()
        habitLogDao.incrementProgress(today, itemId, delta, targetFor(itemId), source, time.nowInstant())
        graduateIfMastered(itemId, today)
    }

    suspend fun clearToday(itemId: String) = habitLogDao.clear(time.today(), itemId)

    suspend fun isCompletedToday(itemId: String): Boolean =
        habitLogDao.getLog(time.today(), itemId)?.isCompleted == true

    /** True if [itemId] had any progress logged in the last [window], across midnight too. */
    suspend fun wasLoggedWithin(itemId: String, window: Duration): Boolean {
        val last = habitLogDao.getLastLoggedAt(itemId) ?: return false
        return time.nowInstant() - last < window.toKotlinDuration()
    }

    /** Takes a free Active 3 slot; a mastered habit restarts its count from today. */
    suspend fun activate(itemId: String): ActivationResult {
        val activated = activeHabitDao.activate(
            itemId = itemId,
            targetCount = targetFor(itemId),
            today = time.today(),
            maxActive = HabitMasteryPolicy.MAX_ACTIVE,
        )
        return if (activated) ActivationResult.ACTIVATED else ActivationResult.SLOTS_FULL
    }

    suspend fun deactivate(itemId: String) = activeHabitDao.deactivate(itemId)

    /** Graduates an active habit once it has enough completed days since activation. */
    private suspend fun graduateIfMastered(itemId: String, today: LocalDate) {
        val habit = activeHabitDao.get(itemId)?.takeIf { it.status == HabitStatus.ACTIVE } ?: return
        val completedDays = habitLogDao.countCompletedDaysSince(itemId, habit.activatedOn)
        if (HabitMasteryPolicy.isMastered(completedDays)) activeHabitDao.markMastered(itemId, today)
    }

    /** The habit's own target if it is an Active 3 habit, else the recommended count. */
    private suspend fun targetFor(itemId: String): Int =
        activeHabitDao.get(itemId)?.targetCount
            ?: sunnahItemDao.getItem(itemId)?.recommendedTargetCount
            ?: 1
}
