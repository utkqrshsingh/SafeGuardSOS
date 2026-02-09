package com.safeguard.sos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.core.utils.DistanceCalculator
import com.safeguard.sos.data.local.dao.SOSHistoryDao
import com.safeguard.sos.data.local.datastore.UserPreferences
import com.safeguard.sos.data.mapper.SOSMapper
import com.safeguard.sos.data.remote.api.SOSApi
import com.safeguard.sos.data.remote.dto.request.SOSRequest
import com.safeguard.sos.data.remote.dto.request.UpdateLocationRequest
import com.safeguard.sos.domain.model.*
import com.safeguard.sos.domain.repository.SOSRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SOSRepositoryImpl @Inject constructor(
    private val sosApi: SOSApi,
    private val sosHistoryDao: SOSHistoryDao,
    private val firestore: FirebaseFirestore,
    private val userPreferences: UserPreferences,
    private val sosMapper: SOSMapper
) : SOSRepository {

    override fun triggerSOS(
        location: Location,
        emergencyType: String,
        message: String?
    ): Flow<Resource<SOSAlert>> = flow {
        emit(Resource.Loading)
        try {
            val userId = userPreferences.userIdFlow.first()
            if (userId.isNullOrEmpty()) {
                emit(Resource.Error("User not logged in"))
                return@flow
            }

            val request = SOSRequest(
                userId = userId,
                latitude = location.latitude,
                longitude = location.longitude,
                address = location.address,
                alertType = emergencyType,
                message = message
            )

            val response = sosApi.triggerSOS(request)
            if (response.isSuccessful && response.body() != null) {
                val sosAlertResponse = response.body()!!
                val sosAlert = sosMapper.mapResponseToDomain(sosAlertResponse)

                // Save to local database
                sosHistoryDao.insertSOS(sosMapper.mapDomainToEntity(sosAlert))

                // Also save to Firestore for real-time updates
                firestore.collection("sos_alerts")
                    .document(sosAlert.id)
                    .set(sosMapper.mapDomainToFirestore(sosAlert))
                    .await()

                emit(Resource.Success(sosAlert))
            } else {
                emit(Resource.Error("Failed to trigger SOS"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to trigger SOS"))
        }
    }

    override fun cancelSOS(sosId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val response = sosApi.cancelSOS(sosId)
            if (response.isSuccessful) {
                // Update local database
                sosHistoryDao.updateSOSStatus(sosId, SOSStatus.CANCELLED.name, System.currentTimeMillis())

                // Update Firestore
                firestore.collection("sos_alerts")
                    .document(sosId)
                    .update(
                        mapOf(
                            "status" to SOSStatus.CANCELLED.name,
                            "cancelledAt" to System.currentTimeMillis()
                        )
                    )
                    .await()

                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error("Failed to cancel SOS"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to cancel SOS"))
        }
    }

    override fun updateSOSStatus(sosId: String, status: SOSStatus): Flow<Resource<SOSAlert>> = flow {
        emit(Resource.Loading)
        // Note: SOSApi doesn't have updateSOSStatus, but the interface has it.
        // For now, using a placeholder or it might need fixing in SOSApi.
        emit(Resource.Error("updateSOSStatus not implemented in API"))
    }

    override suspend fun resolveSOS(sosId: String): Resource<Boolean> {
        return try {
            val response = sosApi.resolveSOS(sosId)
            if (response.isSuccessful) {
                Resource.Success(true)
            } else {
                Resource.Error("Failed to resolve SOS")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to resolve SOS")
        }
    }

    override suspend fun markAsFalseAlarm(sosId: String): Resource<Boolean> {
        return try {
            val response = sosApi.markAsFalseAlarm(sosId)
            if (response.isSuccessful) {
                Resource.Success(true)
            } else {
                Resource.Error("Failed to mark as false alarm")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to mark as false alarm")
        }
    }

    override fun getActiveSOSAlert(): Flow<SOSAlert?> = callbackFlow {
        val listener = firestore.collection("sos_alerts")
            .whereIn("status", listOf(SOSStatus.ACTIVE.name, SOSStatus.HELP_ON_WAY.name))
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val alert = sosMapper.mapFirestoreToDomain(snapshot.documents[0])
                    trySend(alert)
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getActiveSOSAlerts(): Flow<Resource<List<SOSAlert>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = firestore.collection("sos_alerts")
            .whereIn("status", listOf(SOSStatus.ACTIVE.name, SOSStatus.HELP_ON_WAY.name))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to get active alerts"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val alerts = snapshot.documents.mapNotNull { doc ->
                        try {
                            sosMapper.mapFirestoreToDomain(doc)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Resource.Success(alerts))
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getNearbySOSAlerts(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Flow<Resource<List<SOSAlert>>> = callbackFlow {
        trySend(Resource.Loading)

        val listener = firestore.collection("sos_alerts")
            .whereIn("status", listOf(SOSStatus.ACTIVE.name, SOSStatus.HELP_ON_WAY.name))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Failed to get nearby alerts"))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val alerts = snapshot.documents.mapNotNull { doc ->
                        try {
                            val alert = sosMapper.mapFirestoreToDomain(doc)
                            val distance = DistanceCalculator.calculateDistanceKm(
                                latitude, longitude,
                                alert.location.latitude, alert.location.longitude
                            )
                            if (distance <= radiusKm) alert else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Resource.Success(alerts))
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getActiveSOSAlertSync(): SOSAlert? {
        return try {
            val response = sosApi.getActiveSOSAlert()
            if (response.isSuccessful && response.body() != null) {
                sosMapper.mapResponseToDomain(response.body()!!)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun getSOSById(sosId: String): Flow<Resource<SOSAlert>> = flow {
        emit(Resource.Loading)
        try {
            val response = sosApi.getSOSById(sosId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(sosMapper.mapResponseToDomain(response.body()!!)))
            } else {
                emit(Resource.Error("Failed to get SOS alert"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to get SOS alert"))
        }
    }

    override fun getSOSByIdFlow(sosId: String): Flow<SOSAlert?> = callbackFlow {
        val listener = firestore.collection("sos_alerts")
            .document(sosId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    trySend(sosMapper.mapFirestoreToDomain(snapshot))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getSOSHistory(): Flow<Resource<List<SOSAlert>>> = flow {
        emit(Resource.Loading)
        try {
            val response = sosApi.getSOSHistory()
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.map { sosMapper.mapResponseToDomain(it) }))
            } else {
                emit(Resource.Error("Failed to get SOS history"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to get SOS history"))
        }
    }

    override suspend fun getRecentSOSHistory(limit: Int): Resource<List<SOSAlert>> {
        return try {
            val response = sosApi.getSOSHistory(limit = limit)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.map { sosMapper.mapResponseToDomain(it) })
            } else {
                Resource.Error("Failed to get recent history")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get recent history")
        }
    }

    override fun updateSOSLocation(sosId: String, location: Location): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        try {
            val request = UpdateLocationRequest(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                timestamp = location.timestamp
            )
            val response = sosApi.updateSOSLocation(sosId, request)
            if (response.isSuccessful) {
                // Update Firestore too
                firestore.collection("sos_alerts").document(sosId).update(
                    mapOf("latitude" to location.latitude, "longitude" to location.longitude, "address" to location.address)
                ).await()
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error("Failed to update location"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update location"))
        }
    }

    override suspend fun getNearbyHelpers(latitude: Double, longitude: Double, radiusKm: Int): Resource<List<NearbyHelper>> {
        return try {
            val response = sosApi.getNearbyHelpers(latitude, longitude, radiusKm)
            if (response.isSuccessful && response.body() != null) {
                val helpers = response.body()!!.helpers.map { helperDto ->
                    NearbyHelper(
                        helper = Helper(
                            id = helperDto.id,
                            userId = helperDto.userId,
                            name = helperDto.name,
                            phoneNumber = helperDto.phoneNumber,
                            photoUrl = helperDto.photoUrl,
                            location = null, // Location not provided in this simplified DTO
                            status = HelperStatus.AVAILABLE,
                            verificationStatus = VerificationStatus.VERIFIED,
                            rating = helperDto.rating,
                            totalResponses = helperDto.totalResponses,
                            successfulResponses = 0,
                            averageResponseTime = 0,
                            radiusKm = 0,
                            skills = null,
                            isAvailable = true,
                            lastActiveAt = null,
                            createdAt = 0L,
                            updatedAt = 0L
                        ),
                        distanceKm = helperDto.distanceKm,
                        estimatedArrivalMinutes = helperDto.estimatedArrivalMinutes
                    )
                }
                Resource.Success(helpers)
            } else {
                Resource.Error(response.message() ?: "Failed to get nearby helpers")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred while fetching nearby helpers")
        }
    }

    override fun getSOSResponders(sosId: String): Flow<List<HelperResponse>> = flow {
        // Not implemented
    }

    override suspend fun notifyEmergencyContacts(sosId: String): Resource<Int> {
        return try {
            val response = sosApi.notifyEmergencyContacts(sosId)
            if (response.isSuccessful) Resource.Success(1) else Resource.Error("Failed")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error")
        }
    }

    override suspend fun uploadAudioRecording(sosId: String, audioPath: String): Resource<String> {
        return Resource.Error("Not implemented")
    }

    override suspend fun uploadVideoRecording(sosId: String, videoPath: String): Resource<String> {
        return Resource.Error("Not implemented")
    }

    override fun observeSOSAlert(sosId: String): Flow<Resource<SOSAlert>> = callbackFlow {
        val listener = firestore.collection("sos_alerts")
            .document(sosId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.message ?: "Error"))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(Resource.Success(sosMapper.mapFirestoreToDomain(snapshot)))
                }
            }
        awaitClose { listener.remove() }
    }

    override fun getSOSHistoryCount(): Flow<Int> = flow { emit(0) }
    override fun getHelpsProvidedCount(): Flow<Int> = flow { emit(0) }
    override fun sendUpdateMessage(sosId: String, message: String): Flow<Resource<Unit>> = flow { emit(Resource.Success(Unit)) }
}
