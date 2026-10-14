package com.sunnahcompanion.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.sunnahcompanion.app.core.database.entity.HabitLogEntity
import com.sunnahcompanion.app.core.database.model.TriggerSource
import com.sunnahcompanion.app.core.database.relation.DailyCompletion
import java.time.LocalDate
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun observeLogsForDate(date: LocalDate): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE date = :date AND itemId = :itemId")
    fun observeLog(date: LocalDate, itemId: String): Flow<HabitLogEntity?>

    @Query("SELECT * FROM habit_logs WHERE date = :date AND itemId = :itemId")
    suspend fun getLog(date: LocalDate, itemId: String): HabitLogEntity?

    /** Completed, currently-active habits per day in [from]..[to] (inclusive). */
    @Query(
        """
        SELECT l.date AS date, COUNT(*) AS completedCount
        FROM habit_logs l
        INNER JOIN active_habits a ON a.itemId = l.itemId AND a.status = 'ACTIVE'
        WHERE l.isCompleted = 1 AND l.date BETWEEN :from AND :to
        GROUP BY l.date
        ORDER BY l.date
        """,
    )
    fun observeDailyCompletions(from: LocalDate, to: LocalDate): Flow<List<DailyCompletion>>

    /** The latest progress on [itemId] on any day, or null if never logged. */
    @Query("SELECT MAX(updatedAt) FROM habit_logs WHERE itemId = :itemId AND completedCount > 0")
    suspend fun getLastLoggedAt(itemId: String): Instant?

    /** Days on or after [from] on which [itemId] was completed; gaps don't reset the count. */
    @Query("SELECT COUNT(*) FROM habit_logs WHERE itemId = :itemId AND isCompleted = 1 AND date >= :from")
    suspend fun countCompletedDaysSince(itemId: String, from: LocalDate): Int

    /**
     * Adds [delta] to today's count in a single statement, so concurrent writers (a lock-screen
     * action and the Tasbih screen, say) never lose an update. The count is not capped: dhikr
     * past the target is still recorded. In DO UPDATE, bare column names refer to the existing
     * row and `excluded.*` to the values being inserted.
     */
    @Query(
        """
        INSERT INTO habit_logs
            (date, itemId, completedCount, targetCount, isCompleted, triggerSource, completedAt, updatedAt)
        VALUES
            (:date, :itemId, :delta, :targetCount, :delta >= :targetCount, :source,
             CASE WHEN :delta >= :targetCount THEN :now END, :now)
        ON CONFLICT(date, itemId) DO UPDATE SET
            completedCount = completedCount + excluded.completedCount,
            targetCount = excluded.targetCount,
            isCompleted = completedCount + excluded.completedCount >= excluded.targetCount,
            triggerSource = excluded.triggerSource,
            completedAt = COALESCE(
                completedAt,
                CASE WHEN completedCount + excluded.completedCount >= excluded.targetCount
                     THEN excluded.updatedAt END
            ),
            updatedAt = excluded.updatedAt
        """,
    )
    suspend fun incrementProgress(
        date: LocalDate,
        itemId: String,
        delta: Int,
        targetCount: Int,
        source: TriggerSource,
        now: Instant,
    )

    /** "Mark as Read": raises the count to at least [targetCount] and completes the day. */
    @Query(
        """
        INSERT INTO habit_logs
            (date, itemId, completedCount, targetCount, isCompleted, triggerSource, completedAt, updatedAt)
        VALUES
            (:date, :itemId, :targetCount, :targetCount, 1, :source, :now, :now)
        ON CONFLICT(date, itemId) DO UPDATE SET
            completedCount = MAX(completedCount, excluded.completedCount),
            targetCount = excluded.targetCount,
            isCompleted = 1,
            triggerSource = excluded.triggerSource,
            completedAt = COALESCE(completedAt, excluded.completedAt),
            updatedAt = excluded.updatedAt
        """,
    )
    suspend fun markCompleted(
        date: LocalDate,
        itemId: String,
        targetCount: Int,
        source: TriggerSource,
        now: Instant,
    )

    /** Undo: un-ticking a checklist item clears the day's progress for it. */
    @Query("DELETE FROM habit_logs WHERE date = :date AND itemId = :itemId")
    suspend fun clear(date: LocalDate, itemId: String)
}
