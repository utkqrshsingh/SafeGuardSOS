// app/src/main/java/com/safeguard/sos/domain/usecase/sos/TriggerSOSUseCase.kt

package com.safeguard.sos.domain.usecase.sos

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.AlertType
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.repository.EmergencyContactRepository
import com.safeguard.sos.domain.repository.LocationRepository
import com.safeguard.sos.domain.repository.SOSRepository
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

class TriggerSOSUseCase @Inject constructor(
    private val sosRepository: SOSRepository,
    private val locationRepository: LocationRepository,
    private val contactRepository: EmergencyContactRepository
) {
    suspend operator fun invoke(
        alertType: AlertType = AlertType.EMERGENCY,
        message: String? = null
    ): Resource<SOSAlert> {
        // Check if there's already an active SOS
        val activeSOS = sosRepository.getActiveSOSAlertSync()
        if (activeSOS != null) {
            return Resource.Error("You already have an active SOS alert")
        }

        // Get current location
        val locationResult = locationRepository.getCurrentLocation()
        val location = when (locationResult) {
            is Resource.Success -> locationResult.data
            is Resource.Error -> {
                Timber.w("Could not get precise location, using last known")
                // Try to use last known location as fallback
                locationRepository.getLastKnownLocation()
                    ?: return Resource.Error("Could not determine your location. Please enable GPS.")
            }
            else -> return Resource.Error("Could not determine your location")
        }

        // Trigger SOS - handle Flow from repository
        val sosResult = sosRepository.triggerSOS(location, alertType.name, message)
            .filter { it !is Resource.Loading }
            .first()

        when (sosResult) {
            is Resource.Success -> {
                // Notify emergency contacts in background
                try {
                    sosRepository.notifyEmergencyContacts(sosResult.data.id)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to notify emergency contacts")
                    // Don't fail the SOS trigger if contact notification fails
                }
            }
            is Resource.Error -> {
                Timber.e("Failed to trigger SOS: ${sosResult.message}")
            }
            else -> {}
        }

        return sosResult
    }
}
