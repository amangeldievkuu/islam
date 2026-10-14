package com.sunnahcompanion.app.core.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sunnahcompanion.app.core.database.model.DeedPriority
import com.sunnahcompanion.app.core.database.model.HabitStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), SunnahDatabase::class.java)

    @Test
    fun migrate1To2_keepsHistoryAndMovesFirstThreeTrackedHabits() = runTest {
        helper.createDatabase(TEST_DB, 1).apply {
            // Five items; library order is the reverse of insertion order.
            ITEMS.forEachIndexed { index, id ->
                insert("content_items", SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
                    put("id", id)
                    put("kind", "DUA")
                    put("title", id)
                    put("arabicText", "ar")
                    put("transliteration", "tr")
                    put("translation", "en")
                    put("recommendedTargetCount", 1)
                    put("virtueReference", "ref")
                    put("sortOrder", (ITEMS.size - index) * 10)
                })
                insert("item_categories", SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
                    put("itemId", id)
                    put("category", "MORNING_EVENING")
                })
            }
            // All tracked except item 2, which is disabled.
            ITEMS.forEachIndexed { index, id ->
                insert("tracked_habits", SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
                    put("itemId", id)
                    put("targetCount", index + 1)
                    put("isEnabled", if (index == 2) 0 else 1)
                    put("addedAt", ADDED_AT)
                })
            }
            listOf(0L, 1L, 2L).forEach { day ->
                insert("habit_logs", SQLiteDatabase.CONFLICT_FAIL, ContentValues().apply {
                    put("date", LOG_DATE.minusDays(day).toEpochDay())
                    put("itemId", ITEMS[0])
                    put("completedCount", 1)
                    put("targetCount", 1)
                    put("isCompleted", 1)
                    put("triggerSource", "GEOFENCE")
                    put("completedAt", ADDED_AT + day)
                    put("updatedAt", ADDED_AT + day)
                })
            }
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, SunnahDatabase::class.java, TEST_DB)
            .addMigrations(*SunnahDatabase.MIGRATIONS)
            .build()
        try {
            // Enabled habits in library order: ITEMS[4], [3], [1] ([2] is disabled), capped at three.
            val active = listOf(ITEMS[4], ITEMS[3], ITEMS[1]).map { db.activeHabitDao().get(it)!! }
            val expectedDay = Instant.ofEpochMilli(ADDED_AT).atZone(ZoneId.systemDefault()).toLocalDate()
            active.forEach {
                assertEquals(HabitStatus.ACTIVE, it.status)
                assertEquals(expectedDay, it.activatedOn)
            }
            assertEquals(5, db.activeHabitDao().get(ITEMS[4])!!.targetCount)
            assertEquals(3, db.activeHabitDao().countActive())

            assertEquals(3, db.habitLogDao().countCompletedDaysSince(ITEMS[0], LOG_DATE.minusDays(30)))
            assertEquals(kotlin.time.Instant.fromEpochMilliseconds(ADDED_AT + 2), db.habitLogDao().getLastLoggedAt(ITEMS[0]))

            db.sunnahItemDao().getItem(ITEMS[0])!!.let {
                assertEquals(DeedPriority.MUSTAHAB, it.priority)
                assertEquals("", it.sababAndAdabTip)
                assertFalse(it.isTiredModeEssential)
            }
        } finally {
            db.close()
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val ADDED_AT = 1_790_000_000_000L
        val LOG_DATE: LocalDate = LocalDate.of(2026, 10, 14)
        val ITEMS = listOf("dua_a", "dua_b", "dua_c", "dua_d", "dua_e")
    }
}
