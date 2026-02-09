package com.safeguard.sos.domain.usecase.helper

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.repository.SOSRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNearbySOSAlertsUseCase @Inject constructor(
    private val sosRepository: SOSRepository
) {
    operator fun invoke(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Flow<Resource<List<SOSAlert>>> {
        return sosRepository.getNearbySOSAlerts(latitude, longitude, radiusKm)
    }
}