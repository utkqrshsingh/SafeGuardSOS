package com.safeguard.sos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.data.local.dao.EmergencyContactDao
import com.safeguard.sos.data.local.datastore.UserPreferences
import com.safeguard.sos.data.mapper.ContactMapper
import com.safeguard.sos.domain.model.EmergencyContact
import com.safeguard.sos.domain.model.Relationship
import com.safeguard.sos.domain.repository.EmergencyContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyContactRepositoryImpl @Inject constructor(
    private val contactDao: EmergencyContactDao,
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences,
    private val contactMapper: ContactMapper
) : EmergencyContactRepository {

    override fun getContacts(): Flow<List<EmergencyContact>> {
        return flow {
            val userId = userPreferences.userIdFlow.first()
            if (!userId.isNullOrEmpty()) {
                contactDao.getContactsByUserId(userId).collect { entities ->
                    emit(entities.map { contactMapper.mapEntityToDomain(it) })
                }
            } else {
                emit(emptyList())
            }
        }
    }

    override suspend fun getContactsSync(): List<EmergencyContact> {
        val userId = userPreferences.userIdFlow.first() ?: return emptyList()
        return contactDao.getContactsByUserIdSync(userId).map { contactMapper.mapEntityToDomain(it) }
    }

    override suspend fun getContactById(contactId: String): Resource<EmergencyContact> {
        return try {
            val entity = contactDao.getContactById(contactId)
            if (entity != null) {
                Resource.Success(contactMapper.mapEntityToDomain(entity))
            } else {
                Resource.Error("Contact not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }

    override fun getContactByIdFlow(contactId: String): Flow<EmergencyContact?> {
        return contactDao.getContactByIdFlow(contactId).map { it?.let { contactMapper.mapEntityToDomain(it) } }
    }

    override suspend fun getPrimaryContact(): EmergencyContact? {
        val userId = userPreferences.userIdFlow.first() ?: return null
        return contactDao.getPrimaryContact(userId)?.let { contactMapper.mapEntityToDomain(it) }
    }

    override suspend fun addContact(
        name: String,
        phoneNumber: String,
        relationship: Relationship,
        isPrimary: Boolean,
        notifyViaSms: Boolean,
        notifyViaCall: Boolean
    ): Resource<EmergencyContact> {
        return try {
            val userId = userPreferences.userIdFlow.first()
            if (userId.isNullOrEmpty()) return Resource.Error("User not logged in")

            val currentCount = contactDao.getContactCount(userId)
            if (currentCount >= 5) return Resource.Error("Maximum 5 contacts allowed")

            val contactId = UUID.randomUUID().toString()
            val contact = EmergencyContact(
                id = contactId,
                userId = userId,
                name = name,
                phoneNumber = phoneNumber,
                relationship = relationship,
                isPrimary = isPrimary,
                notifyViaSms = notifyViaSms,
                notifyViaCall = notifyViaCall,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Add to Firestore
            firestore.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .document(contactId)
                .set(contact)
                .await()

            if (isPrimary) {
                contactDao.clearPrimaryContact(userId)
            }

            contactDao.insertContact(contactMapper.mapDomainToEntity(contact, userId))
            userPreferences.setEmergencyContactCount(currentCount + 1)

            Resource.Success(contact)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add contact")
        }
    }

    override suspend fun updateContact(contact: EmergencyContact): Resource<EmergencyContact> {
        return try {
            val userId = userPreferences.userIdFlow.first()
            if (userId.isNullOrEmpty()) return Resource.Error("User not logged in")

            val updatedContact = contact.copy(updatedAt = System.currentTimeMillis())

            firestore.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .document(contact.id)
                .set(updatedContact)
                .await()

            if (updatedContact.isPrimary) {
                contactDao.clearPrimaryContact(userId)
            }

            contactDao.updateContact(contactMapper.mapDomainToEntity(updatedContact, userId))
            Resource.Success(updatedContact)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update contact")
        }
    }

    override suspend fun deleteContact(contactId: String): Resource<Boolean> {
        return try {
            val userId = userPreferences.userIdFlow.first()
            if (userId.isNullOrEmpty()) return Resource.Error("User not logged in")

            firestore.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .document(contactId)
                .delete()
                .await()

            contactDao.deleteContactById(contactId)
            val currentCount = contactDao.getContactCount(userId)
            userPreferences.setEmergencyContactCount(currentCount)

            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete contact")
        }
    }

    override suspend fun setPrimaryContact(contactId: String): Resource<Boolean> {
        return try {
            val userId = userPreferences.userIdFlow.first()
            if (userId.isNullOrEmpty()) return Resource.Error("User not logged in")

            // Update local
            contactDao.clearPrimaryContact(userId)
            contactDao.setPrimaryContact(contactId)

            // Update Firestore (Simplified: just update the specific ones)
            // In a real app, you might want to use a batch or cloud function
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .whereEqualTo("isPrimary", true)
                .get()
                .await()

            for (doc in snapshot.documents) {
                if (doc.id != contactId) {
                    doc.reference.update("isPrimary", false).await()
                }
            }
            firestore.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .document(contactId)
                .update("isPrimary", true)
                .await()

            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to set primary contact")
        }
    }

    override suspend fun syncContacts(): Resource<List<EmergencyContact>> {
        return try {
            val userId = userPreferences.userIdFlow.first()
            if (userId.isNullOrEmpty()) return Resource.Error("User not logged in")

            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("emergency_contacts")
                .get()
                .await()

            val remoteContacts = snapshot.documents.mapNotNull { doc ->
                // Firestore can deserialize to object if fields match
                // But EmergencyContact has Relationship enum which needs manual mapping if not using custom converter
                val relationshipStr = doc.getString("relationship") ?: "other"
                EmergencyContact(
                    id = doc.id,
                    userId = userId,
                    name = doc.getString("name") ?: "",
                    phoneNumber = doc.getString("phoneNumber") ?: "",
                    relationship = Relationship.fromValue(relationshipStr),
                    isPrimary = doc.getBoolean("isPrimary") ?: false,
                    notifyViaSms = doc.getBoolean("notifyViaSms") ?: true,
                    notifyViaCall = doc.getBoolean("notifyViaCall") ?: false,
                    photoUri = doc.getString("photoUri"),
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }

            contactDao.deleteAllContactsByUserId(userId)
            contactDao.insertContacts(remoteContacts.map { contactMapper.mapDomainToEntity(it, userId) })
            userPreferences.setEmergencyContactCount(remoteContacts.size)

            Resource.Success(remoteContacts)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to sync contacts")
        }
    }

    override fun getContactCount(): Flow<Int> {
        return flow {
            val userId = userPreferences.userIdFlow.first()
            if (!userId.isNullOrEmpty()) {
                contactDao.getContactCountFlow(userId).collect {
                    emit(it)
                }
            } else {
                emit(0)
            }
        }
    }

    override suspend fun contactExists(phoneNumber: String): Boolean {
        val userId = userPreferences.userIdFlow.first() ?: return false
        return contactDao.contactExists(userId, phoneNumber)
    }
}
