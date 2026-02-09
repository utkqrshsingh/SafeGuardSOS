package com.safeguard.sos.data.remote.dto.request

import com.google.gson.annotations.SerializedName

data class GoogleAuthRequest(
    @SerializedName("id_token")
    val idToken: String,

    @SerializedName("device_token")
    val deviceToken: String? = null,

    @SerializedName("device_info")
    val deviceInfo: DeviceInfo? = null
)