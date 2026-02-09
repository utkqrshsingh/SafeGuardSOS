// app/src/main/java/com/safeguard/sos/data/local/entity/HelperEntity.kt

package com.safeguard.sos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.safeguard.sos.domain.model.Helper
import com.safeguard.sos.domain.model.HelperStatus
import com.safeguard.sos.domain.model.VerificationStatus

@Entity(
    tableName = "helpers",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["status"]),
        Index(value = ["is_available"])
    ]
)
data class HelperEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "photo_url")
    val photoUrl: String?,

    @Embedded(prefix = "location_")
    val location: LocationEntity?,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "verification_status")
    val verificationStatus: String,

    @ColumnInfo(name = "rating")
    val rating: Float,

    @ColumnInfo(name = "total_responses")
    val totalResponses: Int,

    @ColumnInfo(name = "successful_responses")
    val successfulResponses: Int,

    @ColumnInfo(name = "average_response_time")
    val averageResponseTime: Int,

    @ColumnInfo(name = "radius_km")
    val radiusKm: Int,

    @ColumnInfo(name = "skills")
    val skills: String?, // Comma-separated

    @ColumnInfo(name = "is_available")
    val isAvailable: Boolean,

    @ColumnInfo(name = "last_active_at")
    val lastActiveAt: Long?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

fun HelperEntity.toDomain(): Helper {
    return Helper(
        id = id,
        userId = userId,
        name = name,
        phoneNumber = phoneNumber,
        photoUrl = photoUrl,
        location = location?.toDomain(),
        status = HelperStatus.fromValue(status),
        verificationStatus = VerificationStatus.fromValue(verificationStatus),
        rating = rating,
        totalResponses = totalResponses,
        successfulResponses = successfulResponses,
        averageResponseTime = averageResponseTime,
        radiusKm = radiusKm,
        skills = skills?.split(",")?.filter { it.isNotBlank() },
        isAvailable = isAvailable,
        lastActiveAt = lastActiveAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Helper.toEntity(): HelperEntity {
    return HelperEntity(
        id = id,
        userId = userId,
        name = name,
        phoneNumber = phoneNumber,
        photoUrl = photoUrl,
        location = location?.toEntity(),
        status = status.value,
        verificationStatus = verificationStatus.value,
        rating = rating,
        totalResponses = totalResponses,
        successfulResponses = successfulResponses,
        averageResponseTime = averageResponseTime,
        radiusKm = radiusKm,
        skills = skills?.joinToString(","),
        isAvailable = isAvailable,
        lastActiveAt = lastActiveAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}