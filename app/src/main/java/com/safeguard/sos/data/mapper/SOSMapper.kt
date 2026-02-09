package com.safeguard.sos.data.mapper

import com.google.firebase.firestore.DocumentSnapshot
import com.safeguard.sos.data.local.entity.LocationEntity
import com.safeguard.sos.data.local.entity.SOSHistoryEntity
import com.safeguard.sos.data.remote.dto.response.SOSAlertResponse
import com.safeguard.sos.domain.model.AlertType
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.model.SOSStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SOSMapper @Inject constructor() {

    fun mapResponseToDomain(response: SOSAlertResponse): SOSAlert {
        return SOSAlert(
            id = response.id,
            userId = response.userId,
            userName = response.userName ?: "Unknown",
            userPhone = response.userPhone ?: "",
            userPhotoUrl = null, // Not in response
            location = Location(
                latitude = response.latitude,
                longitude = response.longitude,
                address = response.address,
                timestamp = response.createdAt ?: System.currentTimeMillis()
            ),
            status = SOSStatus.valueOf(response.status ?: SOSStatus.ACTIVE.name),
            alertType = AlertType.fromValue(response.emergencyType ?: "emergency"),
            message = response.message,
            createdAt = response.createdAt ?: System.currentTimeMillis(),
            resolvedAt = response.resolvedAt,
            updatedAt = System.currentTimeMillis() // Fallback
        )
    }

    fun mapEntityToDomain(entity: SOSHistoryEntity): SOSAlert {
        return SOSAlert(
            id = entity.id,
            userId = entity.userId,
            userName = entity.userName,
            userPhone = entity.userPhone,
            userPhotoUrl = entity.userPhotoUrl,
            location = entity.location.toDomain(),
            status = SOSStatus.valueOf(entity.status),
            alertType = AlertType.fromValue(entity.alertType),
            message = entity.message,
            audioRecordingUrl = entity.audioRecordingUrl,
            videoRecordingUrl = entity.videoRecordingUrl,
            respondersCount = entity.respondersCount,
            activeRespondersCount = entity.activeRespondersCount,
            notifiedHelpersCount = entity.notifiedHelpersCount,
            resolvedBy = entity.resolvedBy,
            resolvedAt = entity.resolvedAt,
            isFalseAlarm = entity.isFalseAlarm,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun mapDomainToEntity(sosAlert: SOSAlert): SOSHistoryEntity {
        return SOSHistoryEntity(
            id = sosAlert.id,
            userId = sosAlert.userId,
            userName = sosAlert.userName,
            userPhone = sosAlert.userPhone,
            userPhotoUrl = sosAlert.userPhotoUrl,
            location = sosAlert.location.toEntity(),
            status = sosAlert.status.name,
            alertType = sosAlert.alertType.value,
            message = sosAlert.message,
            audioRecordingUrl = sosAlert.audioRecordingUrl,
            videoRecordingUrl = sosAlert.videoRecordingUrl,
            respondersCount = sosAlert.respondersCount,
            activeRespondersCount = sosAlert.activeRespondersCount,
            notifiedHelpersCount = sosAlert.notifiedHelpersCount,
            resolvedBy = sosAlert.resolvedBy,
            resolvedAt = sosAlert.resolvedAt,
            isFalseAlarm = sosAlert.isFalseAlarm,
            createdAt = sosAlert.createdAt,
            updatedAt = sosAlert.updatedAt
        )
    }

    fun mapDomainToFirestore(sosAlert: SOSAlert): Map<String, Any?> {
        return mapOf(
            "userId" to sosAlert.userId,
            "userName" to sosAlert.userName,
            "userPhone" to sosAlert.userPhone,
            "userPhotoUrl" to sosAlert.userPhotoUrl,
            "latitude" to sosAlert.location.latitude,
            "longitude" to sosAlert.location.longitude,
            "address" to sosAlert.location.address,
            "city" to sosAlert.location.city,
            "state" to sosAlert.location.state,
            "status" to sosAlert.status.name,
            "alertType" to sosAlert.alertType.value,
            "message" to sosAlert.message,
            "audioRecordingUrl" to sosAlert.audioRecordingUrl,
            "videoRecordingUrl" to sosAlert.videoRecordingUrl,
            "respondersCount" to sosAlert.respondersCount,
            "activeRespondersCount" to sosAlert.activeRespondersCount,
            "notifiedHelpersCount" to sosAlert.notifiedHelpersCount,
            "resolvedBy" to sosAlert.resolvedBy,
            "resolvedAt" to sosAlert.resolvedAt,
            "isFalseAlarm" to sosAlert.isFalseAlarm,
            "createdAt" to sosAlert.createdAt,
            "updatedAt" to sosAlert.updatedAt
        )
    }

    fun mapFirestoreToDomain(doc: DocumentSnapshot): SOSAlert {
        val lat = doc.getDouble("latitude") ?: 0.0
        val lon = doc.getDouble("longitude") ?: 0.0
        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
        
        return SOSAlert(
            id = doc.id,
            userId = doc.getString("userId") ?: "",
            userName = doc.getString("userName") ?: "Unknown",
            userPhone = doc.getString("userPhone") ?: "",
            userPhotoUrl = doc.getString("userPhotoUrl"),
            location = Location(
                latitude = lat,
                longitude = lon,
                accuracy = doc.getDouble("accuracy")?.toFloat(),
                address = doc.getString("address"),
                city = doc.getString("city"),
                state = doc.getString("state"),
                timestamp = createdAt
            ),
            status = SOSStatus.valueOf(doc.getString("status") ?: SOSStatus.ACTIVE.name),
            alertType = AlertType.fromValue(doc.getString("alertType") ?: "emergency"),
            message = doc.getString("message"),
            audioRecordingUrl = doc.getString("audioRecordingUrl"),
            videoRecordingUrl = doc.getString("videoRecordingUrl"),
            respondersCount = doc.getLong("respondersCount")?.toInt() ?: 0,
            activeRespondersCount = doc.getLong("activeRespondersCount")?.toInt() ?: 0,
            notifiedHelpersCount = doc.getLong("notifiedHelpersCount")?.toInt() ?: 0,
            resolvedBy = doc.getString("resolvedBy"),
            resolvedAt = doc.getLong("resolvedAt"),
            isFalseAlarm = doc.getBoolean("isFalseAlarm") ?: false,
            createdAt = createdAt,
            updatedAt = doc.getLong("updatedAt") ?: createdAt
        )
    }
}