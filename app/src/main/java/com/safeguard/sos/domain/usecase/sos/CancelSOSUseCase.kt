// app/src/main/java/com/safeguard/sos/domain/usecase/sos/CancelSOSUseCase.kt

package com.safeguard.sos.domain.usecase.sos

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.repository.SOSRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CancelSOSUseCase @Inject constructor(
    private val sosRepository: SOSRepository
) {
    operator fun invoke(sosId: String): Flow<Resource<Unit>> {
        return sosRepository.cancelSOS(sosId)
    }
}
