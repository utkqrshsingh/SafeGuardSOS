// app/src/main/java/com/safeguard/sos/data/local/dao/EmergencyContactDao.kt

package com.safeguard.sos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.safeguard.sos.data.local.entity.EmergencyContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {

    @Query("SELECT * FROM emergency_contacts WHERE user_id = :userId ORDER BY is_primary DESC, name ASC")
    fun getContactsByUserId(userId: String): Flow<List<EmergencyContactEntity>>

    @Query("SELECT * FROM emergency_contacts WHERE user_id = :userId ORDER BY is_primary DESC, name ASC")
    suspend fun getContactsByUserIdSync(userId: String): List<EmergencyContactEntity>

    @Query("SELECT * FROM emergency_contacts WHERE id = :contactId LIMIT 1")
    suspend fun getContactById(contactId: String): EmergencyContactEntity?

    @Query("SELECT * FROM emergency_contacts WHERE id = :contactId LIMIT 1")
    fun getContactByIdFlow(contactId: String): Flow<EmergencyContactEntity?>

    @Query("SELECT * FROM emergency_contacts WHERE user_id = :userId AND is_primary = 1 LIMIT 1")
    suspend fun getPrimaryContact(userId: String): EmergencyContactEntity?

    @Query("SELECT * FROM emergency_contacts WHERE user_id = :userId AND phone_number = :phoneNumber LIMIT 1")
    suspend fun getContactByPhoneNumber(userId: String, phoneNumber: String): EmergencyContactEntity?

    @Query("SELECT COUNT(*) FROM emergency_contacts WHERE user_id = :userId")
    suspend fun getContactCount(userId: String): Int

    @Query("SELECT COUNT(*) FROM emergency_contacts WHERE user_id = :userId")
    fun getContactCountFlow(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<EmergencyContactEntity>)

    @Update
    suspend fun updateContact(contact: EmergencyContactEntity)

    @Delete
    suspend fun deleteContact(contact: EmergencyContactEntity)

    @Query("DELETE FROM emergency_contacts WHERE id = :contactId")
    suspend fun deleteContactById(contactId: String)

    @Query("DELETE FROM emergency_contacts WHERE user_id = :userId")
    suspend fun deleteAllContactsByUserId(userId: String)

    @Query("UPDATE emergency_contacts SET is_primary = 0 WHERE user_id = :userId")
    suspend fun clearPrimaryContact(userId: String)

    @Query("UPDATE emergency_contacts SET is_primary = 1 WHERE id = :contactId")
    suspend fun setPrimaryContact(contactId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM emergency_contacts WHERE user_id = :userId AND phone_number = :phoneNumber)")
    suspend fun contactExists(userId: String, phoneNumber: String): Boolean
}