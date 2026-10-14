package com.sunnahcompanion.app.core.di

import android.content.Context
import androidx.room.Room
import com.sunnahcompanion.app.core.database.SunnahDatabase
import com.sunnahcompanion.app.core.database.dao.ActiveHabitDao
import com.sunnahcompanion.app.core.database.dao.HabitLogDao
import com.sunnahcompanion.app.core.database.dao.PersonalDuaDao
import com.sunnahcompanion.app.core.database.dao.SunnahItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * No destructive-migration fallback on purpose: habit history is user data, so every
     * schema change ships with a Migration (verified against `app/schemas`).
     */
    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): SunnahDatabase =
        Room.databaseBuilder(context, SunnahDatabase::class.java, SunnahDatabase.NAME)
            .addMigrations(*SunnahDatabase.MIGRATIONS)
            .build()

    @Provides
    fun providesSunnahItemDao(database: SunnahDatabase): SunnahItemDao = database.sunnahItemDao()

    @Provides
    fun providesHabitLogDao(database: SunnahDatabase): HabitLogDao = database.habitLogDao()

    @Provides
    fun providesActiveHabitDao(database: SunnahDatabase): ActiveHabitDao = database.activeHabitDao()

    @Provides
    fun providesPersonalDuaDao(database: SunnahDatabase): PersonalDuaDao = database.personalDuaDao()
}
