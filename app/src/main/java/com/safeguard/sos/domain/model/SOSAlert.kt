// app/src/main/java/com/safeguard/sos/domain/model/SOSAlert.kt

package com.safeguard.sos.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SOSAlert(
    val id: String,
    val userId: String,
    val userName: String,
    val userPhone: String,
    val userPhotoUrl: String? = null,
    val location: Location,
    val status: SOSStatus = SOSStatus.ACTIVE,
    val alertType: AlertType = AlertType.EMERGENCY,
    val message: String? = null,
    val audioRecordingUrl: String? = null,
    val videoRecordingUrl: String? = null,
    val respondersCount: Int = 0,
    val activeRespondersCount: Int = 0,
    val notifiedHelpersCount: Int = 0,
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val isFalseAlarm: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {

    val isActive: Boolean
        get() = status == SOSStatus.ACTIVE || status == SOSStatus.PENDING || status == SOSStatus.HELP_ON_WAY

    val isResolved: Boolean
        get() = status == SOSStatus.RESOLVED || status == SOSStatus.CANCELLED

    val durationMinutes: Long
        get() {
            val endTime = resolvedAt ?: System.currentTimeMillis()
            return (endTime - createdAt) / 60000
        }

    val hasRecording: Boolean
        get() = !audioRecordingUrl.isNullOrEmpty() || !videoRecordingUrl.isNullOrEmpty()
}

@Parcelize
data class Location(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val altitude: Double? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {

    val displayAddress: String
        get() = address ?: "$latitude, $longitude"

    val shortAddress: String
        get() = listOfNotNull(city, state).joinToString(", ").ifEmpty { displayAddress }

    val coordinates: String
        get() = String.format("%.6f, %.6f", latitude, longitude)
}

enum class SOSStatus(val value: String, val displayName: String) {
    PENDING("pending", "Pending"),
    ACTIVE("active", "Active"),
    HELP_ON_WAY("help_on_way", "Help On Way"),
    RESPONDED("responded", "Responded"),
    RESOLVED("resolved", "Resolved"),
    CANCELLED("cancelled", "Cancelled"),
    FALSE_ALARM("false_alarm", "False Alarm");

    companion object {
        fun fromValue(value: String): SOSStatus {
            return entries.find { it.value == value } ?: PENDING
        }
    }
}

enum class AlertType(val value: String, val displayName: String) {
    EMERGENCY("emergency", "Emergency"),
    MEDICAL("medical", "Medical"),
    ACCIDENT("accident", "Accident"),
    FIRE("fire", "Fire"),
    NATURAL_DISASTER("natural_disaster", "Natural Disaster"),
    CRIME("crime", "Crime"),
    OTHER("other", "Other");

    companion object {
        fun fromValue(value: String): AlertType {
            return entries.find { it.value == value } ?: EMERGENCY
        }

        fun getAll(): List<AlertType> = entries.toList()
    }
}
