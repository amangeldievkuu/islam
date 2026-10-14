package com.sunnahcompanion.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sunnahcompanion.app.core.database.model.TriggerSource
import java.time.LocalDate
import kotlin.time.Instant

/**
 * Progress on one item for one local calendar day. There is at most one row per
 * ([date], [itemId]); writes go through the atomic upserts in `HabitLogDao`.
 *
 * The foreign key deliberately has no cascade: history must survive content re-seeding.
 */
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = SunnahItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
        ),
    ],
    indices = [
        Index(value = ["date", "itemId"], unique = true),
        Index("itemId"),
    ],
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The user's local date when the progress was made. */
    val date: LocalDate,
    val itemId: String,
    val completedCount: Int,
    val targetCount: Int,
    val isCompleted: Boolean,
    /** Source of the most recent update. */
    val triggerSource: TriggerSource,
    /** When [targetCount] was first reached that day, or null. Stored as epoch millis. */
    val completedAt: Instant?,
    /** The most recent update; drives "already read in the last 3 hours". Epoch millis. */
    val updatedAt: Instant,
)
