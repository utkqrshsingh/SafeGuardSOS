// app/src/main/java/com/safeguard/sos/di/DatabaseModule.kt

package com.safeguard.sos.di

import android.content.Context
import androidx.room.Room
import com.safeguard.sos.data.local.dao.EmergencyContactDao
import com.safeguard.sos.data.local.dao.HelperDao
import com.safeguard.sos.data.local.dao.SOSHistoryDao
import com.safeguard.sos.data.local.dao.UserDao
import com.safeguard.sos.data.local.database.SafeGuardDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SafeGuardDatabase {
        return Room.databaseBuilder(
            context,
            SafeGuardDatabase::class.java,
            SafeGuardDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            // .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: SafeGuardDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideEmergencyContactDao(database: SafeGuardDatabase): EmergencyContactDao {
        return database.emergencyContactDao()
    }

    @Provides
    @Singleton
    fun provideSOSHistoryDao(database: SafeGuardDatabase): SOSHistoryDao {
        return database.sosHistoryDao()
    }

    @Provides
    @Singleton
    fun provideHelperDao(database: SafeGuardDatabase): HelperDao {
        return database.helperDao()
    }
}