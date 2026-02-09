// app/src/main/java/com/safeguard/sos/data/remote/api/AuthApi.kt

package com.safeguard.sos.data.remote.api

import com.safeguard.sos.data.remote.dto.request.LoginRequest
import com.safeguard.sos.data.remote.dto.request.RegisterRequest
import com.safeguard.sos.data.remote.dto.request.VerifyAadhaarRequest
import com.safeguard.sos.data.remote.dto.request.VerifyOtpRequest
import com.safeguard.sos.data.remote.dto.response.AuthResponse
import com.safeguard.sos.data.remote.dto.response.BaseResponse
import com.safeguard.sos.data.remote.dto.response.OtpResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/login/google")
    suspend fun loginWithGoogle(@Body idToken: Map<String, String>): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/send-otp")
    suspend fun sendOtp(@Body phoneNumber: Map<String, String>): Response<OtpResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<BaseResponse>

    @POST("auth/verify-aadhaar")
    suspend fun verifyAadhaar(@Body request: VerifyAadhaarRequest): Response<OtpResponse>

    @POST("auth/verify-aadhaar-otp")
    suspend fun verifyAadhaarOtp(@Body request: VerifyOtpRequest): Response<BaseResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body email: Map<String, String>): Response<BaseResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: Map<String, String>): Response<BaseResponse>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: Map<String, String>): Response<BaseResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<BaseResponse>

    @POST("auth/refresh-token")
    suspend fun refreshToken(): Response<AuthResponse>
}