// app/src/main/java/com/safeguard/sos/data/mapper/HelperMapper.kt

package com.safeguard.sos.data.mapper

import com.safeguard.sos.data.local.entity.HelperEntity
import com.safeguard.sos.data.local.entity.toDomain
import com.safeguard.sos.data.local.entity.toEntity
import com.safeguard.sos.data.remote.dto.response.NearbyHelperResponse
import com.safeguard.sos.domain.model.Helper
import com.safeguard.sos.domain.model.HelperStatus
import com.safeguard.sos.domain.model.NearbyHelper
import com.safeguard.sos.domain.model.VerificationStatus

object HelperMapper {

    // Entity to Domain
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

    // Domain to Entity
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

    // Response to Domain
    fun NearbyHelperResponse.toDomain(): NearbyHelper {
        val helper = Helper(
            id = id,
            userId = userId,
            name = name,
            phoneNumber = phoneNumber,
            photoUrl = photoUrl,
            location = null,
            status = HelperStatus.ACTIVE,
            verificationStatus = VerificationStatus.VERIFIED,
            rating = rating,
            totalResponses = totalResponses,
            successfulResponses = 0,
            averageResponseTime = estimatedArrivalMinutes,
            radiusKm = 10,
            skills = null,
            isAvailable = true,
            lastActiveAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return NearbyHelper(
            helper = helper,
            distanceKm = distanceKm,
            estimatedArrivalMinutes = estimatedArrivalMinutes
        )
    }

    // List conversions
    fun List<HelperEntity>.toDomainList(): List<Helper> {
        return map { it.toDomain() }
    }

    fun List<NearbyHelperResponse>.toNearbyHelperList(): List<NearbyHelper> {
        return map { it.toDomain() }
    }
}
