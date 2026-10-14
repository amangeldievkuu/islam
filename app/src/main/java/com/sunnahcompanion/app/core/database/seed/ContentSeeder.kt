package com.sunnahcompanion.app.core.database.seed

import android.content.Context
import androidx.room.withTransaction
import com.sunnahcompanion.app.core.common.AppDispatchers
import com.sunnahcompanion.app.core.common.Dispatcher
import com.sunnahcompanion.app.core.database.SunnahDatabase
import com.sunnahcompanion.app.core.database.entity.ActiveHabitEntity
import com.sunnahcompanion.app.core.database.model.HabitStatus
import com.sunnahcompanion.app.core.database.seed.SeedParser.toRows
import com.sunnahcompanion.app.core.datastore.AppPreferencesDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Writes the bundled library into Room once per [SEED_VERSION]. Idempotent and safe to call
 * from anywhere: the app start, and every background receiver before it reads content (a
 * receiver can run after a reboot before the app has ever been opened).
 *
 * Only content tables are rewritten; active habits, logs and personal duas are never touched,
 * except that the default Active 3 habits are added on the very first seed.
 */
@Singleton
class ContentSeeder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: SunnahDatabase,
    private val preferences: AppPreferencesDataSource,
    private val clock: Clock,
    @param:Dispatcher(AppDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()

    @Volatile
    private var isSeeded = false

    suspend fun ensureSeeded() {
        if (isSeeded) return
        mutex.withLock {
            if (isSeeded) return
            val previousVersion = preferences.seedVersion.first()
            if (previousVersion < SEED_VERSION) {
                seed(isFirstSeed = previousVersion == 0)
                preferences.setSeedVersion(SEED_VERSION)
            }
            isSeeded = true
        }
    }

    private suspend fun seed(isFirstSeed: Boolean) = withContext(ioDispatcher) {
        val bundle = context.assets.open(ASSET_PATH).bufferedReader().use { SeedParser.parse(it.readText()) }
        check(bundle.version == SEED_VERSION) {
            "$ASSET_PATH declares version ${bundle.version}, expected $SEED_VERSION"
        }
        val rows = bundle.toRows()
        val targets = bundle.items.associate { it.id to it.recommendedTargetCount }
        val today = LocalDate.now(clock)

        // If the process dies before the version is stored, the next launch simply re-seeds.
        database.withTransaction {
            database.sunnahItemDao().replaceContent(rows.items, rows.ayahs, rows.crossRefs)
            if (isFirstSeed) {
                database.activeHabitDao().insertIfAbsent(
                    bundle.defaultActiveHabitIds.map { id ->
                        ActiveHabitEntity(
                            itemId = id,
                            targetCount = targets.getValue(id),
                            status = HabitStatus.ACTIVE,
                            activatedOn = today,
                            masteredOn = null,
                        )
                    },
                )
            }
        }
    }

    companion object {
        /** Bump together with `version` in the asset whenever the bundled content changes. */
        const val SEED_VERSION = 3
        const val ASSET_PATH = "initial_sunnah_items.json"
    }
}
