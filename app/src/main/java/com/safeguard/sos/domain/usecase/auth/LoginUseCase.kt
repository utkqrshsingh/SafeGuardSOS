// app/src/main/java/com/safeguard/sos/domain/usecase/auth/LoginUseCase.kt

package com.safeguard.sos.domain.usecase.auth

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.User
import com.safeguard.sos.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(phoneNumber: String, password: String): Resource<User> {
        // Validate inputs
        if (phoneNumber.isBlank()) {
            return Resource.Error("Phone number is required")
        }

        if (phoneNumber.length != 10) {
            return Resource.Error("Please enter a valid 10-digit phone number")
        }

        if (password.isBlank()) {
            return Resource.Error("Password is required")
        }

        if (password.length < 8) {
            return Resource.Error("Password must be at least 8 characters")
        }

        return authRepository.login(phoneNumber, password)
    }
}