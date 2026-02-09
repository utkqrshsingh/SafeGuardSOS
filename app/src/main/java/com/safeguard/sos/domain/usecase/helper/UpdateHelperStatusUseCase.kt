// app/src/main/java/com/safeguard/sos/domain/usecase/helper/UpdateHelperStatusUseCase.kt

package com.safeguard.sos.domain.usecase.helper

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.HelperStatus
import com.safeguard.sos.domain.repository.HelperRepository
import com.safeguard.sos.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.last
import javax.inject.Inject

class UpdateHelperStatusUseCase @Inject constructor(
    private val helperRepository: HelperRepository,
    private val locationRepository: LocationRepository
) {
    operator fun invoke(status: HelperStatus): Flow<Resource<HelperStatus>> = flow {
        emit(Resource.Loading)

        if (status == HelperStatus.AVAILABLE || status == HelperStatus.ACTIVE) {
            // When going active/available, update location first
            val locationResult = locationRepository.getCurrentLocation()
            if (locationResult is Resource.Success) {
                helperRepository.updateHelperLocation(locationResult.data)
            }
        }

        helperRepository.updateHelperStatus(status).collect {
            emit(it)
        }
    }
}
