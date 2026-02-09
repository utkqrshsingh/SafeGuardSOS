package com.safeguard.sos.domain.usecase.auth

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        aadhaarNumber: String,
        otp: String
    ): Resource<Boolean> {
        return authRepository.verifyAadhaarOtp(aadhaarNumber, otp)
    }
}
