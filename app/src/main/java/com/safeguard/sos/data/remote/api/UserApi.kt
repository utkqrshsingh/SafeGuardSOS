// app/src/main/java/com/safeguard/sos/data/remote/api/UserApi.kt

package com.safeguard.sos.data.remote.api

import com.safeguard.sos.data.remote.dto.response.BaseResponse
import com.safeguard.sos.data.remote.dto.response.UserResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface UserApi {

    @GET("users/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @GET("users/{userId}")
    suspend fun getUserById(@Path("userId") userId: String): Response<UserResponse>

    @PATCH("users/me")
    suspend fun updateUser(@Body request: Map<String, Any?>): Response<UserResponse>

    @PATCH("users/me/profile")
    suspend fun updateProfile(@Body request: Map<String, Any?>): Response<UserResponse>

    @Multipart
    @POST("users/me/profile-image")
    suspend fun uploadProfileImage(@Part image: MultipartBody.Part): Response<UserResponse>

    @PATCH("users/me/address")
    suspend fun updateAddress(@Body request: Map<String, Any?>): Response<UserResponse>

    @PATCH("users/me/medical-info")
    suspend fun updateMedicalInfo(@Body request: Map<String, Any?>): Response<UserResponse>

    @PATCH("users/me/fcm-token")
    suspend fun updateFcmToken(@Body token: Map<String, String>): Response<BaseResponse>

    @DELETE("users/me")
    suspend fun deleteAccount(): Response<BaseResponse>
}