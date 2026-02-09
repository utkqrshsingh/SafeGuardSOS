// app/src/main/java/com/safeguard/sos/domain/repository/LocationRepository.kt

package com.safeguard.sos.domain.repository

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.Location
import kotlinx.coroutines.flow.Flow

interface LocationRepository {

    suspend fun getCurrentLocation(): Resource<Location>

    fun getLocationUpdates(): Flow<Location>

    suspend fun getLastKnownLocation(): Location?

    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): Resource<String>

    fun startLocationTracking()

    fun stopLocationTracking()

    fun isLocationTrackingActive(): Flow<Boolean>

    suspend fun saveLocation(location: Location)

    fun calculateDistance(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Float
}