package com.sunnahcompanion.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.sunnahcompanion.app.core.database.entity.AyahEntity
import com.sunnahcompanion.app.core.database.entity.ItemSituationCrossRef
import com.sunnahcompanion.app.core.database.entity.SunnahItemEntity
import com.sunnahcompanion.app.core.database.model.ContentKind
import com.sunnahcompanion.app.core.database.model.FardPrayer
import com.sunnahcompanion.app.core.database.model.LifeSituation
import com.sunnahcompanion.app.core.database.relation.SituationItemCount
import com.sunnahcompanion.app.core.database.relation.SunnahItemWithDetails
import kotlinx.coroutines.flow.Flow

/** Read access to the bundled library. Lists are ordered Fard first, then by library order. */
@Dao
interface SunnahItemDao {

    @Query(
        """
        SELECT c.* FROM content_items c
        INNER JOIN item_situations x ON x.itemId = c.id
        WHERE x.situation = :situation
        ORDER BY c.priority, c.sortOrder
        """,
    )
    fun observeBySituation(situation: LifeSituation): Flow<List<SunnahItemEntity>>

    @Query("SELECT * FROM content_items WHERE kind = :kind ORDER BY priority, sortOrder")
    fun observeByKind(kind: ContentKind): Flow<List<SunnahItemEntity>>

    /** The prayer itself plus the Witr / rawatib anchored to it. */
    @Query("SELECT * FROM content_items WHERE anchorPrayer = :prayer ORDER BY priority, sortOrder")
    fun observeByAnchorPrayer(prayer: FardPrayer): Flow<List<SunnahItemEntity>>

    /** What remains of the pre-sleep routine in "1-Minute Tired Mode". */
    @Query("SELECT * FROM content_items WHERE isTiredModeEssential = 1 ORDER BY priority, sortOrder")
    fun observeTiredModeEssentials(): Flow<List<SunnahItemEntity>>

    @Query("SELECT situation, COUNT(*) AS itemCount FROM item_situations GROUP BY situation")
    fun observeSituationCounts(): Flow<List<SituationItemCount>>

    @Transaction
    @Query("SELECT * FROM content_items WHERE id = :id")
    fun observeItemWithDetails(id: String): Flow<SunnahItemWithDetails?>

    /** One-shot read for background receivers and notifications. */
    @Query("SELECT * FROM content_items WHERE id = :id")
    suspend fun getItem(id: String): SunnahItemEntity?

    @Transaction
    @Query("SELECT * FROM content_items WHERE id = :id")
    suspend fun getItemWithDetails(id: String): SunnahItemWithDetails?

    @Query(
        """
        SELECT * FROM content_items
        WHERE title LIKE '%' || :query || '%'
           OR transliteration LIKE '%' || :query || '%'
           OR translation LIKE '%' || :query || '%'
        ORDER BY priority, sortOrder
        """,
    )
    fun search(query: String): Flow<List<SunnahItemEntity>>

    @Query("SELECT COUNT(*) FROM content_items")
    suspend fun countItems(): Int

    @Upsert
    suspend fun upsertItems(items: List<SunnahItemEntity>)

    @Upsert
    suspend fun upsertAyahs(ayahs: List<AyahEntity>)

    @Upsert
    suspend fun upsertSituations(crossRefs: List<ItemSituationCrossRef>)

    @Query("DELETE FROM item_situations")
    suspend fun deleteAllSituations()

    @Query("DELETE FROM ayahs")
    suspend fun deleteAllAyahs()

    /**
     * Applies a seed. Uses upsert (UPDATE in place) for items, never REPLACE, because REPLACE
     * deletes the row first and would violate the habit log / active habit foreign keys.
     * Items dropped from a newer seed are kept so their history remains valid.
     */
    @Transaction
    suspend fun replaceContent(
        items: List<SunnahItemEntity>,
        ayahs: List<AyahEntity>,
        crossRefs: List<ItemSituationCrossRef>,
    ) {
        upsertItems(items)
        deleteAllAyahs()
        upsertAyahs(ayahs)
        deleteAllSituations()
        upsertSituations(crossRefs)
    }
}
