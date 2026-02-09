package com.safeguard.sos.presentation.sos

import com.safeguard.sos.domain.model.AlertType
import com.safeguard.sos.domain.model.Location

data class SOSUiState(
    val isLoading: Boolean = false,
    val emergencyContactsCount: Int = 0,
    val nearbyHelpersCount: Int = 0,
    val currentLocation: Location? = null,
    val hasActiveAlert: Boolean = false,
    val isSilentMode: Boolean = false,
    val isContactsOnly: Boolean = false,
    val emergencyType: AlertType = AlertType.EMERGENCY,
    val additionalMessage: String = "",
    val hasLocationPermission: Boolean = false,
    val locationError: String? = null,
    val errorMessage: String? = null
)
