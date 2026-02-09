// app/src/main/java/com/safeguard/sos/domain/usecase/location/GetCurrentLocationUseCase.kt

package com.safeguard.sos.domain.usecase.location

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.repository.LocationRepository
import javax.inject.Inject

class GetCurrentLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(): Resource<Location> {
        return locationRepository.getCurrentLocation()
    }

    suspend fun getLastKnown(): Location? {
        return locationRepository.getLastKnownLocation()
    }
}