package com.sunnahcompanion.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.sunnahcompanion.app.core.database.model.LifeSituation

/** Many-to-many: Ayat al-Kursi, for example, is AFTER_FARD_PRAYER, MORNING and BEFORE_SLEEP. */
@Entity(
    tableName = "item_situations",
    primaryKeys = ["itemId", "situation"],
    foreignKeys = [
        ForeignKey(
            entity = SunnahItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("situation")],
)
data class ItemSituationCrossRef(
    val itemId: String,
    val situation: LifeSituation,
)
