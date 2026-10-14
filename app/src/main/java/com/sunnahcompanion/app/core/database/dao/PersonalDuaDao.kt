package com.sunnahcompanion.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sunnahcompanion.app.core.database.entity.PersonalDuaEntity
import com.sunnahcompanion.app.core.database.model.LifeSituation
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalDuaDao {

    @Query("SELECT * FROM personal_duas ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PersonalDuaEntity>>

    @Query("SELECT * FROM personal_duas WHERE situation = :situation ORDER BY updatedAt DESC")
    fun observeBySituation(situation: LifeSituation): Flow<List<PersonalDuaEntity>>

    @Query("SELECT * FROM personal_duas WHERE id = :id")
    suspend fun get(id: Long): PersonalDuaEntity?

    /** Returns the row id; for an update this is the existing id, for an insert the new one. */
    @Upsert
    suspend fun upsert(dua: PersonalDuaEntity): Long

    @Query("DELETE FROM personal_duas WHERE id = :id")
    suspend fun delete(id: Long)
}
