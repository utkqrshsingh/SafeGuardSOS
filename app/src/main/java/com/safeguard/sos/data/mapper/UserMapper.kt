package com.safeguard.sos.data.mapper

import com.safeguard.sos.data.local.entity.UserEntity
import com.safeguard.sos.data.local.entity.toEntity
import com.safeguard.sos.data.local.entity.toDomain
import com.safeguard.sos.data.remote.dto.request.UpdateUserRequest
import com.safeguard.sos.data.remote.dto.response.UserResponse
import com.safeguard.sos.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserMapper @Inject constructor() {

    fun mapResponseToDomain(response: UserResponse): User {
        return User(
            id = response.id,
            fullName = response.fullName,
            phoneNumber = response.phoneNumber,
            email = response.email,
            aadhaarNumber = response.aadhaarNumber,
            profileImageUrl = response.profileImageUrl,
            isHelper = response.isHelper,
            verificationStatus = VerificationStatus.fromValue(response.verificationStatus),
            userType = UserType.fromValue(response.userType),
            helperStatus = response.helperStatus?.let { HelperStatus.fromValue(it) },
            address = response.address?.let {
                Address(
                    street = it.street,
                    city = it.city,
                    state = it.state,
                    pincode = it.pincode,
                    country = it.country ?: "India"
                )
            },
            city = response.city,
            state = response.state,
            pincode = response.pincode,
            bloodGroup = response.bloodGroup,
            dateOfBirth = response.dateOfBirth,
            gender = response.gender?.let { Gender.fromValue(it) },
            medicalInfo = response.medicalInfo?.let { info ->
                MedicalInfo(
                    allergies = info.allergies ?: emptyList(),
                    medications = info.medications ?: emptyList(),
                    conditions = info.conditions ?: emptyList(),
                    emergencyNotes = info.emergencyNotes ?: "",
                    organDonor = info.organDonor ?: false
                )
            },
            fcmToken = response.fcmToken,
            createdAt = response.createdAt.toLongOrNull() ?: System.currentTimeMillis(),
            updatedAt = response.updatedAt.toLongOrNull() ?: System.currentTimeMillis()
        )
    }

    fun mapEntityToDomain(entity: UserEntity): User = entity.toDomain()

    fun mapDomainToEntity(user: User): UserEntity = user.toEntity()

    fun mapDomainToRequest(user: User): UpdateUserRequest {
        return UpdateUserRequest(
            name = user.fullName,
            email = user.email,
            address = user.address?.street,
            city = user.city,
            state = user.state,
            pincode = user.pincode,
            bloodGroup = user.bloodGroup,
            dateOfBirth = user.dateOfBirth,
            gender = user.gender?.value,
            profileImageUrl = user.profileImageUrl
        )
    }
}
