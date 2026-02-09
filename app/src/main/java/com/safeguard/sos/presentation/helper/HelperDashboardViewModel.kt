package com.safeguard.sos.presentation.helper

import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.HelperStatus
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.repository.HelperRepository
import com.safeguard.sos.domain.repository.LocationRepository
import com.safeguard.sos.domain.usecase.helper.GetNearbySOSAlertsUseCase
import com.safeguard.sos.domain.usecase.helper.RespondToSOSUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HelperDashboardViewModel @Inject constructor(
    private val helperRepository: HelperRepository,
    private val locationRepository: LocationRepository,
    private val getNearbySOSAlertsUseCase: GetNearbySOSAlertsUseCase,
    private val respondToSOSUseCase: RespondToSOSUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(HelperDashboardUiState())
    val uiState: StateFlow<HelperDashboardUiState> = _uiState.asStateFlow()

    init {
        checkHelperRegistration()
        loadCurrentLocation()
    }

    fun checkHelperRegistration() {
        viewModelScope.launch {
            helperRepository.isRegisteredHelper().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update { it.copy(isRegisteredHelper = result.data) }
                        if (result.data) {
                            loadHelperData()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadHelperData() {
        viewModelScope.launch {
            helperRepository.getHelperProfile().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data.let { helper ->
                            _uiState.update {
                                it.copy(
                                    isRegisteredHelper = true,
                                    isVerified = helper.isVerified,
                                    isHelperModeActive = helper.status == HelperStatus.AVAILABLE,
                                    availabilityStatus = helper.status,
                                    peopleHelpedCount = helper.peopleHelped,
                                    averageResponseTime = formatResponseTime(helper.averageResponseTimeMinutes),
                                    rating = helper.rating ?: 0f,
                                    totalResponses = helper.totalResponses
                                )
                            }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isRegisteredHelper = false) }
                    }
                    else -> {}
                }
            }
        }

        // Load active response
        viewModelScope.launch {
            helperRepository.getActiveResponse().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { response ->
                            _uiState.update {
                                it.copy(
                                    hasActiveResponse = true,
                                    activeResponseAlertId = response.alertId,
                                    activeResponseUserName = response.userName,
                                    activeResponseType = response.emergencyType,
                                    activeResponseTime = formatTimeAgo(response.respondedAt)
                                )
                            }
                        } ?: run {
                            _uiState.update { it.copy(hasActiveResponse = false) }
                        }
                    }
                    else -> {
                        _uiState.update { it.copy(hasActiveResponse = false) }
                    }
                }
            }
        }
    }

    private fun loadCurrentLocation() {
        viewModelScope.launch {
            val result = locationRepository.getCurrentLocation()
            when (result) {
                is Resource.Success -> {
                    result.data.let { location ->
                        _uiState.update { it.copy(currentLocation = location) }
                        loadNearbyAlerts(location)
                    }
                }
                else -> {}
            }
        }
    }

    private fun loadNearbyAlerts(location: Location) {
        viewModelScope.launch {
            getNearbySOSAlertsUseCase(
                latitude = location.latitude,
                longitude = location.longitude,
                radiusKm = 10.0
            ).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                nearbyAlerts = result.data,
                                isLoading = false
                            )
                        }
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is Resource.Empty -> {
                        _uiState.update { it.copy(isLoading = false, nearbyAlerts = emptyList()) }
                    }
                }
            }
        }
    }

    fun refreshData() {
        _uiState.update { it.copy(isLoading = true) }
        loadHelperData()
        loadCurrentLocation()
    }

    fun toggleHelperMode(enabled: Boolean) {
        viewModelScope.launch {
            val status = if (enabled) HelperStatus.AVAILABLE else HelperStatus.OFFLINE

            helperRepository.updateHelperStatus(status).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isHelperModeActive = result.data == HelperStatus.AVAILABLE,
                                availabilityStatus = result.data
                            )
                        }

                        if (enabled) {
                            _uiState.value.currentLocation?.let { loadNearbyAlerts(it) }
                        } else {
                            _uiState.update { it.copy(nearbyAlerts = emptyList()) }
                        }

                        showToast("Status updated")
                    }
                    is Resource.Error -> {
                        showToast(result.message ?: "Failed to update status")
                    }
                    else -> {}
                }
            }
        }
    }

    fun setAvailabilityStatus(status: HelperStatus) {
        viewModelScope.launch {
            helperRepository.updateHelperStatus(status).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                availabilityStatus = result.data,
                                isHelperModeActive = result.data == HelperStatus.AVAILABLE
                            )
                        }
                        showToast("Status updated")
                    }
                    is Resource.Error -> {
                        showToast(result.message ?: "Failed to update status")
                    }
                    else -> {}
                }
            }
        }
    }

    fun respondToAlert(alertId: String) {
        viewModelScope.launch {
            respondToSOSUseCase(alertId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        showToast("You are now responding to this alert")
                        loadHelperData()
                    }
                    is Resource.Error -> {
                        showToast(result.message ?: "Failed to respond")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun formatResponseTime(minutes: Int?): String {
        return when {
            minutes == null -> "--"
            minutes < 1 -> "< 1 min"
            minutes < 60 -> "$minutes min"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            else -> "${diff / 86400_000}d ago"
        }
    }
}

data class HelperDashboardUiState(
    val isLoading: Boolean = false,
    val isRegisteredHelper: Boolean = false,
    val isVerified: Boolean = false,
    val isHelperModeActive: Boolean = false,
    val availabilityStatus: HelperStatus = HelperStatus.OFFLINE,
    val currentLocation: Location? = null,

    // Stats
    val peopleHelpedCount: Int = 0,
    val averageResponseTime: String = "--",
    val rating: Float = 0f,
    val totalResponses: Int = 0,

    // Active Response
    val hasActiveResponse: Boolean = false,
    val activeResponseAlertId: String? = null,
    val activeResponseUserName: String? = null,
    val activeResponseType: String? = null,
    val activeResponseTime: String? = null,

    // Nearby Alerts
    val nearbyAlerts: List<SOSAlert> = emptyList(),

    val errorMessage: String? = null
)
