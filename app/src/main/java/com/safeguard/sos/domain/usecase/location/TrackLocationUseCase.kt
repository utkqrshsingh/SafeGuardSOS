// app/src/main/java/com/safeguard/sos/domain/usecase/location/TrackLocationUseCase.kt

package com.safeguard.sos.domain.usecase.location

import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TrackLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    operator fun invoke(): Flow<Location> {
        return locationRepository.getLocationUpdates()
    }

    suspend fun startTracking() {
        locationRepository.startLocationTracking()
    }

    suspend fun stopTracking() {
        locationRepository.stopLocationTracking()
    }

    fun isTrackingActive(): Flow<Boolean> {
        return locationRepository.isLocationTrackingActive()
    }
}