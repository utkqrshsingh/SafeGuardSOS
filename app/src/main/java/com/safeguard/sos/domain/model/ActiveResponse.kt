package com.safeguard.sos.domain.model

data class ActiveResponse(
    val alertId: String,
    val userId: String,
    val userName: String,
    val distance: String,
    val status: HelperStatus,
    val startedAt: Long
)