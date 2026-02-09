// app/src/main/java/com/safeguard/sos/domain/usecase/sos/ResolveSOSUseCase.kt

package com.safeguard.sos.domain.usecase.sos

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.repository.SOSRepository
import javax.inject.Inject

class ResolveSOSUseCase @Inject constructor(
    private val sosRepository: SOSRepository
) {
    suspend operator fun invoke(sosId: String, isFalseAlarm: Boolean = false): Resource<Boolean> {
        if (sosId.isBlank()) {
            return Resource.Error("Invalid SOS ID")
        }

        return if (isFalseAlarm) {
            sosRepository.markAsFalseAlarm(sosId)
        } else {
            sosRepository.resolveSOS(sosId)
        }
    }
}