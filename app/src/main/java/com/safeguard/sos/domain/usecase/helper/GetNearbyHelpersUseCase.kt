package com.safeguard.sos.domain.usecase.helper

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.Helper
import com.safeguard.sos.domain.repository.SOSRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetNearbyHelpersUseCase @Inject constructor(
    private val sosRepository: SOSRepository
) {
    operator fun invoke(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Flow<Resource<List<Helper>>> = flow {
        emit(Resource.Loading)
        when (val resource = sosRepository.getNearbyHelpers(latitude, longitude, radiusKm.toInt())) {
            is Resource.Success -> {
                val helpers = resource.data.map { it.helper }
                emit(Resource.Success(helpers))
            }
            is Resource.Error -> {
                emit(Resource.Error(resource.message, resource.errorCode, resource.exception))
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
