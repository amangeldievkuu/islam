package com.sunnahcompanion.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * One verse of a [SunnahItemEntity] of kind SURAH. An item may span several surahs
 * (e.g. the three Quls), so verses are ordered by [position] rather than [ayahNumber].
 */
@Entity(
    tableName = "ayahs",
    primaryKeys = ["itemId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = SunnahItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("surahNumber", "ayahNumber")],
)
data class AyahEntity(
    val itemId: String,
    val position: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val arabicText: String,
    val transliteration: String,
    val translation: String,
    val audioUrl: String?,
)
