// app/src/main/java/com/safeguard/sos/data/remote/api/HelperApi.kt

package com.safeguard.sos.data.remote.api

import com.safeguard.sos.data.remote.dto.request.HelperResponseRequest
import com.safeguard.sos.data.remote.dto.request.UpdateLocationRequest
import com.safeguard.sos.data.remote.dto.response.BaseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface HelperApi {

    @POST("helpers/register")
    suspend fun registerAsHelper(@Body request: Map<String, Any?>): Response<Any>

    @GET("helpers/me")
    suspend fun getHelperProfile(): Response<Any>

    @PATCH("helpers/me/status")
    suspend fun updateHelperStatus(@Body request: Map<String, Any>): Response<BaseResponse>

    @PATCH("helpers/me/location")
    suspend fun updateHelperLocation(@Body request: UpdateLocationRequest): Response<BaseResponse>

    @PATCH("helpers/me/radius")
    suspend fun updateHelperRadius(@Body request: Map<String, Int>): Response<BaseResponse>

    @GET("helpers/nearby-sos")
    suspend fun getNearbySOSAlerts(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("radiusKm") radiusKm: Int
    ): Response<List<Any>>

    @POST("helpers/respond/{sosId}")
    suspend fun respondToSOS(
        @Path("sosId") sosId: String,
        @Body request: HelperResponseRequest
    ): Response<Any>

    @PATCH("helpers/response/{responseId}/cancel")
    suspend fun cancelResponse(@Path("responseId") responseId: String): Response<BaseResponse>

    @PATCH("helpers/response/{responseId}/arrived")
    suspend fun markArrived(@Path("responseId") responseId: String): Response<BaseResponse>

    @PATCH("helpers/response/{responseId}/complete")
    suspend fun completeResponse(
        @Path("responseId") responseId: String,
        @Body notes: Map<String, String?>
    ): Response<BaseResponse>

    @GET("helpers/active-response")
    suspend fun getActiveResponse(): Response<Any?>

    @GET("helpers/response-history")
    suspend fun getResponseHistory(): Response<List<Any>>

    @GET("helpers/stats")
    suspend fun getHelperStats(): Response<Any>
}
