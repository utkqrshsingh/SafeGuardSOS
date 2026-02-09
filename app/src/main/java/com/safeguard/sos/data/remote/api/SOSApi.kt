// app/src/main/java/com/safeguard/sos/data/remote/api/SOSApi.kt

package com.safeguard.sos.data.remote.api

import com.safeguard.sos.data.remote.dto.request.SOSRequest
import com.safeguard.sos.data.remote.dto.request.UpdateLocationRequest
import com.safeguard.sos.data.remote.dto.response.BaseResponse
import com.safeguard.sos.data.remote.dto.response.NearbyHelpersResponse
import com.safeguard.sos.data.remote.dto.response.SOSAlertResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface SOSApi {

    @POST("sos")
    suspend fun triggerSOS(@Body request: SOSRequest): Response<SOSAlertResponse>

    @PATCH("sos/{sosId}/cancel")
    suspend fun cancelSOS(@Path("sosId") sosId: String): Response<BaseResponse>

    @PATCH("sos/{sosId}/resolve")
    suspend fun resolveSOS(@Path("sosId") sosId: String): Response<BaseResponse>

    @PATCH("sos/{sosId}/false-alarm")
    suspend fun markAsFalseAlarm(@Path("sosId") sosId: String): Response<BaseResponse>

    @GET("sos/active")
    suspend fun getActiveSOSAlert(): Response<SOSAlertResponse>

    @GET("sos/{sosId}")
    suspend fun getSOSById(@Path("sosId") sosId: String): Response<SOSAlertResponse>

    @GET("sos/history")
    suspend fun getSOSHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<List<SOSAlertResponse>>

    @PATCH("sos/{sosId}/location")
    suspend fun updateSOSLocation(
        @Path("sosId") sosId: String,
        @Body request: UpdateLocationRequest
    ): Response<BaseResponse>

    @GET("sos/nearby-helpers")
    suspend fun getNearbyHelpers(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Int
    ): Response<NearbyHelpersResponse>

    @GET("sos/{sosId}/responders")
    suspend fun getSOSResponders(@Path("sosId") sosId: String): Response<List<Any>>

    @POST("sos/{sosId}/notify-contacts")
    suspend fun notifyEmergencyContacts(@Path("sosId") sosId: String): Response<BaseResponse>

    @Multipart
    @POST("sos/{sosId}/audio")
    suspend fun uploadAudioRecording(
        @Path("sosId") sosId: String,
        @Part audio: MultipartBody.Part
    ): Response<BaseResponse>

    @Multipart
    @POST("sos/{sosId}/video")
    suspend fun uploadVideoRecording(
        @Path("sosId") sosId: String,
        @Part video: MultipartBody.Part
    ): Response<BaseResponse>
}
