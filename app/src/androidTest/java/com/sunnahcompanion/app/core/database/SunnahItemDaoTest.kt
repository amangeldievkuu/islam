package com.sunnahcompanion.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sunnahcompanion.app.core.database.entity.ItemSituationCrossRef
import com.sunnahcompanion.app.core.database.entity.PersonalDuaEntity
import com.sunnahcompanion.app.core.database.model.ContentKind
import com.sunnahcompanion.app.core.database.model.DeedPriority
import com.sunnahcompanion.app.core.database.model.FardPrayer
import com.sunnahcompanion.app.core.database.model.LifeSituation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SunnahItemDaoTest {

    private lateinit var db: SunnahDatabase

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SunnahDatabase::class.java,
        ).build()
        db.sunnahItemDao().replaceContent(
            items = listOf(
                // Library order deliberately disagrees with priority.
                testItem("kursi", sortOrder = 10, tiredModeEssential = true),
                testItem("witr", kind = ContentKind.PRAYER, priority = DeedPriority.WAJIB, sortOrder = 20, anchorPrayer = FardPrayer.ISHA),
                testItem("isha", kind = ContentKind.PRAYER, priority = DeedPriority.FARD, sortOrder = 30, anchorPrayer = FardPrayer.ISHA),
                testItem("mulk", sortOrder = 40),
                testItem("tasbih_fatimah", kind = ContentKind.DHIKR, sortOrder = 50, tiredModeEssential = true),
            ),
            ayahs = emptyList(),
            crossRefs = listOf(
                ItemSituationCrossRef("kursi", LifeSituation.BEFORE_SLEEP),
                ItemSituationCrossRef("mulk", LifeSituation.BEFORE_SLEEP),
                ItemSituationCrossRef("tasbih_fatimah", LifeSituation.BEFORE_SLEEP),
                ItemSituationCrossRef("witr", LifeSituation.BEFORE_SLEEP),
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun listsPutHigherPriorityFirst() = runTest {
        val dao = db.sunnahItemDao()
        assertEquals(listOf("isha", "witr"), dao.observeByAnchorPrayer(FardPrayer.ISHA).first().map { it.id })
        assertEquals(
            listOf("witr", "kursi", "mulk", "tasbih_fatimah"),
            dao.observeBySituation(LifeSituation.BEFORE_SLEEP).first().map { it.id },
        )
        assertEquals(DeedPriority.WAJIB, dao.getItem("witr")!!.priority)
    }

    @Test
    fun tiredModeKeepsOnlyEssentials() = runTest {
        assertEquals(
            listOf("kursi", "tasbih_fatimah"),
            db.sunnahItemDao().observeTiredModeEssentials().first().map { it.id },
        )
    }

    @Test
    fun personalDuas_roundTripAndFilterBySituation() = runTest {
        val dao = db.personalDuaDao()
        val id = dao.upsert(
            PersonalDuaEntity(
                title = "Ата-энем үчүн",
                text = "Оо, Алла, ата-энемди кечир жана аларга ырайым кыл.",
                languageTag = "ky",
                situation = LifeSituation.GRATITUDE,
                createdAt = millis(1_000L),
                updatedAt = millis(1_000L),
            ),
        )
        dao.upsert(dao.get(id)!!.copy(title = "Ата-эне", updatedAt = millis(2_000L)))

        val saved = dao.observeBySituation(LifeSituation.GRATITUDE).first().single()
        assertEquals("Ата-эне", saved.title)
        assertEquals("ky", saved.languageTag)
        assertEquals(millis(2_000L), saved.updatedAt)
        assertEquals(emptyList<PersonalDuaEntity>(), dao.observeBySituation(LifeSituation.ANXIOUS).first())

        dao.delete(id)
        assertNull(dao.get(id))
    }
}
