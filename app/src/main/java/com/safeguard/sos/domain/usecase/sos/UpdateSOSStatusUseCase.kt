package com.safeguard.sos.domain.usecase.sos

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.model.SOSStatus
import com.safeguard.sos.domain.repository.SOSRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UpdateSOSStatusUseCase @Inject constructor(
    private val sosRepository: SOSRepository
) {
    operator fun invoke(sosId: String, status: SOSStatus): Flow<Resource<SOSAlert>> {
        return sosRepository.updateSOSStatus(sosId, status)
    }
}
