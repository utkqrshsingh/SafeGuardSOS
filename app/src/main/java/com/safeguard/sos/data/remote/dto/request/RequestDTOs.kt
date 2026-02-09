// app/src/main/java/com/safeguard/sos/data/remote/dto/request/RequestDTOs.kt

package com.safeguard.sos.data.remote.dto.request

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("password")
    val password: String
)

data class RegisterRequest(
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("email")
    val email: String?,
    @SerializedName("password")
    val password: String,
    @SerializedName("user_type")
    val userType: String
)

data class VerifyOtpRequest(
    @SerializedName("phone_number")
    val phoneNumber: String?,
    @SerializedName("aadhaar_number")
    val aadhaarNumber: String?,
    @SerializedName("otp")
    val otp: String
)

data class VerifyAadhaarRequest(
    @SerializedName("aadhaar_number")
    val aadhaarNumber: String
)

data class SOSRequest(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("accuracy")
    val accuracy: Float? = null,
    @SerializedName("address")
    val address: String?,
    @SerializedName("alert_type")
    val alertType: String,
    @SerializedName("message")
    val message: String?
)

data class UpdateLocationRequest(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("accuracy")
    val accuracy: Float?,
    @SerializedName("timestamp")
    val timestamp: Long
)

data class HelperResponseRequest(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("estimated_arrival_minutes")
    val estimatedArrivalMinutes: Int?
)