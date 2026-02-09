package com.safeguard.sos.data.remote.dto.request

import com.google.gson.annotations.SerializedName

data class UpdateUserRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("email")
    val email: String?,

    @SerializedName("address")
    val address: String?,

    @SerializedName("city")
    val city: String?,

    @SerializedName("state")
    val state: String?,

    @SerializedName("pincode")
    val pincode: String?,

    @SerializedName("bloodGroup")
    val bloodGroup: String?,

    @SerializedName("dateOfBirth")
    val dateOfBirth: String?,

    @SerializedName("gender")
    val gender: String?,

    @SerializedName("profileImageUrl")
    val profileImageUrl: String?
)