package com.safeguard.sos.domain.usecase.helper

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.repository.HelperRepository
import com.safeguard.sos.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class RespondToSOSUseCase @Inject constructor(
    private val helperRepository: HelperRepository,
    private val locationRepository: LocationRepository
) {
    operator fun invoke(sosId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        val locationResult = locationRepository.getCurrentLocation()
        if (locationResult is Resource.Error) {
            emit(Resource.Error(locationResult.message, locationResult.errorCode, locationResult.exception))
            return@flow
        }

        val location = (locationResult as? Resource.Success)?.data
        if (location == null) {
            emit(Resource.Error("Could not get current location"))
            return@flow
        }

        when (val result = helperRepository.respondToSOS(sosId, location)) {
            is Resource.Success -> {
                emit(Resource.Success(Unit))
            }
            is Resource.Error -> {
                emit(Resource.Error(result.message, result.errorCode, result.exception))
            }
            is Resource.Loading -> {
                emit(Resource.Loading)
            }
            is Resource.Empty -> {
                emit(Resource.Empty)
            }
        }
    }
}
