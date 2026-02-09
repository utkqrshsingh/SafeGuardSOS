// app/src/main/java/com/safeguard/sos/data/local/entity/EmergencyContactEntity.kt

package com.safeguard.sos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.safeguard.sos.domain.model.EmergencyContact
import com.safeguard.sos.domain.model.Relationship

@Entity(
    tableName = "emergency_contacts",
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["phone_number"])
    ]
)
data class EmergencyContactEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "relationship")
    val relationship: String,

    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = false,

    @ColumnInfo(name = "notify_via_sms")
    val notifyViaSms: Boolean = true,

    @ColumnInfo(name = "notify_via_call")
    val notifyViaCall: Boolean = false,

    @ColumnInfo(name = "photo_uri")
    val photoUri: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

fun EmergencyContactEntity.toDomain(): EmergencyContact {
    return EmergencyContact(
        id = id,
        userId = userId,
        name = name,
        phoneNumber = phoneNumber,
        relationship = Relationship.fromValue(relationship),
        isPrimary = isPrimary,
        notifyViaSms = notifyViaSms,
        notifyViaCall = notifyViaCall,
        photoUri = photoUri,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

fun EmergencyContact.toEntity(): EmergencyContactEntity {
    return EmergencyContactEntity(
        id = id,
        userId = userId,
        name = name,
        phoneNumber = phoneNumber,
        relationship = relationship.value,
        isPrimary = isPrimary,
        notifyViaSms = notifyViaSms,
        notifyViaCall = notifyViaCall,
        photoUri = photoUri,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}