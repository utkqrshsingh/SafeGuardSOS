package com.safeguard.sos.data.remote.dto.request

import com.google.gson.annotations.SerializedName

data class DeviceInfo(
    @SerializedName("device_id")
    val deviceId: String?,
    @SerializedName("device_model")
    val deviceModel: String?,
    @SerializedName("os_version")
    val osVersion: String?,
    @SerializedName("app_version")
    val appVersion: String?
)
