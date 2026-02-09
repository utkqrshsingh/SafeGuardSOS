package com.safeguard.sos.service.sos

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.EmergencyContact
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.model.SOSStatus
import com.safeguard.sos.domain.model.AlertType
import com.safeguard.sos.domain.model.User
import com.safeguard.sos.domain.repository.EmergencyContactRepository
import com.safeguard.sos.domain.repository.HelperRepository
import com.safeguard.sos.domain.repository.SOSRepository
import com.safeguard.sos.domain.repository.UserRepository
import com.safeguard.sos.service.sms.SMSService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SOSManager @Inject constructor(
    private val sosRepository: SOSRepository,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val helperRepository: HelperRepository,
    private val userRepository: UserRepository,
    private val smsService: SMSService
) {

    suspend fun createSOSAlert(
        location: Location,
        emergencyType: String,
        message: String,
        contactsOnly: Boolean
    ): Result<SOSAlert> = coroutineScope {
        try {
            // Get current user
            val user = userRepository.getCurrentUser().first()
                ?: return@coroutineScope Result.failure(Exception("User not found"))

            // Get emergency contacts
            val contacts = emergencyContactRepository.getContacts().first()

            // Create SOS alert
            val sosAlert = SOSAlert(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                userName = user.name,
                userPhone = user.phone,
                location = location,
                alertType = AlertType.valueOf(emergencyType),
                message = message,
                status = SOSStatus.PENDING,
                createdAt = System.currentTimeMillis(),
                respondersCount = 0
            )

            // Save to repository
            val saveResult = sosRepository.triggerSOS(location, emergencyType, message).first()
            if (saveResult is Resource.Error) {
                return@coroutineScope Result.failure(Exception(saveResult.message))
            }
            
            val actualAlert = (saveResult as? Resource.Success<SOSAlert>)?.data ?: sosAlert

            // Notify contacts and helpers concurrently
            val notifyContactsDeferred = async { notifyEmergencyContacts(actualAlert, contacts) }
            // Notification logic for helpers usually handled on backend, or if needed here:
            // val notifyHelpersDeferred = if (!contactsOnly) { ... }

            val smsSentCount = notifyContactsDeferred.await()

            // Update SOS with actual status if needed
            Result.success(actualAlert)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun notifyEmergencyContacts(
        sosAlert: SOSAlert,
        contacts: List<EmergencyContact>
    ): Int {
        var smsSentCount = 0

        val message = buildSOSMessage(sosAlert)

        contacts.forEach { contact ->
            try {
                val sent = smsService.sendSMS(contact.phoneNumber, message)
                if (sent) smsSentCount++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return smsSentCount
    }

    private fun buildSOSMessage(sosAlert: SOSAlert): String {
        val locationUrl = "https://maps.google.com/?q=${sosAlert.location.latitude},${sosAlert.location.longitude}"

        return buildString {
            append("🆘 EMERGENCY SOS ALERT!\n\n")
            append("${sosAlert.userName} needs help!\n")
            append("Type: ${sosAlert.alertType.name}\n")
            if (!sosAlert.message.isNullOrBlank()) {
                append("Message: ${sosAlert.message}\n")
            }
            append("\n📍 Location: $locationUrl\n")
            append("\nSent via SafeGuard SOS")
        }
    }

    suspend fun cancelSOSAlert(sosId: String): Result<Unit> {
        return try {
            sosRepository.cancelSOS(sosId).first()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveSOSAlert(sosId: String): Result<Unit> {
        return try {
            sosRepository.updateSOSStatus(sosId, SOSStatus.RESOLVED).first()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSOSLocation(sosId: String, location: Location): Result<Unit> {
        return try {
            sosRepository.updateSOSLocation(sosId, location).first()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
