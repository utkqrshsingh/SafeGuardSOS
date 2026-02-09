package com.safeguard.sos.domain.usecase.auth

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.data.remote.dto.response.OtpResponse
import com.safeguard.sos.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SendOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(aadhaarNumber: String): Flow<Resource<OtpResponse>> {
        return authRepository.sendAadhaarOtp(aadhaarNumber)
    }
}