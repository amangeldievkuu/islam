package com.sunnahcompanion.app.core.database.relation

import androidx.room.Embedded
import com.sunnahcompanion.app.core.database.entity.SunnahItemEntity
import com.sunnahcompanion.app.core.database.model.HabitStatus
import com.sunnahcompanion.app.core.database.model.LifeSituation
import java.time.LocalDate

/** An Active 3 habit joined with its log for a single day (zeros when not started). */
data class ActiveHabitWithLog(
    @Embedded val item: SunnahItemEntity,
    val habitTargetCount: Int,
    val status: HabitStatus,
    val activatedOn: LocalDate,
    val masteredOn: LocalDate?,
    val completedCount: Int,
    val isCompleted: Boolean,
)

data class SituationItemCount(
    val situation: LifeSituation,
    val itemCount: Int,
)

/** Number of active habits completed on [date]; feeds the 7-day consistency grid. */
data class DailyCompletion(
    val date: LocalDate,
    val completedCount: Int,
)
