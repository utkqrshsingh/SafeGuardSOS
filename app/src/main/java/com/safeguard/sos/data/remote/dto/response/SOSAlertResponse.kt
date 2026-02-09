package com.safeguard.sos.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class SOSAlertResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("userName")
    val userName: String?,

    @SerializedName("userPhone")
    val userPhone: String?,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("address")
    val address: String?,

    @SerializedName("emergencyType")
    val emergencyType: String?,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("status")
    val status: String?,

    @SerializedName("respondedHelpers")
    val respondedHelpers: List<String>? = null,

    @SerializedName("createdAt")
    val createdAt: Long?,

    @SerializedName("resolvedAt")
    val resolvedAt: Long? = null,

    @SerializedName("cancelledAt")
    val cancelledAt: Long? = null
)
