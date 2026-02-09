package com.safeguard.sos.presentation.home

import com.safeguard.sos.domain.model.Helper
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.model.SOSStatus
import com.safeguard.sos.domain.model.SafetyTip

data class HomeUiState(
    val isLoading: Boolean = false,
    val userName: String = "User",
    val userInitials: String = "U",
    val isVerified: Boolean = false,
    val isHelper: Boolean = false,
    val isHelperModeActive: Boolean = false,
    val hasLocationPermission: Boolean = true,

    // Active Alert
    val hasActiveAlert: Boolean = false,
    val activeSOSId: String? = null,
    val activeSOSStatus: SOSStatus? = null,
    val activeSOSTime: String? = null,
    val respondingHelpersCount: Int = 0,

    // Stats
    val totalAlerts: Int = 0,
    val nearbyHelpersCount: Int = 0,
    val peopleHelped: Int = 0,

    // Lists
    val recentAlerts: List<SOSAlert> = emptyList(),
    val nearbyHelpers: List<Helper> = emptyList(),

    // Safety Tip
    val currentSafetyTip: SafetyTip? = null,

    val errorMessage: String? = null
)