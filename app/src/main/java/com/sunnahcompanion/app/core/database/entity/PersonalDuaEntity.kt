package com.sunnahcompanion.app.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sunnahcompanion.app.core.database.model.LifeSituation
import kotlin.time.Instant

/** "Жүрөктөгү дубалар": a supplication the user wrote in their own words and language. */
@Entity(
    tableName = "personal_duas",
    indices = [Index("situation")],
)
data class PersonalDuaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val text: String,
    /** BCP-47 tag of [text], e.g. `ky`, `ru`, `en`; drives text direction and fonts. */
    val languageTag: String,
    /** Optional situation the dua is surfaced with, e.g. ANXIOUS. */
    val situation: LifeSituation?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
