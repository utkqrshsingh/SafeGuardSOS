// app/src/main/java/com/safeguard/sos/domain/repository/EmergencyContactRepository.kt

package com.safeguard.sos.domain.repository

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.EmergencyContact
import com.safeguard.sos.domain.model.Relationship
import kotlinx.coroutines.flow.Flow

interface EmergencyContactRepository {

    fun getContacts(): Flow<List<EmergencyContact>>

    suspend fun getContactsSync(): List<EmergencyContact>

    suspend fun getContactById(contactId: String): Resource<EmergencyContact>

    fun getContactByIdFlow(contactId: String): Flow<EmergencyContact?>

    suspend fun getPrimaryContact(): EmergencyContact?

    suspend fun addContact(
        name: String,
        phoneNumber: String,
        relationship: Relationship,
        isPrimary: Boolean = false,
        notifyViaSms: Boolean = true,
        notifyViaCall: Boolean = false
    ): Resource<EmergencyContact>

    suspend fun updateContact(contact: EmergencyContact): Resource<EmergencyContact>

    suspend fun deleteContact(contactId: String): Resource<Boolean>

    suspend fun setPrimaryContact(contactId: String): Resource<Boolean>

    suspend fun syncContacts(): Resource<List<EmergencyContact>>

    fun getContactCount(): Flow<Int>

    suspend fun contactExists(phoneNumber: String): Boolean
}