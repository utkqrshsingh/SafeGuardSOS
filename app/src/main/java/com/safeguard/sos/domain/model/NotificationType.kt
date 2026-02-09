// app/src/main/java/com/safeguard/sos/domain/model/NotificationType.kt

package com.safeguard.sos.domain.model

enum class NotificationType(val value: String) {
    SOS_ALERT("sos_alert"),
    SOS_CANCELLED("sos_cancelled"),
    SOS_RESOLVED("sos_resolved"),
    HELPER_RESPONDING("helper_responding"),
    HELPER_ARRIVED("helper_arrived"),
    HELPER_REQUEST("helper_request"),
    VERIFICATION_STATUS("verification_status"),
    SYSTEM("system");

    companion object {
        fun fromValue(value: String): NotificationType {
            return entries.find { it.value == value } ?: SYSTEM
        }
    }
}

data class PushNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: Map<String, String>,
    val receivedAt: Long,
    val isRead: Boolean = false
)