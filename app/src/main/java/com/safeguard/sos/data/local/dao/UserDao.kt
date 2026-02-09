// app/src/main/java/com/safeguard/sos/data/local/dao/UserDao.kt

package com.safeguard.sos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.safeguard.sos.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE phone_number = :phoneNumber LIMIT 1")
    suspend fun getUserByPhoneNumber(phoneNumber: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    @Query("UPDATE users SET verification_status = :status WHERE id = :userId")
    suspend fun updateVerificationStatus(userId: String, status: String)

    @Query("UPDATE users SET helper_status = :status WHERE id = :userId")
    suspend fun updateHelperStatus(userId: String, status: String)

    @Query("UPDATE users SET fcm_token = :token WHERE id = :userId")
    suspend fun updateFcmToken(userId: String, token: String)

    @Query("UPDATE users SET profile_image_url = :url WHERE id = :userId")
    suspend fun updateProfileImage(userId: String, url: String?)

    @Query("UPDATE users SET last_active_at = :timestamp WHERE id = :userId")
    suspend fun updateLastActiveAt(userId: String, timestamp: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE id = :userId)")
    suspend fun userExists(userId: String): Boolean
}