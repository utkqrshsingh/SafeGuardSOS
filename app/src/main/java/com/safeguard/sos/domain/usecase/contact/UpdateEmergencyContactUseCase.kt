// app/src/main/java/com/safeguard/sos/domain/usecase/contact/UpdateEmergencyContactUseCase.kt

package com.safeguard.sos.domain.usecase.contact

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.core.extensions.isValidName
import com.safeguard.sos.core.extensions.isValidPhoneNumber
import com.safeguard.sos.domain.model.EmergencyContact
import com.safeguard.sos.domain.repository.EmergencyContactRepository
import javax.inject.Inject

class UpdateEmergencyContactUseCase @Inject constructor(
    private val contactRepository: EmergencyContactRepository
) {
    suspend operator fun invoke(contact: EmergencyContact): Resource<EmergencyContact> {
        // Validate name
        if (contact.name.isBlank()) {
            return Resource.Error("Contact name is required")
        }

        if (!contact.name.isValidName()) {
            return Resource.Error("Please enter a valid name")
        }

        // Validate phone number
        if (!contact.phoneNumber.isValidPhoneNumber()) {
            return Resource.Error("Please enter a valid phone number")
        }

        return contactRepository.updateContact(contact)
    }
}