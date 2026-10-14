package com.sunnahcompanion.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sunnahcompanion.app.core.database.model.HabitStatus
import com.sunnahcompanion.app.core.database.model.TriggerSource
import com.sunnahcompanion.app.core.time.TimeProvider
import com.sunnahcompanion.app.data.model.ActivationResult
import com.sunnahcompanion.app.data.model.HabitMasteryPolicy
import com.sunnahcompanion.app.data.repository.HabitRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActiveHabitDaoTest {

    private lateinit var db: SunnahDatabase
    private lateinit var repository: HabitRepository
    private val start = LocalDate.of(2026, 10, 1)
    private val clock = MutableClock(start)

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            SunnahDatabase::class.java,
        ).build()
        db.sunnahItemDao().replaceContent(
            items = IDS.mapIndexed { index, id -> testItem(id, target = if (id == DHIKR) 33 else 1, sortOrder = index) },
            ayahs = emptyList(),
            crossRefs = emptyList(),
        )
        repository = HabitRepository(
            database = db,
            habitLogDao = db.habitLogDao(),
            activeHabitDao = db.activeHabitDao(),
            sunnahItemDao = db.sunnahItemDao(),
            time = TimeProvider(clock),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun activate_capsAtThreeSlots() = runTest {
        assertEquals(ActivationResult.ACTIVATED, repository.activate(IDS[0]))
        assertEquals(ActivationResult.ACTIVATED, repository.activate(IDS[1]))
        assertEquals(ActivationResult.ACTIVATED, repository.activate(IDS[2]))
        assertFalse(repository.observeHasFreeSlot().first())

        assertEquals(ActivationResult.SLOTS_FULL, repository.activate(IDS[3]))
        assertNull(db.activeHabitDao().get(IDS[3]))
        // Re-activating an active habit is a no-op, not a slot error.
        assertEquals(ActivationResult.ACTIVATED, repository.activate(IDS[0]))

        repository.deactivate(IDS[1])
        assertTrue(repository.observeHasFreeSlot().first())
        assertEquals(ActivationResult.ACTIVATED, repository.activate(IDS[3]))
        assertEquals(HabitMasteryPolicy.MAX_ACTIVE, db.activeHabitDao().countActive())
    }

    @Test
    fun activeHabits_joinTodaysProgressAndCountDailyCompletions() = runTest {
        repository.activate(IDS[0])
        repository.activate(DHIKR)
        repository.markCompleted(IDS[0], TriggerSource.IN_APP)
        repository.increment(DHIKR, 10, TriggerSource.TASBIH)
        // Logged but not an active habit: excluded from the grid.
        repository.markCompleted(IDS[3], TriggerSource.IN_APP)

        val habits = repository.observeActiveHabits(start).first().associateBy { it.item.id }
        assertTrue(habits.getValue(IDS[0]).isCompleted)
        assertEquals(10, habits.getValue(DHIKR).completedCount)
        assertEquals(33, habits.getValue(DHIKR).targetCount)
        assertFalse(habits.getValue(DHIKR).isCompleted)

        val week = repository.observeWeek(start).first()
        assertEquals(1, week.last().completed)
        assertEquals(2, week.last().total)
    }

    @Test
    fun habitGraduatesOnTwentyFirstCompletedDay_evenWithMissedDays() = runTest {
        repository.activate(IDS[0])
        // Complete every other day: missed days never reset progress.
        repeat(HabitMasteryPolicy.MASTERY_DAYS - 1) {
            repository.markCompleted(IDS[0], TriggerSource.IN_APP)
            clock.date = clock.date.plusDays(2)
        }
        assertEquals(HabitStatus.ACTIVE, db.activeHabitDao().get(IDS[0])!!.status)

        repository.markCompleted(IDS[0], TriggerSource.IN_APP)
        db.activeHabitDao().get(IDS[0])!!.let {
            assertEquals(HabitStatus.MASTERED, it.status)
            assertEquals(clock.date, it.masteredOn)
        }
        assertEquals(0, db.activeHabitDao().countActive())
        assertEquals(listOf(IDS[0]), repository.observeMasteredHabits(clock.date).first().map { it.item.id })
    }

    @Test
    fun daysCompletedBeforeActivationDoNotCount() = runTest {
        repeat(HabitMasteryPolicy.MASTERY_DAYS) {
            repository.markCompleted(IDS[0], TriggerSource.IN_APP)
            clock.date = clock.date.plusDays(1)
        }
        repository.activate(IDS[0])
        repository.markCompleted(IDS[0], TriggerSource.IN_APP)
        assertEquals(HabitStatus.ACTIVE, db.activeHabitDao().get(IDS[0])!!.status)
    }

    @Test
    fun reactivatingMasteredHabitRestartsItsCount() = runTest {
        repository.activate(IDS[0])
        db.activeHabitDao().markMastered(IDS[0], start)
        clock.date = start.plusDays(3)

        assertEquals(ActivationResult.ACTIVATED, repository.activate(IDS[0]))
        db.activeHabitDao().get(IDS[0])!!.let {
            assertEquals(HabitStatus.ACTIVE, it.status)
            assertEquals(start.plusDays(3), it.activatedOn)
            assertNull(it.masteredOn)
        }
    }

    private companion object {
        const val DHIKR = "dhikr_subhanallah"
        val IDS = listOf("dua_leaving_home", "quran_ayat_al_kursi", "quran_al_mulk", "dua_entering_home", DHIKR)
    }
}
