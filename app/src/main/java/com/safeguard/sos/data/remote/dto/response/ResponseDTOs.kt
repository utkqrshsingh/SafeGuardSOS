// app/src/main/java/com/safeguard/sos/data/remote/dto/response/ResponseDTOs.kt

package com.safeguard.sos.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class BaseResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String?,
    @SerializedName("data")
    val data: Any?
)

data class AuthResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String?,
    @SerializedName("data")
    val data: AuthData?
)

data class AuthData(
    @SerializedName("user")
    val user: UserResponse,
    @SerializedName("token")
    val token: String,
    @SerializedName("refresh_token")
    val refreshToken: String?
)

data class AddressResponse(
    @SerializedName("street")
    val street: String?,
    @SerializedName("city")
    val city: String?,
    @SerializedName("state")
    val state: String?,
    @SerializedName("pincode")
    val pincode: String?,
    @SerializedName("country")
    val country: String?
)

data class MedicalInfoResponse(
    @SerializedName("conditions")
    val conditions: List<String>?,
    @SerializedName("allergies")
    val allergies: List<String>?,
    @SerializedName("medications")
    val medications: List<String>?,
    @SerializedName("emergency_notes")
    val emergencyNotes: String?,
    @SerializedName("organ_donor")
    val organDonor: Boolean?
)

data class OtpResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("message")
    val message: String?,
    @SerializedName("request_id")
    val requestId: String?,
    @SerializedName("masked_phone")
    val maskedPhone: String?
)

data class LocationResponse(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("accuracy")
    val accuracy: Float?,
    @SerializedName("altitude")
    val altitude: Double?,
    @SerializedName("address")
    val address: String?,
    @SerializedName("city")
    val city: String?,
    @SerializedName("state")
    val state: String?,
    @SerializedName("timestamp")
    val timestamp: Long?
)

data class NearbyHelpersResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("helpers")
    val helpers: List<NearbyHelperResponse>
)

data class NearbyHelperResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("photo_url")
    val photoUrl: String?,
    @SerializedName("rating")
    val rating: Float,
    @SerializedName("total_responses")
    val totalResponses: Int,
    @SerializedName("distance_km")
    val distanceKm: Float,
    @SerializedName("estimated_arrival_minutes")
    val estimatedArrivalMinutes: Int
)
