// app/src/main/java/com/safeguard/sos/domain/usecase/sos/GetActiveSOSUseCase.kt

package com.safeguard.sos.domain.usecase.sos

import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.repository.SOSRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetActiveSOSUseCase @Inject constructor(
    private val sosRepository: SOSRepository
) {
    operator fun invoke(): Flow<SOSAlert?> {
        return sosRepository.getActiveSOSAlert()
    }

    suspend fun getSync(): SOSAlert? {
        return sosRepository.getActiveSOSAlertSync()
    }
}