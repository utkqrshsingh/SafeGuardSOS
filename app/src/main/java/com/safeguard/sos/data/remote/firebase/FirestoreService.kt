// app/src/main/java/com/safeguard/sos/data/remote/firebase/FirestoreService.kt

package com.safeguard.sos.data.remote.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.safeguard.sos.core.common.Constants
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.EmergencyContact
import com.safeguard.sos.domain.model.Helper
import com.safeguard.sos.domain.model.HelperResponse
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // ==================== USER OPERATIONS ====================

    suspend fun createUser(userId: String, userData: Map<String, Any?>): Resource<Boolean> {
        return try {
            firestore.collection(Constants.Firestore.COLLECTION_USERS)
                .document(userId)
                .set(userData)
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Create user failed")
            Resource.Error(e.message ?: "Failed to create user", exception = e)
        }
    }

    suspend fun getUser(userId: String): Resource<DocumentSnapshot> {
        return try {
            val document = firestore.collection(Constants.Firestore.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            if (document.exists()) {
                Resource.Success(document)
            } else {
                Resource.Error("User not found")
            }
        } catch (e: Exception) {
            Timber.e(e, "Get user failed")
            Resource.Error(e.message ?: "Failed to get user", exception = e)
        }
    }

    fun getUserFlow(userId: String): Flow<DocumentSnapshot?> = callbackFlow {
        val listener = firestore.collection(Constants.Firestore.COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "User listener error")
                    return@addSnapshotListener
                }
                trySend(snapshot)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateUser(userId: String, updates: Map<String, Any?>): Resource<Boolean> {
        return try {
            firestore.collection(Constants.Firestore.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Update user failed")
            Resource.Error(e.message ?: "Failed to update user", exception = e)
        }
    }

    suspend fun deleteUser(userId: String): Resource<Boolean> {
        return try {
            firestore.collection(Constants.Firestore.COLLECTION_USERS)
                .document(userId)
                .delete()
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Delete user failed")
            Resource.Error(e.message ?: "Failed to delete user", exception = e)
        }
    }

    // ==================== SOS OPERATIONS ====================

    suspend fun createSOSAlert(sosData: Map<String, Any?>): Resource<String> {
        return try {
            val docRef = firestore.collection(Constants.Firestore.COLLECTION_SOS_ALERTS)
                .add(sosData)
                .await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Timber.e(e, "Create SOS alert failed")
            Resource.Error(e.message ?: "Failed to create SOS alert", exception = e)
        }
    }

    suspend fun getSOSAlert(sosId: String): Resource<DocumentSnapshot> {
        return try {
            val document = firestore.collection(Constants.Firestore.COLLECTION_SOS_ALERTS)
                .document(sosId)
                .get()
                .await()
            if (document.exists()) {
                Resource.Success(document)
            } else {
                Resource.Error("SOS alert not found")
            }
        } catch (e: Exception) {
            Timber.e(e, "Get SOS alert failed")
            Resource.Error(e.message ?: "Failed to get SOS alert", exception = e)
        }
    }

    fun getActiveSOSAlertFlow(userId: String): Flow<DocumentSnapshot?> = callbackFlow {
        val listener = firestore.collection(Constants.Firestore.COLLECTION_SOS_ALERTS)
            .whereEqualTo("user_id", userId)
            .whereIn("status", listOf("pending", "active"))
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Active SOS listener error")
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.firstOrNull())
            }
        awaitClose { listener.remove() }
    }

    fun getSOSHistoryFlow(userId: String): Flow<List<DocumentSnapshot>> = callbackFlow {
        val listener = firestore.collection(Constants.Firestore.COLLECTION_SOS_ALERTS)
            .whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "SOS history listener error")
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateSOSAlert(sosId: String, updates: Map<String, Any?>): Resource<Boolean> {
        return try {
            firestore.collection(Constants.Firestore.COLLECTION_SOS_ALERTS)
                .document(sosId)
                .update(updates)
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Update SOS alert failed")
            Resource.Error(e.message ?: "Failed to update SOS alert", exception = e)
        }
    }

    suspend fun updateSOSLocation(sosId: String, location: Location): Resource<Boolean> {
        return try {
            val locationData = mapOf(
                "location" to mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "accuracy" to location.accuracy,
                    "address" to location.address,
                    "timestamp" to location.timestamp
                ),
                "updated_at" to FieldValue.serverTimestamp()
            )
            firestore.collection(Constants.Firestore.COLLECTION_SOS_ALERTS)
                .document(sosId)
                .update(locationData)
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Update SOS location failed")
            Resource.Error(e.message ?: "Failed to update location", exception = e)
        }
    }

    // ==================== HELPER OPERATIONS ====================

    suspend fun registerHelper(helperId: String, helperData: Map<String, Any?>): Resource<Boolean> {
        return try {
            firestore.collection(Constants.Firestore.COLLECTION_HELPERS)
                .document(helperId)
                .set(helperData)
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Register helper failed")
            Resource.Error(e.message ?: "Failed to register helper", exception = e)
        }
    }

    suspend fun getHelper(helperId: String): Resource<DocumentSnapshot> {
        return try {
            val document = firestore.collection(Constants.Firestore.COLLECTION_HELPERS)
                .document(helperId)
                .get()
                .await()
            if (document.exists()) {
                Resource.Success(document)
            } else {
                Resource.Error("Helper not found")
            }
        } catch (e: Exception) {
            Timber.e(e, "Get helper failed")
            Resource.Error(e.message ?: "Failed to get helper", exception = e)
        }
    }

    fun getHelperFlow(helperId: String): Flow<DocumentSnapshot?> = callbackFlow {
        val listener = firestore.collection(Constants.Firestore.COLLECTION_HELPERS)
            .document(helperId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Helper listener error")
                    return@addSnapshotListener
                }
                trySend(snapshot)
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateHelperStatus(helperId: String, isActive: Boolean): Resource<Boolean> {
        return try {
            firestore.collection(Constants.Firestore.COLLECTION_HELPERS)
                .document(helperId)
                .update(
                    mapOf(
                        "status" to if (isActive) "active" else "inactive",
                        "is_available" to isActive,
                        "updated_at" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Update helper status failed")
            Resource.Error(e.message ?: "Failed to update status", exception = e)
        }
    }

    suspend fun updateHelperLocation(helperId: String, location: Location): Resource<Boolean> {
        return try {
            val locationData = mapOf(
                "location" to GeoPoint(location.latitude, location.longitude),
                "last_location_update" to FieldValue.serverTimestamp(),
                "updated_at" to FieldValue.serverTimestamp()
            )
            firestore.collection(Constants.Firestore.COLLECTION_HELPERS)
                .document(helperId)
                .update(locationData)
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Update helper location failed")
            Resource.Error(e.message ?: "Failed to update location", exception = e)
        }
    }

    suspend fun getNearbyActiveHelpers(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Resource<List<DocumentSnapshot>> {
        return try {
            // Note: For production, use Firebase GeoFirestore or a server-side solution
            // for efficient geolocation queries
            val documents = firestore.collection(Constants.Firestore.COLLECTION_HELPERS)
                .whereEqualTo("is_available", true)
                .whereEqualTo("status", "active")
                .whereEqualTo("verification_status", "verified")
                .get()
                .await()
            Resource.Success(documents.documents)
        } catch (e: Exception) {
            Timber.e(e, "Get nearby helpers failed")
            Resource.Error(e.message ?: "Failed to get nearby helpers", exception = e)
        }
    }

    fun getNearbySOSAlertsFlow(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Flow<List<DocumentSnapshot>> = callbackFlow {
        val listener = firestore.collection(Constants.Firestore.COLLECTION_SOS_ALERTS)
            .whereIn("status", listOf("pending", "active"))
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Nearby SOS listener error")
                    return@addSnapshotListener
                }
                // Filter by distance client-side (for demo purposes)
                // In production, use server-side geolocation queries
                trySend(snapshot?.documents ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ==================== EMERGENCY CONTACTS ====================

    suspend fun saveEmergencyContacts(
        userId: String,
        contacts: List<Map<String, Any?>>
    ): Resource<Boolean> {
        return try {
            val batch = firestore.batch()
            val contactsRef = firestore.collection(Constants.Firestore.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.Firestore.COLLECTION_EMERGENCY_CONTACTS)

            contacts.forEach { contact ->
                val docRef = contactsRef.document(contact["id"] as String)
                batch.set(docRef, contact, SetOptions.merge())
            }

            batch.commit().await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Save emergency contacts failed")
            Resource.Error(e.message ?: "Failed to save contacts", exception = e)
        }
    }

    suspend fun getEmergencyContacts(userId: String): Resource<List<DocumentSnapshot>> {
        return try {
            val documents = firestore.collection(Constants.Firestore.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.Firestore.COLLECTION_EMERGENCY_CONTACTS)
                .orderBy("is_primary", Query.Direction.DESCENDING)
                .get()
                .await()
            Resource.Success(documents.documents)
        } catch (e: Exception) {
            Timber.e(e, "Get emergency contacts failed")
            Resource.Error(e.message ?: "Failed to get contacts", exception = e)
        }
    }

    fun getEmergencyContactsFlow(userId: String): Flow<List<DocumentSnapshot>> = callbackFlow {
        val listener = firestore.collection(Constants.Firestore.COLLECTION_USERS)
            .document(userId)
            .collection(Constants.Firestore.COLLECTION_EMERGENCY_CONTACTS)
            .orderBy("is_primary", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Emergency contacts listener error")
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun deleteEmergencyContact(userId: String, contactId: String): Resource<Boolean> {
        return try {
            firestore.collection(Constants.Firestore.COLLECTION_USERS)
                .document(userId)
                .collection(Constants.Firestore.COLLECTION_EMERGENCY_CONTACTS)
                .document(contactId)
                .delete()
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Delete emergency contact failed")
            Resource.Error(e.message ?: "Failed to delete contact", exception = e)
        }
    }

    // ==================== HELPER RESPONSES ====================

    suspend fun createHelperResponse(responseData: Map<String, Any?>): Resource<String> {
        return try {
            val docRef = firestore.collection(Constants.Firestore.COLLECTION_HELPER_RESPONSES)
                .add(responseData)
                .await()
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Timber.e(e, "Create helper response failed")
            Resource.Error(e.message ?: "Failed to create response", exception = e)
        }
    }

    suspend fun updateHelperResponse(
        responseId: String,
        updates: Map<String, Any?>
    ): Resource<Boolean> {
        return try {
            firestore.collection(Constants.Firestore.COLLECTION_HELPER_RESPONSES)
                .document(responseId)
                .update(updates)
                .await()
            Resource.Success(true)
        } catch (e: Exception) {
            Timber.e(e, "Update helper response failed")
            Resource.Error(e.message ?: "Failed to update response", exception = e)
        }
    }

    fun getSOSResponsesFlow(sosId: String): Flow<List<DocumentSnapshot>> = callbackFlow {
        val listener = firestore.collection(Constants.Firestore.COLLECTION_HELPER_RESPONSES)
            .whereEqualTo("sos_alert_id", sosId)
            .orderBy("responded_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "SOS responses listener error")
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents ?: emptyList())
            }
        awaitClose { listener.remove() }
    }
}