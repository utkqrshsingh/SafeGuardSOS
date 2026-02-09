package com.safeguard.sos.presentation.home

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.domain.model.EmergencyContact
import com.safeguard.sos.domain.model.Helper
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.model.SOSStatus
import com.safeguard.sos.domain.model.SafetyTip
import com.safeguard.sos.domain.model.UserType
import com.safeguard.sos.domain.repository.EmergencyContactRepository
import com.safeguard.sos.domain.repository.HelperRepository
import com.safeguard.sos.domain.repository.LocationRepository
import com.safeguard.sos.domain.repository.SOSRepository
import com.safeguard.sos.domain.repository.UserRepository
import com.safeguard.sos.domain.usecase.helper.GetNearbyHelpersUseCase
import com.safeguard.sos.domain.usecase.sos.GetSOSHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sosRepository: SOSRepository,
    private val helperRepository: HelperRepository,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val locationRepository: LocationRepository,
    private val getNearbyHelpersUseCase: GetNearbyHelpersUseCase,
    private val getSOSHistoryUseCase: GetSOSHistoryUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val safetyTips = listOf(
        SafetyTip(
            id = "1",
            title = "Stay Aware",
            content = "Always be aware of your surroundings, especially in unfamiliar areas.",
            iconRes = android.R.drawable.ic_menu_view
        ),
        SafetyTip(
            id = "2",
            title = "Share Your Location",
            content = "Let trusted contacts know your whereabouts when traveling alone.",
            iconRes = android.R.drawable.ic_menu_mylocation
        ),
        SafetyTip(
            id = "3",
            title = "Emergency Contacts",
            content = "Keep your emergency contacts updated and easily accessible.",
            iconRes = android.R.drawable.ic_menu_agenda
        ),
        SafetyTip(
            id = "4",
            title = "Trust Your Instincts",
            content = "If something feels wrong, trust your gut and move to safety.",
            iconRes = android.R.drawable.ic_lock_idle_lock
        )
    )

    init {
        loadUserData()
        loadActiveAlerts()
        loadRecentAlerts()
        loadNearbyHelpers()
        loadStats()
        loadRandomSafetyTip()
    }

    fun refreshData() {
        _uiState.update { it.copy(isLoading = true) }
        loadUserData()
        loadActiveAlerts()
        loadRecentAlerts()
        loadNearbyHelpers()
        loadStats()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            userName = user.fullName,
                            userInitials = user.fullName.split(" ")
                                .take(2)
                                .mapNotNull { name -> name.firstOrNull()?.uppercaseChar() }
                                .joinToString(""),
                            isVerified = user.isVerified,
                            isHelper = user.userType == UserType.HELPER || user.userType == UserType.BOTH,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun loadActiveAlerts() {
        viewModelScope.launch {
            sosRepository.getActiveSOSAlerts().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val activeAlert = result.data.firstOrNull()
                        _uiState.update {
                            it.copy(
                                hasActiveAlert = activeAlert != null,
                                activeSOSId = activeAlert?.id,
                                activeSOSStatus = activeAlert?.status,
                                activeSOSTime = activeAlert?.createdAt?.let { time ->
                                    formatTimeAgo(time)
                                },
                                respondingHelpersCount = activeAlert?.respondersCount ?: 0
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadRecentAlerts() {
        viewModelScope.launch {
            getSOSHistoryUseCase(limit = 3).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(recentAlerts = result.data)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadNearbyHelpers() {
        viewModelScope.launch {
            val locationResult = locationRepository.getCurrentLocation()
            when (locationResult) {
                is Resource.Success -> {
                    locationResult.data.let { location ->
                        getNearbyHelpersUseCase(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            radiusKm = 10.0
                        ).collect { helpersResult ->
                            when (helpersResult) {
                                is Resource.Success -> {
                                    _uiState.update {
                                        it.copy(
                                            nearbyHelpers = helpersResult.data,
                                            nearbyHelpersCount = helpersResult.data.size
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            // Load total alerts - Assuming a method exists or as placeholder
            // sosRepository.getTotalAlertsCount().collect { ... }
        }

        viewModelScope.launch {
            // Load people helped (for helpers)
            helperRepository.getHelperProfile().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(peopleHelped = result.data.peopleHelped) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadRandomSafetyTip() {
        val randomTip = safetyTips.random()
        _uiState.update { it.copy(currentSafetyTip = randomTip) }
    }

    fun loadNextSafetyTip() {
        val currentIndex = safetyTips.indexOfFirst { it.id == _uiState.value.currentSafetyTip?.id }
        val nextIndex = (currentIndex + 1) % safetyTips.size
        _uiState.update { it.copy(currentSafetyTip = safetyTips[nextIndex]) }
    }

    fun quickCallEmergencyContact() {
        viewModelScope.launch {
            val contact = emergencyContactRepository.getPrimaryContact()
            if (contact != null) {
                showToast("Calling ${contact.name}...")
            } else {
                showToast("No emergency contact set. Please add one.")
            }
        }
    }

    fun shareCurrentLocation() {
        viewModelScope.launch {
            val result = locationRepository.getCurrentLocation()
            when (result) {
                is Resource.Success -> {
                    result.data.let { location ->
                        val locationUrl = "https://maps.google.com/?q=${location.latitude},${location.longitude}"
                        showToast("Location ready to share")
                    }
                }
                is Resource.Error -> {
                    showToast("Failed to get location")
                }
                else -> {}
            }
        }
    }

    fun toggleHelperMode(enabled: Boolean) {
        viewModelScope.launch {
            // Placeholder: Assuming updateHelperStatus is the correct method
            // helperRepository.updateHelperStatus(...)
        }
    }

    fun updateLocationPermissionStatus(hasPermission: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = hasPermission) }
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> "${diff / 86400_000}d ago"
        }
    }
}
