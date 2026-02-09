// app/src/main/java/com/safeguard/sos/domain/model/Helper.kt

package com.safeguard.sos.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Helper(
    val id: String,
    val userId: String,
    val name: String,
    val phoneNumber: String,
    val photoUrl: String?,
    val location: Location?,
    val status: HelperStatus,
    val verificationStatus: VerificationStatus,
    val rating: Float,
    val totalResponses: Int,
    val successfulResponses: Int,
    val averageResponseTime: Int, // in minutes
    val radiusKm: Int,
    val skills: List<String>?,
    val isAvailable: Boolean,
    val lastActiveAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
) : Parcelable {

    val isActive: Boolean
        get() = status == HelperStatus.ACTIVE && isAvailable

    val isVerified: Boolean
        get() = verificationStatus == VerificationStatus.VERIFIED

    val successRate: Float
        get() = if (totalResponses > 0) {
            (successfulResponses.toFloat() / totalResponses) * 100
        } else 0f

    val displayRating: String
        get() = String.format("%.1f", rating)

    val initials: String
        get() = name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")

    // Added for compatibility with HelperDashboardViewModel
    val peopleHelped: Int get() = successfulResponses
    val averageResponseTimeMinutes: Int get() = averageResponseTime
}

@Parcelize
data class HelperResponse(
    val id: String,
    val sosAlertId: String,
    val helperId: String,
    val helperName: String,
    val helperPhone: String,
    val helperPhotoUrl: String?,
    val status: ResponseStatus,
    val location: Location?,
    val distanceKm: Float?,
    val estimatedArrivalMinutes: Int?,
    val respondedAt: Long,
    val arrivedAt: Long?,
    val completedAt: Long?,
    val notes: String?
) : Parcelable {

    val isActive: Boolean
        get() = status == ResponseStatus.RESPONDING || status == ResponseStatus.ARRIVED

    val responseTimeMinutes: Long
        get() = (arrivedAt ?: System.currentTimeMillis() - respondedAt) / 60000

    // Added for compatibility with HelperDashboardViewModel
    val alertId: String get() = sosAlertId
    val userName: String get() = "" // Should come from SOS alert but mapping here for now
    val emergencyType: String get() = "" // Should come from SOS alert
}

enum class ResponseStatus(val value: String, val displayName: String) {
    PENDING("pending", "Pending"),
    RESPONDING("responding", "On the way"),
    ARRIVED("arrived", "Arrived"),
    COMPLETED("completed", "Completed"),
    CANCELLED("cancelled", "Cancelled");

    companion object {
        fun fromValue(value: String): ResponseStatus {
            return entries.find { it.value == value } ?: PENDING
        }
    }
}

@Parcelize
data class NearbyHelper(
    val helper: Helper,
    val distanceKm: Float,
    val estimatedArrivalMinutes: Int
) : Parcelable
