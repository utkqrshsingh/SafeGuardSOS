package com.safeguard.sos.presentation.profile

import com.safeguard.sos.domain.model.User
import com.safeguard.sos.domain.model.VerificationStatus

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.NOT_VERIFIED,
    val emergencyContactCount: Int = 0,
    val sosCount: Int = 0,
    val helpsProvided: Int = 0,
    val memberSince: String = "",
    val error: String? = null
) {
    val isVerified: Boolean get() = verificationStatus == VerificationStatus.VERIFIED
    val displayName: String get() = user?.name ?: "User"
    val displayPhone: String get() = user?.phone ?: ""
    val displayEmail: String get() = user?.email ?: ""
    val isHelper: Boolean get() = user?.isHelper ?: false
}

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val bloodGroup: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val profileImageUri: String? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val hasChanges: Boolean = false,
    val isValid: Boolean = false,
    val originalUser: User? = null
)

data class MedicalInfoUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val bloodGroup: String = "",
    val allergies: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val medicalConditions: List<String> = emptyList(),
    val emergencyNotes: String = "",
    val organDonor: Boolean = false,
    val insuranceProvider: String = "",
    val insurancePolicyNumber: String = "",
    val primaryPhysician: String = "",
    val physicianPhone: String = "",
    val hasChanges: Boolean = false,
    val error: String? = null
)

sealed class ProfileEvent {
    data class ShowError(val message: String) : ProfileEvent()
    data class ShowSuccess(val message: String) : ProfileEvent()
    object NavigateToEditProfile : ProfileEvent()
    object NavigateToMedicalInfo : ProfileEvent()
    object NavigateToVerification : ProfileEvent()
    object ProfileUpdated : ProfileEvent()
    object LoggedOut : ProfileEvent()
}