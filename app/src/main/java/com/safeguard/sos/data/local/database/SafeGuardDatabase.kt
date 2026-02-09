// app/src/main/java/com/safeguard/sos/data/local/database/SafeGuardDatabase.kt

package com.safeguard.sos.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.safeguard.sos.data.local.dao.EmergencyContactDao
import com.safeguard.sos.data.local.dao.HelperDao
import com.safeguard.sos.data.local.dao.SOSHistoryDao
import com.safeguard.sos.data.local.dao.UserDao
import com.safeguard.sos.data.local.entity.EmergencyContactEntity
import com.safeguard.sos.data.local.entity.HelperEntity
import com.safeguard.sos.data.local.entity.SOSHistoryEntity
import com.safeguard.sos.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        EmergencyContactEntity::class,
        SOSHistoryEntity::class,
        HelperEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SafeGuardDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun sosHistoryDao(): SOSHistoryDao
    abstract fun helperDao(): HelperDao

    companion object {
        const val DATABASE_NAME = "safeguard_database"
    }
}