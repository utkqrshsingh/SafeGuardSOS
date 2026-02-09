package com.safeguard.sos.domain.repository

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.*
import kotlinx.coroutines.flow.Flow

interface SOSRepository {
    fun triggerSOS(location: Location, emergencyType: String, message: String?): Flow<Resource<SOSAlert>>
    fun cancelSOS(sosId: String): Flow<Resource<Unit>>
    fun updateSOSStatus(sosId: String, status: SOSStatus): Flow<Resource<SOSAlert>>
    suspend fun resolveSOS(sosId: String): Resource<Boolean>
    suspend fun markAsFalseAlarm(sosId: String): Resource<Boolean>
    fun getActiveSOSAlert(): Flow<SOSAlert?>
    fun getActiveSOSAlerts(): Flow<Resource<List<SOSAlert>>>
    fun getNearbySOSAlerts(latitude: Double, longitude: Double, radiusKm: Double): Flow<Resource<List<SOSAlert>>>
    suspend fun getActiveSOSAlertSync(): SOSAlert?
    fun getSOSById(sosId: String): Flow<Resource<SOSAlert>>
    fun getSOSByIdFlow(sosId: String): Flow<SOSAlert?>
    fun getSOSHistory(): Flow<Resource<List<SOSAlert>>>
    suspend fun getRecentSOSHistory(limit: Int = 10): Resource<List<SOSAlert>>
    fun updateSOSLocation(sosId: String, location: Location): Flow<Resource<Unit>>
    suspend fun getNearbyHelpers(latitude: Double, longitude: Double, radiusKm: Int): Resource<List<NearbyHelper>>
    fun getSOSResponders(sosId: String): Flow<List<HelperResponse>>
    suspend fun notifyEmergencyContacts(sosId: String): Resource<Int>
    suspend fun uploadAudioRecording(sosId: String, audioPath: String): Resource<String>
    suspend fun uploadVideoRecording(sosId: String, videoPath: String): Resource<String>
    fun observeSOSAlert(sosId: String): Flow<Resource<SOSAlert>>
    fun getSOSHistoryCount(): Flow<Int>
    fun getHelpsProvidedCount(): Flow<Int>
    fun sendUpdateMessage(sosId: String, message: String): Flow<Resource<Unit>>
}
