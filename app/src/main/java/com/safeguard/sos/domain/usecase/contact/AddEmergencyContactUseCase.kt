// app/src/main/java/com/safeguard/sos/domain/usecase/contact/AddEmergencyContactUseCase.kt

package com.safeguard.sos.domain.usecase.contact

import com.safeguard.sos.core.common.Constants
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.core.extensions.isValidName
import com.safeguard.sos.core.extensions.isValidPhoneNumber
import com.safeguard.sos.domain.model.EmergencyContact
import com.safeguard.sos.domain.model.Relationship
import com.safeguard.sos.domain.repository.EmergencyContactRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AddEmergencyContactUseCase @Inject constructor(
    private val contactRepository: EmergencyContactRepository
) {
    suspend operator fun invoke(
        name: String,
        phoneNumber: String,
        relationship: Relationship,
        isPrimary: Boolean = false,
        notifyViaSms: Boolean = true,
        notifyViaCall: Boolean = false
    ): Resource<EmergencyContact> {
        // Validate name
        if (name.isBlank()) {
            return Resource.Error("Contact name is required")
        }

        if (!name.isValidName()) {
            return Resource.Error("Please enter a valid name")
        }

        // Validate phone number
        val cleanedPhone = phoneNumber.replace(Regex("[^0-9]"), "")
        if (!cleanedPhone.isValidPhoneNumber()) {
            return Resource.Error("Please enter a valid 10-digit phone number")
        }

        // Check if contact already exists
        if (contactRepository.contactExists(cleanedPhone)) {
            return Resource.Error("A contact with this phone number already exists")
        }

        // Check max contacts limit
        val currentCount = contactRepository.getContactCount().first()
        if (currentCount >= Constants.MAX_EMERGENCY_CONTACTS) {
            return Resource.Error("Maximum ${Constants.MAX_EMERGENCY_CONTACTS} contacts allowed")
        }

        return contactRepository.addContact(
            name = name.trim(),
            phoneNumber = cleanedPhone,
            relationship = relationship,
            isPrimary = isPrimary,
            notifyViaSms = notifyViaSms,
            notifyViaCall = notifyViaCall
        )
    }
}
