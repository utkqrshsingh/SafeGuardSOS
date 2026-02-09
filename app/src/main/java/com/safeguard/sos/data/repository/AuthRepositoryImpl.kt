package com.safeguard.sos.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.data.local.datastore.UserPreferences
import com.safeguard.sos.data.mapper.UserMapper
import com.safeguard.sos.data.remote.api.AuthApi
import com.safeguard.sos.data.remote.dto.request.LoginRequest
import com.safeguard.sos.data.remote.dto.request.RegisterRequest
import com.safeguard.sos.data.remote.dto.request.VerifyAadhaarRequest
import com.safeguard.sos.data.remote.dto.request.VerifyOtpRequest
import com.safeguard.sos.data.remote.dto.response.OtpResponse
import com.safeguard.sos.domain.model.User
import com.safeguard.sos.domain.model.UserType
import com.safeguard.sos.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val firebaseAuth: FirebaseAuth,
    private val userPreferences: UserPreferences,
    private val userMapper: UserMapper
) : AuthRepository {

    override suspend fun login(phoneNumber: String, password: String): Resource<User> {
        return try {
            val response = authApi.login(LoginRequest(phoneNumber, password))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                if (authResponse.success && authResponse.data != null) {
                    val user = userMapper.mapResponseToDomain(authResponse.data.user)

                    // Save user data
                    userPreferences.setUserData(
                        userId = user.id,
                        name = user.fullName,
                        phone = user.phoneNumber,
                        email = user.email
                    )
                    userPreferences.setAuthToken(authResponse.data.token)
                    authResponse.data.refreshToken?.let { userPreferences.setRefreshToken(it) }
                    userPreferences.setIsHelper(user.isHelper)

                    Resource.Success(user)
                } else {
                    Resource.Error(authResponse.message ?: "Login failed")
                }
            } else {
                Resource.Error("Login failed: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Resource<User> {
        return try {
            val response = authApi.loginWithGoogle(mapOf("idToken" to idToken))
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                if (authResponse.success && authResponse.data != null) {
                    val user = userMapper.mapResponseToDomain(authResponse.data.user)

                    // Save user data
                    userPreferences.setUserData(
                        userId = user.id,
                        name = user.fullName,
                        phone = user.phoneNumber,
                        email = user.email
                    )
                    userPreferences.setAuthToken(authResponse.data.token)
                    authResponse.data.refreshToken?.let { userPreferences.setRefreshToken(it) }
                    userPreferences.setIsHelper(user.isHelper)

                    Resource.Success(user)
                } else {
                    Resource.Error(authResponse.message ?: "Google login failed")
                }
            } else {
                Resource.Error("Google login failed: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    override suspend fun register(
        fullName: String,
        phoneNumber: String,
        email: String?,
        password: String,
        userType: String
    ): Resource<User> {
        return try {
            val request = RegisterRequest(
                fullName = fullName,
                phoneNumber = phoneNumber,
                email = email,
                password = password,
                userType = userType
            )

            val response = authApi.register(request)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                if (authResponse.success && authResponse.data != null) {
                    val user = userMapper.mapResponseToDomain(authResponse.data.user)

                    // Save user data
                    userPreferences.setUserData(
                        userId = user.id,
                        name = user.fullName,
                        phone = user.phoneNumber,
                        email = user.email
                    )
                    userPreferences.setAuthToken(authResponse.data.token)
                    authResponse.data.refreshToken?.let { userPreferences.setRefreshToken(it) }
                    userPreferences.setIsHelper(userType == UserType.HELPER.value)

                    Resource.Success(user)
                } else {
                    Resource.Error(authResponse.message ?: "Registration failed")
                }
            } else {
                Resource.Error("Registration failed: ${response.message()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    override suspend fun sendOtp(phoneNumber: String): Resource<String> {
        return try {
            val response = authApi.sendOtp(mapOf("phone" to phoneNumber))
            if (response.isSuccessful && response.body() != null) {
                val otpResponse = response.body()!!
                if (otpResponse.success) {
                    Resource.Success(otpResponse.message ?: "OTP sent successfully")
                } else {
                    Resource.Error(otpResponse.message ?: "Failed to send OTP")
                }
            } else {
                Resource.Error("Failed to send OTP")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send OTP")
        }
    }

    override suspend fun verifyOtp(phoneNumber: String, otp: String): Resource<Boolean> {
        return try {
            val request = VerifyOtpRequest(phoneNumber = phoneNumber, aadhaarNumber = null, otp = otp)
            val response = authApi.verifyOtp(request)

            if (response.isSuccessful && response.body() != null) {
                val otpResponse = response.body()!!
                if (otpResponse.success) {
                    Resource.Success(true)
                } else {
                    Resource.Error(otpResponse.message ?: "Invalid OTP")
                }
            } else {
                Resource.Error("OTP verification failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "OTP verification failed")
        }
    }

    override fun sendAadhaarOtp(aadhaarNumber: String): Flow<Resource<OtpResponse>> = flow {
        emit(Resource.Loading)
        try {
            val request = VerifyAadhaarRequest(aadhaarNumber = aadhaarNumber)
            val response = authApi.verifyAadhaar(request)

            if (response.isSuccessful && response.body() != null) {
                val otpResponse = response.body()!!
                if (otpResponse.success) {
                    emit(Resource.Success(otpResponse))
                } else {
                    emit(Resource.Error(otpResponse.message ?: "Failed to send OTP to Aadhaar linked mobile"))
                }
            } else {
                emit(Resource.Error("Failed to send OTP. Please try again."))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred"))
        }
    }

    override suspend fun verifyAadhaar(aadhaarNumber: String): Resource<String> {
        return try {
            val request = VerifyAadhaarRequest(aadhaarNumber = aadhaarNumber)
            val response = authApi.verifyAadhaar(request)

            if (response.isSuccessful && response.body() != null) {
                val aadhaarResponse = response.body()!!
                if (aadhaarResponse.success) {
                    Resource.Success(aadhaarResponse.message ?: "Aadhaar verification successful")
                } else {
                    Resource.Error(aadhaarResponse.message ?: "Aadhaar verification failed")
                }
            } else {
                Resource.Error("Aadhaar verification failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Aadhaar verification failed")
        }
    }

    override suspend fun verifyAadhaarOtp(aadhaarNumber: String, otp: String): Resource<Boolean> {
        return try {
            val request = VerifyOtpRequest(phoneNumber = null, aadhaarNumber = aadhaarNumber, otp = otp)
            val response = authApi.verifyAadhaarOtp(request)

            if (response.isSuccessful && response.body() != null) {
                val baseResponse = response.body()!!
                if (baseResponse.success) {
                    Resource.Success(true)
                } else {
                    Resource.Error(baseResponse.message ?: "Invalid OTP")
                }
            } else {
                Resource.Error("OTP verification failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "OTP verification failed")
        }
    }

    override fun sendPasswordResetEmail(email: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to send reset email"))
        }
    }

    override fun sendPasswordResetSms(phoneNumber: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        try {
            val response = authApi.forgotPassword(mapOf("phone" to phoneNumber))
            if (response.isSuccessful && response.body() != null) {
                val baseResponse = response.body()!!
                if (baseResponse.success) {
                    emit(Resource.Success(true))
                } else {
                    emit(Resource.Error(baseResponse.message ?: "Failed to send reset SMS"))
                }
            } else {
                emit(Resource.Error("Failed to send reset SMS"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An error occurred"))
        }
    }

    override suspend fun resetPassword(token: String, newPassword: String): Resource<Boolean> {
        return try {
            val response = authApi.resetPassword(mapOf("token" to token, "password" to newPassword))
            if (response.isSuccessful && response.body() != null) {
                val baseResponse = response.body()!!
                if (baseResponse.success) {
                    Resource.Success(true)
                } else {
                    Resource.Error(baseResponse.message ?: "Failed to reset password")
                }
            } else {
                Resource.Error("Failed to reset password")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Resource<Boolean> {
        return try {
            val response = authApi.changePassword(
                mapOf("current_password" to currentPassword, "new_password" to newPassword)
            )
            if (response.isSuccessful && response.body() != null) {
                val baseResponse = response.body()!!
                if (baseResponse.success) {
                    Resource.Success(true)
                } else {
                    Resource.Error(baseResponse.message ?: "Failed to change password")
                }
            } else {
                Resource.Error("Failed to change password")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }

    override suspend fun logout(): Resource<Boolean> {
        return try {
            authApi.logout()
            firebaseAuth.signOut()
            userPreferences.clearUserData()
            Resource.Success(true)
        } catch (e: Exception) {
            userPreferences.clearUserData()
            Resource.Success(true)
        }
    }

    override suspend fun refreshToken(): Resource<String> {
        return try {
            val response = authApi.refreshToken()
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                if (authResponse.success && authResponse.data?.token != null) {
                    userPreferences.setAuthToken(authResponse.data.token)
                    Resource.Success(authResponse.data.token)
                } else {
                    Resource.Error("Token refresh failed")
                }
            } else {
                Resource.Error("Token refresh failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Token refresh failed")
        }
    }

    override fun isLoggedIn(): Flow<Boolean> = userPreferences.isLoggedInFlow

    override fun getCurrentUserId(): Flow<String?> = userPreferences.userIdFlow

    override suspend fun getCurrentUserIdSync(): String? {
        return userPreferences.userIdFlow.first()
    }
}
