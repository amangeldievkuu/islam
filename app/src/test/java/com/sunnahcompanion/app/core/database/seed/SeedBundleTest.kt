package com.sunnahcompanion.app.core.database.seed

import com.sunnahcompanion.app.core.database.model.ContentKind
import com.sunnahcompanion.app.core.database.model.DeedPriority
import com.sunnahcompanion.app.core.database.model.FardPrayer
import com.sunnahcompanion.app.core.database.model.LifeSituation
import com.sunnahcompanion.app.core.database.seed.SeedParser.toRows
import com.sunnahcompanion.app.data.model.ContentIds
import com.sunnahcompanion.app.data.model.HabitMasteryPolicy
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the committed seed asset against regeneration mistakes. */
class SeedBundleTest {

    // Local unit tests run with the module directory as the working directory.
    private val bundle = SeedParser.parse(File("src/main/assets/${ContentSeeder.ASSET_PATH}").readText())
    private val itemsById = bundle.items.associateBy { it.id }

    @Test
    fun versionMatchesSeeder() {
        assertEquals(ContentSeeder.SEED_VERSION, bundle.version)
    }

    @Test
    fun idsAreUnique() {
        assertEquals(bundle.items.size, itemsById.size)
    }

    @Test
    fun everySituationHasContent() {
        val used = bundle.items.flatMap { it.situations }.toSet()
        assertEquals(LifeSituation.entries.toSet(), used)
    }

    @Test
    fun wellKnownIdsExist() {
        listOf(ContentIds.LEAVING_HOME, ContentIds.RIDING_VEHICLE, ContentIds.AL_MULK, ContentIds.AL_KAHF)
            .forEach { assertTrue(it, it in itemsById) }
    }

    @Test
    fun defaultActiveHabitsFitTheSlots() {
        assertTrue(bundle.defaultActiveHabitIds.size <= HabitMasteryPolicy.MAX_ACTIVE)
        assertTrue(bundle.defaultActiveHabitIds.all { it in itemsById })
    }

    @Test
    fun itemsAreComplete() {
        bundle.items.forEach { item ->
            assertTrue("${item.id} target", item.recommendedTargetCount > 0)
            assertTrue("${item.id} arabic", item.arabicText.isNotBlank())
            assertTrue("${item.id} transliteration", item.transliteration.isNotBlank())
            assertTrue("${item.id} translation", item.translation.isNotBlank())
            assertTrue("${item.id} virtue", item.hadithVirtue.isNotBlank())
            assertTrue("${item.id} reference", item.virtueReference.isNotBlank())
            assertTrue("${item.id} sabab & adab", item.sababAndAdabTip.isNotBlank())
            if (item.kind == ContentKind.SURAH) assertTrue("${item.id} ayahs", item.ayahs.isNotEmpty())
            if (item.kind == ContentKind.PRAYER) {
                assertTrue("${item.id} anchor", item.anchorPrayer != null)
            } else if (item.kind != ContentKind.DHIKR) {
                assertTrue("${item.id} situations", item.situations.isNotEmpty())
            }
        }
    }

    @Test
    fun fiveFardPrayersAnchorTheDay() {
        val fard = bundle.items.filter { it.priority == DeedPriority.FARD }
        assertTrue(fard.all { it.kind == ContentKind.PRAYER })
        assertEquals(FardPrayer.entries.toSet(), fard.map { it.anchorPrayer }.toSet())
        assertEquals(FardPrayer.entries.size, fard.size)
    }

    @Test
    fun tiredModeKeepsAyatAlKursiAndTasbihFatimah() {
        val essentials = bundle.items.filter { it.tiredModeEssential }
        assertEquals(setOf("quran_ayat_al_kursi", "dhikr_tasbih_fatimah"), essentials.map { it.id }.toSet())
        assertTrue(essentials.all { LifeSituation.BEFORE_SLEEP in it.situations })
        assertEquals(100, itemsById.getValue("dhikr_tasbih_fatimah").recommendedTargetCount)
    }

    @Test
    fun surahsHaveCanonicalAyahCounts() {
        assertEquals(30, itemsById.getValue("quran_al_mulk").ayahs.size)
        assertEquals(96, itemsById.getValue("quran_al_waqiah").ayahs.size)
        assertEquals(110, itemsById.getValue("quran_al_kahf").ayahs.size)
        assertEquals(15, itemsById.getValue("quran_three_quls").ayahs.size)
        assertEquals(1, itemsById.getValue("quran_ayat_al_kursi").ayahs.size)
        assertEquals(2, itemsById.getValue("quran_al_baqarah_last_two").ayahs.size)
    }

    @Test
    fun basmalaIsNotPartOfAyahOne() {
        val mulkFirst = itemsById.getValue("quran_al_mulk").ayahs.first()
        assertTrue(mulkFirst.arabicText.startsWith("تَبَٰرَكَ"))
    }

    @Test
    fun toRowsKeepsAyahOrderAndSharedSituations() {
        val rows = bundle.toRows()
        val quls = rows.ayahs.filter { it.itemId == "quran_three_quls" }
        assertEquals((0 until 15).toList(), quls.map { it.position })
        assertEquals(listOf(112, 113, 114), quls.map { it.surahNumber }.distinct())

        val kursiSituations = rows.crossRefs.filter { it.itemId == "quran_ayat_al_kursi" }.map { it.situation }
        assertEquals(
            setOf(LifeSituation.AFTER_FARD_PRAYER, LifeSituation.MORNING, LifeSituation.EVENING, LifeSituation.BEFORE_SLEEP),
            kursiSituations.toSet(),
        )
        assertEquals(DeedPriority.WAJIB, rows.items.single { it.id == "prayer_witr" }.priority)
    }
}
