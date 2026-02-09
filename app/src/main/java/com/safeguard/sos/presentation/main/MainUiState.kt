package com.safeguard.sos.presentation.main

import com.safeguard.sos.domain.model.User
import com.safeguard.sos.domain.model.UserType

data class MainUiState(
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val isSOSActive: Boolean = false,
    val isTriggering: Boolean = false,
    val isLoggingOut: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val user: User? = null,
    val userType: UserType? = null,
    val isVerified: Boolean = false,
    val activeSOSCount: Int = 0,
    val nearbyAlertsCount: Int = 0,
    val errorMessage: String? = null
) {
    val userName: String
        get() = user?.fullName ?: "User"

    val userInitials: String
        get() = user?.fullName?.split(" ")
            ?.take(2)
            ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
            ?.joinToString("")
            ?: "U"

    val isHelper: Boolean
        get() = userType == UserType.HELPER || userType == UserType.BOTH

    val canReceiveAlerts: Boolean
        get() = isHelper && isVerified && hasLocationPermission
}
