package com.safeguard.sos.data.remote.dto.request

import com.google.gson.annotations.SerializedName

data class ResendOtpRequest(
    @SerializedName("aadhaar_number")
    val aadhaarNumber: String,
    @SerializedName("transaction_id")
    val transactionId: String
)
