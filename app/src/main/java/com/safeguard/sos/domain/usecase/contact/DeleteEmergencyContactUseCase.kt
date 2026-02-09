// app/src/main/java/com/safeguard/sos/domain/usecase/contact/DeleteEmergencyContactUseCase.kt

package com.safeguard.sos.domain.usecase.contact

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.repository.EmergencyContactRepository
import javax.inject.Inject

class DeleteEmergencyContactUseCase @Inject constructor(
    private val contactRepository: EmergencyContactRepository
) {
    suspend operator fun invoke(contactId: String): Resource<Boolean> {
        if (contactId.isBlank()) {
            return Resource.Error("Invalid contact ID")
        }

        return contactRepository.deleteContact(contactId)
    }
}