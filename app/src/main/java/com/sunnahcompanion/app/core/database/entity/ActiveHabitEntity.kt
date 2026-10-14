package com.sunnahcompanion.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sunnahcompanion.app.core.database.model.HabitStatus
import java.time.LocalDate

/**
 * A Sunnah the user is mastering ("Active 3") or has mastered. At most
 * `HabitMasteryPolicy.MAX_ACTIVE` rows are [HabitStatus.ACTIVE]; `ActiveHabitDao.activate`
 * enforces this. Removing a habit deletes its row; its daily history stays in [HabitLogEntity].
 */
@Entity(
    tableName = "active_habits",
    foreignKeys = [
        ForeignKey(
            entity = SunnahItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
        ),
    ],
    indices = [Index("status")],
)
data class ActiveHabitEntity(
    @PrimaryKey val itemId: String,
    val targetCount: Int,
    val status: HabitStatus,
    /** Completed days on or after this date count towards mastery. */
    val activatedOn: LocalDate,
    val masteredOn: LocalDate?,
)
