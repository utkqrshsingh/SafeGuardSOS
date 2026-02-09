// app/src/main/java/com/safeguard/sos/data/local/entity/UserEntity.kt

package com.safeguard.sos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.safeguard.sos.domain.model.*

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "email")
    val email: String?,

    @ColumnInfo(name = "profile_image_url")
    val profileImageUrl: String?,

    @ColumnInfo(name = "aadhaar_number")
    val aadhaarNumber: String?,

    @ColumnInfo(name = "user_type")
    val userType: String,

    @ColumnInfo(name = "verification_status")
    val verificationStatus: String,

    @ColumnInfo(name = "is_helper")
    val isHelper: Boolean,

    @ColumnInfo(name = "helper_status")
    val helperStatus: String?,

    @ColumnInfo(name = "date_of_birth")
    val dateOfBirth: String?,

    @ColumnInfo(name = "gender")
    val gender: String?,

    @ColumnInfo(name = "blood_group")
    val bloodGroup: String?,

    @Embedded(prefix = "address_")
    val address: AddressEntity?,

    @Embedded(prefix = "medical_")
    val medicalInfo: MedicalInfoEntity?,

    @ColumnInfo(name = "fcm_token")
    val fcmToken: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "last_active_at")
    val lastActiveAt: Long?,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)

data class AddressEntity(
    val street: String?,
    val city: String?,
    val state: String?,
    val pincode: String?,
    val country: String = "India"
)

data class MedicalInfoEntity(
    val conditions: String?, // JSON array string
    val allergies: String?, // JSON array string
    val medications: String?, // JSON array string
    @ColumnInfo(name = "emergency_notes")
    val emergencyNotes: String?,
    @ColumnInfo(name = "organ_donor")
    val organDonor: Boolean = false
)

// Extension functions to convert between Entity and Domain model
fun UserEntity.toDomain(): User {
    return User(
        id = id,
        fullName = fullName,
        phoneNumber = phoneNumber,
        email = email,
        profileImageUrl = profileImageUrl,
        aadhaarNumber = aadhaarNumber,
        userType = UserType.fromValue(userType),
        verificationStatus = VerificationStatus.fromValue(verificationStatus),
        isHelper = isHelper,
        helperStatus = helperStatus?.let { HelperStatus.fromValue(it) },
        dateOfBirth = dateOfBirth,
        gender = gender?.let { Gender.fromValue(it) },
        bloodGroup = bloodGroup,
        address = address?.toDomain(),
        medicalInfo = medicalInfo?.toDomain(),
        fcmToken = fcmToken,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastActiveAt = lastActiveAt,
        isActive = isActive
    )
}

fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        fullName = fullName,
        phoneNumber = phoneNumber,
        email = email,
        profileImageUrl = profileImageUrl,
        aadhaarNumber = aadhaarNumber,
        userType = userType.value,
        verificationStatus = verificationStatus.value,
        isHelper = isHelper,
        helperStatus = helperStatus?.value,
        dateOfBirth = dateOfBirth,
        gender = gender?.value,
        bloodGroup = bloodGroup,
        address = address?.toEntity(),
        medicalInfo = medicalInfo?.toEntity(),
        fcmToken = fcmToken,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastActiveAt = lastActiveAt,
        isActive = isActive
    )
}

fun AddressEntity.toDomain(): Address {
    return Address(
        street = street,
        city = city,
        state = state,
        pincode = pincode,
        country = country
    )
}

fun Address.toEntity(): AddressEntity {
    return AddressEntity(
        street = street,
        city = city,
        state = state,
        pincode = pincode,
        country = country
    )
}

fun MedicalInfoEntity.toDomain(): MedicalInfo {
    return MedicalInfo(
        conditions = conditions?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        allergies = allergies?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        medications = medications?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        emergencyNotes = emergencyNotes ?: "",
        organDonor = organDonor
    )
}

fun MedicalInfo.toEntity(): MedicalInfoEntity {
    return MedicalInfoEntity(
        conditions = conditions.joinToString(","),
        allergies = allergies.joinToString(","),
        medications = medications.joinToString(","),
        emergencyNotes = emergencyNotes,
        organDonor = organDonor
    )
}
