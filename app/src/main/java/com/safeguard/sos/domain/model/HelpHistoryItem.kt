package com.safeguard.sos.domain.model

data class HelpHistoryItem(
    val id: String,
    val alertId: String,
    val userId: String,
    val userName: String,
    val emergencyType: String,
    val timestamp: Long,
    val responseTime: String?, // e.g., "3m"
    val rating: Float?,
    val feedback: String?
)