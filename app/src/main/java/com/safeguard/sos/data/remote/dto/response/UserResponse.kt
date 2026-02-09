package com.safeguard.sos.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("full_name")
    val fullName: String,
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("email")
    val email: String?,
    @SerializedName("profile_image_url")
    val profileImageUrl: String?,
    @SerializedName("aadhaar_number")
    val aadhaarNumber: String?,
    @SerializedName("user_type")
    val userType: String,
    @SerializedName("verification_status")
    val verificationStatus: String,
    @SerializedName("is_helper")
    val isHelper: Boolean,
    @SerializedName("helper_status")
    val helperStatus: String?,
    @SerializedName("date_of_birth")
    val dateOfBirth: String?,
    @SerializedName("gender")
    val gender: String?,
    @SerializedName("blood_group")
    val bloodGroup: String?,
    @SerializedName("address")
    val address: AddressResponse?,
    @SerializedName("city")
    val city: String?,
    @SerializedName("state")
    val state: String?,
    @SerializedName("pincode")
    val pincode: String?,
    @SerializedName("medical_info")
    val medicalInfo: MedicalInfoResponse?,
    @SerializedName("fcm_token")
    val fcmToken: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)
