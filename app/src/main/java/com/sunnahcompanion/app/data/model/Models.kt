package com.sunnahcompanion.app.data.model

import androidx.compose.runtime.Immutable
import com.sunnahcompanion.app.core.database.model.ContentKind
import com.sunnahcompanion.app.core.database.model.DeedPriority
import com.sunnahcompanion.app.core.database.model.FardPrayer
import com.sunnahcompanion.app.core.database.model.HabitStatus
import com.sunnahcompanion.app.core.database.model.LifeSituation
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Well-known content ids referenced by triggers and reminders. */
object ContentIds {
    const val LEAVING_HOME = "dua_leaving_home"
    const val RIDING_VEHICLE = "dua_riding_vehicle"
    const val AL_MULK = "quran_al_mulk"
    const val AL_KAHF = "quran_al_kahf"
}

@Immutable
data class Ayah(
    val surahNumber: Int,
    val ayahNumber: Int,
    val arabicText: String,
    val transliteration: String,
    val translation: String,
    val audioUrl: String?,
)

@Immutable
data class ContentItem(
    val id: String,
    val kind: ContentKind,
    val priority: DeedPriority,
    val title: String,
    val arabicText: String,
    val transliteration: String,
    val translation: String,
    val audioUrl: String?,
    val recommendedTargetCount: Int,
    val hadithVirtue: String,
    val virtueReference: String,
    val sababAndAdabTip: String,
    val anchorPrayer: FardPrayer?,
    val isTiredModeEssential: Boolean,
    val surahNumber: Int?,
    val situations: ImmutableList<LifeSituation> = persistentListOf(),
    /** For a SURAH, its verses; for a DUA, the Quranic verses it quotes (used for audio). */
    val ayahs: ImmutableList<Ayah> = persistentListOf(),
) {
    /** Ordered audio URLs: the whole-item recording if there is one, else one per ayah. */
    val audioUrls: List<String>
        get() = audioUrl?.let(::listOf) ?: ayahs.mapNotNull { it.audioUrl }

    val hasAudio: Boolean get() = audioUrls.isNotEmpty()
}

/** An Active 3 (or mastered) habit with today's progress. */
@Immutable
data class HabitItem(
    val item: ContentItem,
    val targetCount: Int,
    val completedCount: Int,
    val isCompleted: Boolean,
    val status: HabitStatus,
    val activatedOn: LocalDate,
    val masteredOn: LocalDate?,
)

/** Result of asking for a free Active 3 slot. */
enum class ActivationResult { ACTIVATED, SLOTS_FULL }

@Immutable
data class DayProgress(
    val date: LocalDate,
    val completed: Int,
    val total: Int,
) {
    val fraction: Float get() = if (total == 0) 0f else (completed.toFloat() / total).coerceIn(0f, 1f)
}

@Immutable
data class ItemProgress(
    val completedCount: Int,
    val targetCount: Int,
    val isCompleted: Boolean,
)

@Immutable
data class PersonalDua(
    val id: Long,
    val title: String,
    val text: String,
    val languageTag: String,
    val situation: LifeSituation?,
)
