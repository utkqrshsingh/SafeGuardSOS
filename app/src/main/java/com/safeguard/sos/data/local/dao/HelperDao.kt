// app/src/main/java/com/safeguard/sos/data/local/dao/HelperDao.kt

package com.safeguard.sos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.safeguard.sos.data.local.entity.HelperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HelperDao {

    @Query("SELECT * FROM helpers WHERE id = :helperId LIMIT 1")
    suspend fun getHelperById(helperId: String): HelperEntity?

    @Query("SELECT * FROM helpers WHERE id = :helperId LIMIT 1")
    fun getHelperByIdFlow(helperId: String): Flow<HelperEntity?>

    @Query("SELECT * FROM helpers WHERE user_id = :userId LIMIT 1")
    suspend fun getHelperByUserId(userId: String): HelperEntity?

    @Query("SELECT * FROM helpers WHERE user_id = :userId LIMIT 1")
    fun getHelperByUserIdFlow(userId: String): Flow<HelperEntity?>

    @Query("SELECT * FROM helpers WHERE is_available = 1 AND status = 'active'")
    fun getActiveHelpers(): Flow<List<HelperEntity>>

    @Query("SELECT * FROM helpers WHERE is_available = 1 AND status = 'active'")
    suspend fun getActiveHelpersSync(): List<HelperEntity>

    @Query("""
        SELECT * FROM helpers 
        WHERE is_available = 1 
        AND status = 'active'
        AND verification_status = 'verified'
        ORDER BY rating DESC
    """)
    fun getVerifiedActiveHelpers(): Flow<List<HelperEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHelper(helper: HelperEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHelpers(helpers: List<HelperEntity>)

    @Update
    suspend fun updateHelper(helper: HelperEntity)

    @Delete
    suspend fun deleteHelper(helper: HelperEntity)

    @Query("DELETE FROM helpers WHERE id = :helperId")
    suspend fun deleteHelperById(helperId: String)

    @Query("DELETE FROM helpers")
    suspend fun deleteAll()

    @Query("UPDATE helpers SET status = :status, updated_at = :updatedAt WHERE id = :helperId")
    suspend fun updateHelperStatus(helperId: String, status: String, updatedAt: Long)

    @Query("UPDATE helpers SET is_available = :isAvailable, updated_at = :updatedAt WHERE id = :helperId")
    suspend fun updateAvailability(helperId: String, isAvailable: Boolean, updatedAt: Long)

    @Query("""
        UPDATE helpers SET 
            location_latitude = :latitude, 
            location_longitude = :longitude, 
            location_timestamp = :timestamp,
            updated_at = :timestamp 
        WHERE id = :helperId
    """)
    suspend fun updateLocation(helperId: String, latitude: Double, longitude: Double, timestamp: Long)

    @Query("""
        UPDATE helpers SET 
            total_responses = total_responses + 1,
            updated_at = :updatedAt 
        WHERE id = :helperId
    """)
    suspend fun incrementTotalResponses(helperId: String, updatedAt: Long)

    @Query("""
        UPDATE helpers SET 
            successful_responses = successful_responses + 1,
            updated_at = :updatedAt 
        WHERE id = :helperId
    """)
    suspend fun incrementSuccessfulResponses(helperId: String, updatedAt: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM helpers WHERE user_id = :userId)")
    suspend fun isUserHelper(userId: String): Boolean
}