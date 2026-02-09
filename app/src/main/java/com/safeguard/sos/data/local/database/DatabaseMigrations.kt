// app/src/main/java/com/safeguard/sos/data/local/database/DatabaseMigrations.kt

package com.safeguard.sos.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    // Example migration from version 1 to 2
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Example: Add a new column to users table
            // database.execSQL("ALTER TABLE users ADD COLUMN new_column TEXT")
        }
    }

    // Add more migrations as needed
    val ALL_MIGRATIONS = arrayOf<Migration>(
        // MIGRATION_1_2
    )
}
