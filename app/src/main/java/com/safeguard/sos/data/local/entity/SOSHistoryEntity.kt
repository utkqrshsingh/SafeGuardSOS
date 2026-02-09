// app/src/main/java/com/safeguard/sos/data/local/entity/SOSHistoryEntity.kt

package com.safeguard.sos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.safeguard.sos.domain.model.AlertType
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.model.SOSStatus

@Entity(
    tableName = "sos_history",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["status"]),
        Index(value = ["created_at"])
    ]
)
data class SOSHistoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "user_name")
    val userName: String,

    @ColumnInfo(name = "user_phone")
    val userPhone: String,

    @ColumnInfo(name = "user_photo_url")
    val userPhotoUrl: String?,

    @Embedded(prefix = "location_")
    val location: LocationEntity,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "alert_type")
    val alertType: String,

    @ColumnInfo(name = "message")
    val message: String?,

    @ColumnInfo(name = "audio_recording_url")
    val audioRecordingUrl: String?,

    @ColumnInfo(name = "video_recording_url")
    val videoRecordingUrl: String?,

    @ColumnInfo(name = "responders_count")
    val respondersCount: Int,

    @ColumnInfo(name = "active_responders_count")
    val activeRespondersCount: Int,

    @ColumnInfo(name = "notified_helpers_count")
    val notifiedHelpersCount: Int,

    @ColumnInfo(name = "resolved_by")
    val resolvedBy: String?,

    @ColumnInfo(name = "resolved_at")
    val resolvedAt: Long?,

    @ColumnInfo(name = "is_false_alarm")
    val isFalseAlarm: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

data class LocationEntity(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val altitude: Double?,
    val address: String?,
    val city: String?,
    val state: String?,
    val timestamp: Long
)

fun SOSHistoryEntity.toDomain(): SOSAlert {
    return SOSAlert(
        id = id,
        userId = userId,
        userName = userName,
        userPhone = userPhone,
        userPhotoUrl = userPhotoUrl,
        location = location.toDomain(),
        status = SOSStatus.fromValue(status),
        alertType = AlertType.fromValue(alertType),
        message = message,
        audioRecordingUrl = audioRecordingUrl,
        videoRecordingUrl = videoRecordingUrl,
        respondersCount = respondersCount,
        activeRespondersCount = activeRespondersCount,
        notifiedHelpersCount = notifiedHelpersCount,
        resolvedBy = resolvedBy,
        resolvedAt = resolvedAt,
        isFalseAlarm = isFalseAlarm,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun SOSAlert.toEntity(): SOSHistoryEntity {
    return SOSHistoryEntity(
        id = id,
        userId = userId,
        userName = userName,
        userPhone = userPhone,
        userPhotoUrl = userPhotoUrl,
        location = location.toEntity(),
        status = status.value,
        alertType = alertType.value,
        message = message,
        audioRecordingUrl = audioRecordingUrl,
        videoRecordingUrl = videoRecordingUrl,
        respondersCount = respondersCount,
        activeRespondersCount = activeRespondersCount,
        notifiedHelpersCount = notifiedHelpersCount,
        resolvedBy = resolvedBy,
        resolvedAt = resolvedAt,
        isFalseAlarm = isFalseAlarm,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun LocationEntity.toDomain(): Location {
    return Location(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        altitude = altitude,
        address = address,
        city = city,
        state = state,
        timestamp = timestamp
    )
}

fun Location.toEntity(): LocationEntity {
    return LocationEntity(
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        altitude = altitude,
        address = address,
        city = city,
        state = state,
        timestamp = timestamp
    )
}