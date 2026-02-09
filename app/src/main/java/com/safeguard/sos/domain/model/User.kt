package com.safeguard.sos.domain.model

data class User(
    val id: String,
    val fullName: String,
    val phoneNumber: String,
    val email: String? = null,
    val aadhaarNumber: String? = null,
    val profileImageUrl: String? = null,
    val userType: UserType = UserType.REGULAR,
    val verificationStatus: VerificationStatus = VerificationStatus.NOT_VERIFIED,
    val isHelper: Boolean = false,
    val helperStatus: HelperStatus? = null,
    val address: Address? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    val bloodGroup: String? = null,
    val dateOfBirth: String? = null,
    val gender: Gender? = null,
    val medicalInfo: MedicalInfo? = null,
    val fcmToken: String? = null,
    val lastLocation: Location? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long? = null,
    val isActive: Boolean = true
) {
    val name: String get() = fullName
    val phone: String get() = phoneNumber
    val isVerified: Boolean get() = verificationStatus == VerificationStatus.VERIFIED
}
