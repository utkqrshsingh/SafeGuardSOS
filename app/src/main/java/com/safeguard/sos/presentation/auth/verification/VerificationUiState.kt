// app/src/main/java/com/safeguard/sos/presentation/auth/verification/VerificationUiState.kt

package com.safeguard.sos.presentation.auth.verification

data class VerificationUiState(
    val isLoading: Boolean = false,
    val isOtpSent: Boolean = false,
    val isOtpVerifying: Boolean = false,
    val isVerified: Boolean = false,
    val isLocked: Boolean = false,
    val aadhaarNumber: String = "",
    val maskedPhone: String = "",
    val otpAttempts: Int = 0,
    val error: String? = null
)
