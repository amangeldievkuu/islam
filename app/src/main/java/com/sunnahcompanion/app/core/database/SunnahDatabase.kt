package com.sunnahcompanion.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sunnahcompanion.app.core.database.dao.ActiveHabitDao
import com.sunnahcompanion.app.core.database.dao.HabitLogDao
import com.sunnahcompanion.app.core.database.dao.PersonalDuaDao
import com.sunnahcompanion.app.core.database.dao.SunnahItemDao
import com.sunnahcompanion.app.core.database.entity.ActiveHabitEntity
import com.sunnahcompanion.app.core.database.entity.AyahEntity
import com.sunnahcompanion.app.core.database.entity.HabitLogEntity
import com.sunnahcompanion.app.core.database.entity.ItemSituationCrossRef
import com.sunnahcompanion.app.core.database.entity.PersonalDuaEntity
import com.sunnahcompanion.app.core.database.entity.SunnahItemEntity

@Database(
    entities = [
        SunnahItemEntity::class,
        AyahEntity::class,
        ItemSituationCrossRef::class,
        ActiveHabitEntity::class,
        HabitLogEntity::class,
        PersonalDuaEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SunnahDatabase : RoomDatabase() {
    abstract fun sunnahItemDao(): SunnahItemDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun activeHabitDao(): ActiveHabitDao
    abstract fun personalDuaDao(): PersonalDuaDao

    companion object {
        const val NAME = "sunnah_companion.db"

        val MIGRATIONS = arrayOf(MIGRATION_1_2)
    }
}
