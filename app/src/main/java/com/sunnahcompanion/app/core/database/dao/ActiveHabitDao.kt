package com.sunnahcompanion.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.sunnahcompanion.app.core.database.entity.ActiveHabitEntity
import com.sunnahcompanion.app.core.database.model.HabitStatus
import com.sunnahcompanion.app.core.database.relation.ActiveHabitWithLog
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveHabitDao {

    /** Every habit with [status] and its progress on [date] (zeros when not started). */
    @Query(
        """
        SELECT c.*,
               a.targetCount AS habitTargetCount,
               a.status AS status,
               a.activatedOn AS activatedOn,
               a.masteredOn AS masteredOn,
               COALESCE(l.completedCount, 0) AS completedCount,
               COALESCE(l.isCompleted, 0) AS isCompleted
        FROM active_habits a
        INNER JOIN content_items c ON c.id = a.itemId
        LEFT JOIN habit_logs l ON l.itemId = a.itemId AND l.date = :date
        WHERE a.status = :status
        ORDER BY c.priority, c.sortOrder
        """,
    )
    fun observeWithLog(date: LocalDate, status: HabitStatus): Flow<List<ActiveHabitWithLog>>

    fun observeActiveWithLog(date: LocalDate): Flow<List<ActiveHabitWithLog>> =
        observeWithLog(date, HabitStatus.ACTIVE)

    @Query("SELECT * FROM active_habits WHERE status = 'MASTERED' ORDER BY masteredOn DESC")
    fun observeMastered(): Flow<List<ActiveHabitEntity>>

    @Query("SELECT COUNT(*) FROM active_habits WHERE status = 'ACTIVE'")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM active_habits WHERE status = 'ACTIVE'")
    suspend fun countActive(): Int

    @Query("SELECT * FROM active_habits WHERE itemId = :itemId")
    suspend fun get(itemId: String): ActiveHabitEntity?

    @Query("SELECT * FROM active_habits WHERE itemId = :itemId")
    fun observe(itemId: String): Flow<ActiveHabitEntity?>

    @Upsert
    suspend fun upsert(habit: ActiveHabitEntity)

    /** Seeds defaults without overwriting rows the user already has. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(habits: List<ActiveHabitEntity>)

    /**
     * Puts [itemId] into a free slot, restarting its mastery count from [today]. Returns false,
     * changing nothing, when [maxActive] habits are already active. Re-activating a habit that
     * is already active keeps its original start date.
     */
    @Transaction
    suspend fun activate(itemId: String, targetCount: Int, today: LocalDate, maxActive: Int): Boolean {
        val existing = get(itemId)
        if (existing?.status == HabitStatus.ACTIVE) return true
        if (countActive() >= maxActive) return false
        upsert(
            ActiveHabitEntity(
                itemId = itemId,
                targetCount = targetCount,
                status = HabitStatus.ACTIVE,
                activatedOn = today,
                masteredOn = null,
            ),
        )
        return true
    }

    /** Frees the slot. Daily history in `habit_logs` is kept. */
    @Query("DELETE FROM active_habits WHERE itemId = :itemId")
    suspend fun deactivate(itemId: String)

    @Query("UPDATE active_habits SET status = 'MASTERED', masteredOn = :on WHERE itemId = :itemId AND status = 'ACTIVE'")
    suspend fun markMastered(itemId: String, on: LocalDate)
}
