// app/src/main/java/com/safeguard/sos/domain/repository/AuthRepository.kt

package com.safeguard.sos.domain.repository

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.data.remote.dto.response.OtpResponse
import com.safeguard.sos.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    suspend fun login(phoneNumber: String, password: String): Resource<User>

    suspend fun loginWithGoogle(idToken: String): Resource<User>

    suspend fun register(
        fullName: String,
        phoneNumber: String,
        email: String?,
        password: String,
        userType: String
    ): Resource<User>

    suspend fun sendOtp(phoneNumber: String): Resource<String>

    suspend fun verifyOtp(phoneNumber: String, otp: String): Resource<Boolean>

    fun sendAadhaarOtp(aadhaarNumber: String): Flow<Resource<OtpResponse>>

    suspend fun verifyAadhaar(aadhaarNumber: String): Resource<String>

    suspend fun verifyAadhaarOtp(aadhaarNumber: String, otp: String): Resource<Boolean>

    fun sendPasswordResetEmail(email: String): Flow<Resource<Boolean>>

    fun sendPasswordResetSms(phoneNumber: String): Flow<Resource<Boolean>>

    suspend fun resetPassword(token: String, newPassword: String): Resource<Boolean>

    suspend fun changePassword(currentPassword: String, newPassword: String): Resource<Boolean>

    suspend fun logout(): Resource<Boolean>

    suspend fun refreshToken(): Resource<String>

    fun isLoggedIn(): Flow<Boolean>

    fun getCurrentUserId(): Flow<String?>

    suspend fun getCurrentUserIdSync(): String?
}