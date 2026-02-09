// app/src/main/java/com/safeguard/sos/domain/usecase/auth/RegisterUseCase.kt

package com.safeguard.sos.domain.usecase.auth

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.core.extensions.isValidEmail
import com.safeguard.sos.core.extensions.isValidName
import com.safeguard.sos.core.extensions.isValidPhoneNumber
import com.safeguard.sos.domain.model.User
import com.safeguard.sos.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        fullName: String,
        phoneNumber: String,
        email: String?,
        password: String,
        confirmPassword: String,
        userType: String
    ): Resource<User> {
        // Validate full name
        if (fullName.isBlank()) {
            return Resource.Error("Full name is required")
        }

        if (!fullName.isValidName()) {
            return Resource.Error("Please enter a valid name")
        }

        // Validate phone number
        if (phoneNumber.isBlank()) {
            return Resource.Error("Phone number is required")
        }

        if (!phoneNumber.isValidPhoneNumber()) {
            return Resource.Error("Please enter a valid 10-digit phone number")
        }

        // Validate email if provided
        if (!email.isNullOrBlank() && !email.isValidEmail()) {
            return Resource.Error("Please enter a valid email address")
        }

        // Validate password
        if (password.isBlank()) {
            return Resource.Error("Password is required")
        }

        if (password.length < 8) {
            return Resource.Error("Password must be at least 8 characters")
        }

        // Validate confirm password
        if (password != confirmPassword) {
            return Resource.Error("Passwords do not match")
        }

        // Validate user type
        if (userType.isBlank()) {
            return Resource.Error("Please select a user type")
        }

        return authRepository.register(
            fullName = fullName.trim(),
            phoneNumber = phoneNumber.trim(),
            email = email?.trim(),
            password = password,
            userType = userType
        )
    }
}