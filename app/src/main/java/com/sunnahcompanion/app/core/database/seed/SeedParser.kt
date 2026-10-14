package com.sunnahcompanion.app.core.database.seed

import com.sunnahcompanion.app.core.database.entity.AyahEntity
import com.sunnahcompanion.app.core.database.entity.ItemSituationCrossRef
import com.sunnahcompanion.app.core.database.entity.SunnahItemEntity
import kotlinx.serialization.json.Json

/** Rows ready to hand to `SunnahItemDao.replaceContent`. */
data class SeedRows(
    val items: List<SunnahItemEntity>,
    val ayahs: List<AyahEntity>,
    val crossRefs: List<ItemSituationCrossRef>,
)

/** Pure JSON -> entity mapping, kept free of Android types so it runs in JVM unit tests. */
object SeedParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): SeedBundle = json.decodeFromString(SeedBundle.serializer(), text)

    fun SeedBundle.toRows(): SeedRows = SeedRows(
        items = items.map { it.toEntity() },
        ayahs = items.flatMap { item ->
            item.ayahs.mapIndexed { index, ayah ->
                AyahEntity(
                    itemId = item.id,
                    position = index,
                    surahNumber = ayah.surahNumber,
                    ayahNumber = ayah.ayahNumber,
                    arabicText = ayah.arabicText,
                    transliteration = ayah.transliteration,
                    translation = ayah.translation,
                    audioUrl = ayah.audioUrl,
                )
            }
        },
        crossRefs = items.flatMap { item ->
            item.situations.distinct().map { ItemSituationCrossRef(itemId = item.id, situation = it) }
        },
    )

    private fun SeedItem.toEntity() = SunnahItemEntity(
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
        isTiredModeEssential = tiredModeEssential,
        surahNumber = surahNumber,
        sortOrder = sortOrder,
    )
}
