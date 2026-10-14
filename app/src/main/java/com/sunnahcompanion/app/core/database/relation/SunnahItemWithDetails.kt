package com.sunnahcompanion.app.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.sunnahcompanion.app.core.database.entity.AyahEntity
import com.sunnahcompanion.app.core.database.entity.ItemSituationCrossRef
import com.sunnahcompanion.app.core.database.entity.SunnahItemEntity

/** Everything the reader screen needs for one item. [ayahs] is unordered; sort by position. */
data class SunnahItemWithDetails(
    @Embedded val item: SunnahItemEntity,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val ayahs: List<AyahEntity>,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val situations: List<ItemSituationCrossRef>,
)
