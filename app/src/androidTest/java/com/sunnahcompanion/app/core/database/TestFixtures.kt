package com.sunnahcompanion.app.core.database

import com.sunnahcompanion.app.core.database.entity.SunnahItemEntity
import com.sunnahcompanion.app.core.database.model.ContentKind
import com.sunnahcompanion.app.core.database.model.DeedPriority
import com.sunnahcompanion.app.core.database.model.FardPrayer
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

fun testItem(
    id: String,
    target: Int = 1,
    kind: ContentKind = ContentKind.DUA,
    priority: DeedPriority = DeedPriority.MUSTAHAB,
    sortOrder: Int = 0,
    anchorPrayer: FardPrayer? = null,
    tiredModeEssential: Boolean = false,
) = SunnahItemEntity(
    id = id,
    kind = kind,
    priority = priority,
    title = id,
    arabicText = "ar",
    transliteration = "tr",
    translation = "en",
    audioUrl = null,
    recommendedTargetCount = target,
    hadithVirtue = "virtue",
    virtueReference = "ref",
    sababAndAdabTip = "tip",
    anchorPrayer = anchorPrayer,
    isTiredModeEssential = tiredModeEssential,
    surahNumber = null,
    sortOrder = sortOrder,
)

fun millis(value: Long): kotlin.time.Instant = kotlin.time.Instant.fromEpochMilliseconds(value)

/** A clock tests can move forward one local day at a time. */
class MutableClock(start: LocalDate, private val zone: ZoneId = ZoneOffset.UTC) : Clock() {
    var date: LocalDate = start

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(date, zone)

    /** Noon on [date], so day arithmetic never lands on a midnight boundary. */
    override fun instant(): Instant = date.atTime(12, 0).atZone(zone).toInstant()
}
