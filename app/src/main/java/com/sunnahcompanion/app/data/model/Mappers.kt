package com.sunnahcompanion.app.data.model

import com.sunnahcompanion.app.core.database.entity.AyahEntity
import com.sunnahcompanion.app.core.database.entity.PersonalDuaEntity
import com.sunnahcompanion.app.core.database.entity.SunnahItemEntity
import com.sunnahcompanion.app.core.database.model.LifeSituation
import com.sunnahcompanion.app.core.database.relation.ActiveHabitWithLog
import com.sunnahcompanion.app.core.database.relation.SunnahItemWithDetails
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

fun SunnahItemEntity.asModel(
    situations: List<LifeSituation> = emptyList(),
    ayahs: List<AyahEntity> = emptyList(),
) = ContentItem(
    id = id,
    kind = kind,
    priority = priority,
    title = title,
    arabicText = arabicText,
    transliteration = transliteration,
    translation = translation,
    audioUrl = audioUrl,
    recommendedTargetCount = recommendedTargetCount,
    hadithVirtue = hadithVirtue,
    virtueReference = virtueReference,
    sababAndAdabTip = sababAndAdabTip,
    anchorPrayer = anchorPrayer,
    isTiredModeEssential = isTiredModeEssential,
    surahNumber = surahNumber,
    situations = if (situations.isEmpty()) persistentListOf() else situations.toImmutableList(),
    ayahs = ayahs.sortedBy { it.position }.map { it.asModel() }.toImmutableList(),
)

fun SunnahItemWithDetails.asModel() = item.asModel(
    situations = situations.map { it.situation }.sortedBy { it.ordinal },
    ayahs = ayahs,
)

fun AyahEntity.asModel() = Ayah(
    surahNumber = surahNumber,
    ayahNumber = ayahNumber,
    arabicText = arabicText,
    transliteration = transliteration,
    translation = translation,
    audioUrl = audioUrl,
)

fun ActiveHabitWithLog.asModel() = HabitItem(
    item = item.asModel(),
    targetCount = habitTargetCount,
    completedCount = completedCount,
    isCompleted = isCompleted,
    status = status,
    activatedOn = activatedOn,
    masteredOn = masteredOn,
)

fun PersonalDuaEntity.asModel() = PersonalDua(
    id = id,
    title = title,
    text = text,
    languageTag = languageTag,
    situation = situation,
)
