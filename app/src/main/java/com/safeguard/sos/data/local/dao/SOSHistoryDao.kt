// app/src/main/java/com/safeguard/sos/data/local/dao/SOSHistoryDao.kt

package com.safeguard.sos.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.safeguard.sos.data.local.entity.SOSHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SOSHistoryDao {

    @Query("SELECT * FROM sos_history WHERE user_id = :userId ORDER BY created_at DESC")
    fun getHistoryByUserId(userId: String): Flow<List<SOSHistoryEntity>>

    @Query("SELECT * FROM sos_history WHERE user_id = :userId ORDER BY created_at DESC")
    fun getHistoryByUserIdPaged(userId: String): PagingSource<Int, SOSHistoryEntity>

    @Query("SELECT * FROM sos_history WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentHistory(userId: String, limit: Int = 10): List<SOSHistoryEntity>

    @Query("SELECT * FROM sos_history WHERE id = :sosId LIMIT 1")
    suspend fun getSOSById(sosId: String): SOSHistoryEntity?

    @Query("SELECT * FROM sos_history WHERE id = :sosId LIMIT 1")
    fun getSOSByIdFlow(sosId: String): Flow<SOSHistoryEntity?>

    @Query("SELECT * FROM sos_history WHERE user_id = :userId AND status IN ('pending', 'active') ORDER BY created_at DESC LIMIT 1")
    suspend fun getActiveSOSByUserId(userId: String): SOSHistoryEntity?

    @Query("SELECT * FROM sos_history WHERE user_id = :userId AND status IN ('pending', 'active') ORDER BY created_at DESC LIMIT 1")
    fun getActiveSOSByUserIdFlow(userId: String): Flow<SOSHistoryEntity?>

    @Query("SELECT * FROM sos_history WHERE status = :status ORDER BY created_at DESC")
    fun getSOSByStatus(status: String): Flow<List<SOSHistoryEntity>>

    @Query("SELECT COUNT(*) FROM sos_history WHERE user_id = :userId")
    suspend fun getSOSCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM sos_history WHERE user_id = :userId AND is_false_alarm = 1")
    suspend fun getFalseAlarmCount(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSOS(sos: SOSHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sosList: List<SOSHistoryEntity>)

    @Update
    suspend fun updateSOS(sos: SOSHistoryEntity)

    @Delete
    suspend fun deleteSOS(sos: SOSHistoryEntity)

    @Query("DELETE FROM sos_history WHERE id = :sosId")
    suspend fun deleteSOSById(sosId: String)

    @Query("DELETE FROM sos_history WHERE user_id = :userId")
    suspend fun deleteAllByUserId(userId: String)

    @Query("DELETE FROM sos_history")
    suspend fun deleteAll()

    @Query("UPDATE sos_history SET status = :status, updated_at = :updatedAt WHERE id = :sosId")
    suspend fun updateSOSStatus(sosId: String, status: String, updatedAt: Long)

    @Query("UPDATE sos_history SET status = 'resolved', resolved_by = :resolvedBy, resolved_at = :resolvedAt, updated_at = :resolvedAt WHERE id = :sosId")
    suspend fun resolveSOS(sosId: String, resolvedBy: String?, resolvedAt: Long)

    @Query("UPDATE sos_history SET is_false_alarm = 1, status = 'false_alarm', updated_at = :updatedAt WHERE id = :sosId")
    suspend fun markAsFalseAlarm(sosId: String, updatedAt: Long)

    @Query("UPDATE sos_history SET responders_count = responders_count + 1 WHERE id = :sosId")
    suspend fun incrementResponderCount(sosId: String)

    @Query("""
        SELECT * FROM sos_history 
        WHERE user_id = :userId 
        AND created_at BETWEEN :startTime AND :endTime 
        ORDER BY created_at DESC
    """)
    suspend fun getHistoryBetweenDates(userId: String, startTime: Long, endTime: Long): List<SOSHistoryEntity>
}