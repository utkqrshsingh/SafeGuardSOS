// app/src/main/java/com/safeguard/sos/domain/repository/HelperRepository.kt

package com.safeguard.sos.domain.repository

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.Helper
import com.safeguard.sos.domain.model.HelperResponse
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.model.HelperStatus
import kotlinx.coroutines.flow.Flow

interface HelperRepository {

    fun isRegisteredHelper(): Flow<Resource<Boolean>>

    suspend fun registerAsHelper(radiusKm: Int, skills: List<String>?): Resource<Helper>

    suspend fun getHelperProfile(): Flow<Resource<Helper>>

    fun getHelperProfileFlow(): Flow<Helper?>

    suspend fun updateHelperStatus(status: HelperStatus): Flow<Resource<HelperStatus>>

    suspend fun updateHelperLocation(location: Location): Resource<Boolean>

    suspend fun updateHelperRadius(radiusKm: Int): Resource<Boolean>

    fun getNearbySOSAlerts(): Flow<List<SOSAlert>>

    suspend fun getNearbySOSAlertsSync(
        latitude: Double,
        longitude: Double,
        radiusKm: Int
    ): Resource<List<SOSAlert>>

    suspend fun respondToSOS(sosId: String, location: Location): Resource<HelperResponse>

    suspend fun cancelResponse(responseId: String): Resource<Boolean>

    suspend fun markArrived(responseId: String): Resource<Boolean>

    suspend fun completeResponse(responseId: String, notes: String?): Resource<Boolean>

    fun getActiveResponse(): Flow<Resource<HelperResponse?>>

    suspend fun getResponseHistory(): Resource<List<HelperResponse>>

    fun getHelperStats(): Flow<HelperStats>

    data class HelperStats(
        val totalResponses: Int,
        val successfulResponses: Int,
        val averageResponseTime: Int,
        val rating: Float
    )
}
