package com.sunnahcompanion.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sunnahcompanion.app.core.database.entity.ItemSituationCrossRef
import com.sunnahcompanion.app.core.database.model.LifeSituation
import com.sunnahcompanion.app.core.database.model.TriggerSource
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitLogDaoTest {

    private lateinit var db: SunnahDatabase
    private val today = LocalDate.of(2026, 10, 14)

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SunnahDatabase::class.java,
        ).build()
        db.sunnahItemDao().replaceContent(
            items = listOf(testItem(DUA_ID, target = 1), testItem(DHIKR_ID, target = 33), testItem(KURSI_ID, target = 1)),
            ayahs = emptyList(),
            crossRefs = listOf(
                ItemSituationCrossRef(KURSI_ID, LifeSituation.AFTER_FARD_PRAYER),
                ItemSituationCrossRef(KURSI_ID, LifeSituation.BEFORE_SLEEP),
                ItemSituationCrossRef(DUA_ID, LifeSituation.LEAVING_HOME),
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun incrementProgress_completesAtTargetAndKeepsCounting() = runTest {
        val dao = db.habitLogDao()
        repeat(32) { dao.incrementProgress(today, DHIKR_ID, 1, 33, TriggerSource.TASBIH, now = millis(1_000L + it)) }
        dao.getLog(today, DHIKR_ID)!!.let {
            assertEquals(32, it.completedCount)
            assertFalse(it.isCompleted)
            assertNull(it.completedAt)
        }

        dao.incrementProgress(today, DHIKR_ID, 1, 33, TriggerSource.TASBIH, now = millis(5_000L))
        dao.incrementProgress(today, DHIKR_ID, 1, 33, TriggerSource.TASBIH, now = millis(6_000L))
        dao.getLog(today, DHIKR_ID)!!.let {
            assertEquals(34, it.completedCount)
            assertTrue(it.isCompleted)
            assertEquals(millis(5_000L), it.completedAt)
            assertEquals(millis(6_000L), it.updatedAt)
        }
    }

    @Test
    fun incrementProgress_concurrentWritersLoseNothing() = runTest {
        val dao = db.habitLogDao()
        withContext(Dispatchers.IO) {
            (1..50).map { i ->
                async { dao.incrementProgress(today, DHIKR_ID, 1, 33, TriggerSource.TASBIH, now = millis(i.toLong())) }
            }.awaitAll()
        }
        assertEquals(50, dao.getLog(today, DHIKR_ID)!!.completedCount)
    }

    @Test
    fun markCompleted_raisesToTargetWithoutLoweringHigherCount() = runTest {
        val dao = db.habitLogDao()
        dao.markCompleted(today, DUA_ID, 1, TriggerSource.NOTIFICATION, now = millis(10L))
        dao.getLog(today, DUA_ID)!!.let {
            assertEquals(1, it.completedCount)
            assertTrue(it.isCompleted)
            assertEquals(TriggerSource.NOTIFICATION, it.triggerSource)
        }

        repeat(40) { dao.incrementProgress(today, DHIKR_ID, 1, 33, TriggerSource.TASBIH, now = millis(20L)) }
        dao.markCompleted(today, DHIKR_ID, 33, TriggerSource.IN_APP, now = millis(30L))
        assertEquals(40, dao.getLog(today, DHIKR_ID)!!.completedCount)
    }

    @Test
    fun getLastLoggedAt_returnsLatestUpdateAcrossDays() = runTest {
        val dao = db.habitLogDao()
        assertNull(dao.getLastLoggedAt(DUA_ID))
        dao.markCompleted(today.minusDays(1), DUA_ID, 1, TriggerSource.GEOFENCE, now = millis(100L))
        dao.markCompleted(today, DUA_ID, 1, TriggerSource.NOTIFICATION, now = millis(200L))
        assertEquals(millis(200L), dao.getLastLoggedAt(DUA_ID))
    }

    @Test
    fun countCompletedDaysSince_ignoresGapsPartialDaysAndEarlierDays() = runTest {
        val dao = db.habitLogDao()
        dao.markCompleted(today.minusDays(10), DUA_ID, 1, TriggerSource.IN_APP, now = millis(1L))
        dao.markCompleted(today.minusDays(5), DUA_ID, 1, TriggerSource.IN_APP, now = millis(2L))
        dao.markCompleted(today.minusDays(2), DUA_ID, 1, TriggerSource.IN_APP, now = millis(3L))
        dao.incrementProgress(today, DHIKR_ID, 10, 33, TriggerSource.TASBIH, now = millis(4L))

        assertEquals(2, dao.countCompletedDaysSince(DUA_ID, today.minusDays(5)))
        assertEquals(0, dao.countCompletedDaysSince(DHIKR_ID, today.minusDays(5)))
    }

    @Test
    fun crossRefs_returnSharedItemUnderBothSituations() = runTest {
        val items = db.sunnahItemDao()
        assertEquals(listOf(KURSI_ID), items.observeBySituation(LifeSituation.AFTER_FARD_PRAYER).first().map { it.id })
        assertEquals(listOf(KURSI_ID), items.observeBySituation(LifeSituation.BEFORE_SLEEP).first().map { it.id })
        val details = items.observeItemWithDetails(KURSI_ID).first()
        assertEquals(2, details!!.situations.size)
    }

    @Test
    fun reseeding_preservesHabitHistory() = runTest {
        db.habitLogDao().markCompleted(today, DUA_ID, 1, TriggerSource.GEOFENCE, now = millis(1L))
        db.sunnahItemDao().replaceContent(
            items = listOf(testItem(DUA_ID).copy(title = "Updated"), testItem(DHIKR_ID, 33), testItem(KURSI_ID)),
            ayahs = emptyList(),
            crossRefs = emptyList(),
        )
        assertNotNull(db.habitLogDao().getLog(today, DUA_ID))
        assertEquals("Updated", db.sunnahItemDao().getItem(DUA_ID)!!.title)
    }

    private companion object {
        const val DUA_ID = "dua_leaving_home"
        const val DHIKR_ID = "dhikr_subhanallah"
        const val KURSI_ID = "quran_ayat_al_kursi"
    }
}
