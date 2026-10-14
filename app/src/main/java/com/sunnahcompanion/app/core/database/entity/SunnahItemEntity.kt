package com.sunnahcompanion.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sunnahcompanion.app.core.database.model.ContentKind
import com.sunnahcompanion.app.core.database.model.DeedPriority
import com.sunnahcompanion.app.core.database.model.FardPrayer

/**
 * A prayer, dua, surah, adhkar or dhikr from the bundled library. Rows are owned by the seeder
 * and replaced on each seed version bump; user data lives in [ActiveHabitEntity],
 * [HabitLogEntity] and [PersonalDuaEntity].
 *
 * For a [ContentKind.SURAH], [arabicText], [transliteration] and [translation] hold the full
 * text when it is short (Ayat al-Kursi, the three Quls) and only the opening ayah as a preview
 * for long surahs; the authoritative per-verse text is in [AyahEntity].
 *
 * Columns added in schema v2 carry a [ColumnInfo.defaultValue] matching `MIGRATION_1_2`.
 */
@Entity(
    tableName = "content_items",
    indices = [Index("anchorPrayer")],
)
data class SunnahItemEntity(
    /** Stable slug, e.g. `dua_leaving_home`. Referenced by code and notifications. */
    @PrimaryKey val id: String,
    val kind: ContentKind,
    /** Stored as [DeedPriority.rank]. */
    @ColumnInfo(defaultValue = "40")
    val priority: DeedPriority,
    val title: String,
    val arabicText: String,
    val transliteration: String,
    val translation: String,
    val audioUrl: String?,
    val recommendedTargetCount: Int,
    /** One-line virtue, e.g. "Whoever says it in the evening and dies that night enters Paradise." */
    @ColumnInfo(defaultValue = "")
    val hadithVirtue: String,
    /** Source of [hadithVirtue], e.g. "Sahih al-Bukhari 6306". */
    val virtueReference: String,
    /** One practical action (sabab) or courtesy (adab) to pair with the words. */
    @ColumnInfo(defaultValue = "")
    val sababAndAdabTip: String,
    /** The fard prayer this item belongs to on the Today timeline (prayers, Witr, rawatib). */
    val anchorPrayer: FardPrayer?,
    /** Kept in the routine when "1-Minute Tired Mode" is on. */
    @ColumnInfo(defaultValue = "0")
    val isTiredModeEssential: Boolean,
    val surahNumber: Int?,
    val sortOrder: Int,
)
