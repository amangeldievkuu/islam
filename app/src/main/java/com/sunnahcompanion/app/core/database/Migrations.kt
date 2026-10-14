package com.sunnahcompanion.app.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: Deed priority, Sabab/Adab tips, life situations, "Active 3" habits and personal
 * duas. `habit_logs` is left untouched so all history survives. `item_situations` starts empty
 * and is filled by the seeder, whose version bump runs on the next launch.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Content: new columns. Defaults must match the entity's @ColumnInfo(defaultValue).
        db.execSQL("ALTER TABLE content_items ADD COLUMN priority INTEGER NOT NULL DEFAULT 40")
        db.execSQL("ALTER TABLE content_items ADD COLUMN hadithVirtue TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE content_items ADD COLUMN sababAndAdabTip TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE content_items ADD COLUMN anchorPrayer TEXT")
        db.execSQL("ALTER TABLE content_items ADD COLUMN isTiredModeEssential INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_content_items_anchorPrayer ON content_items (anchorPrayer)")

        // Categories -> situations. The enum changed, so the old rows are not carried over.
        db.execSQL("DROP TABLE item_categories")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS item_situations (
                itemId TEXT NOT NULL,
                situation TEXT NOT NULL,
                PRIMARY KEY (itemId, situation),
                FOREIGN KEY (itemId) REFERENCES content_items (id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """,
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_item_situations_situation ON item_situations (situation)")

        // Tracked habits -> Active 3: the first three enabled habits, in library order.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS active_habits (
                itemId TEXT NOT NULL,
                targetCount INTEGER NOT NULL,
                status TEXT NOT NULL,
                activatedOn INTEGER NOT NULL,
                masteredOn INTEGER,
                PRIMARY KEY (itemId),
                FOREIGN KEY (itemId) REFERENCES content_items (id) ON UPDATE NO ACTION ON DELETE NO ACTION
            )
            """,
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_active_habits_status ON active_habits (status)")
        // addedAt was epoch millis; activatedOn is the local epoch day it was added.
        db.execSQL(
            """
            INSERT INTO active_habits (itemId, targetCount, status, activatedOn, masteredOn)
            SELECT t.itemId, t.targetCount, 'ACTIVE',
                   CAST(julianday(t.addedAt / 1000, 'unixepoch', 'localtime') - 2440587.5 AS INTEGER),
                   NULL
            FROM tracked_habits t
            INNER JOIN content_items c ON c.id = t.itemId
            WHERE t.isEnabled = 1
            ORDER BY c.sortOrder
            LIMIT 3
            """,
        )
        db.execSQL("DROP TABLE tracked_habits")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS personal_duas (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                text TEXT NOT NULL,
                languageTag TEXT NOT NULL,
                situation TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """,
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_personal_duas_situation ON personal_duas (situation)")
    }
}
