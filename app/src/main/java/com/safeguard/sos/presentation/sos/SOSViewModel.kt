package com.safeguard.sos.presentation.sos

import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.AlertType
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.repository.EmergencyContactRepository
import com.safeguard.sos.domain.repository.HelperRepository
import com.safeguard.sos.domain.repository.LocationRepository
import com.safeguard.sos.domain.repository.SOSRepository
import com.safeguard.sos.domain.usecase.helper.GetNearbyHelpersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SOSViewModel @Inject constructor(
    private val sosRepository: SOSRepository,
    private val locationRepository: LocationRepository,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val helperRepository: HelperRepository,
    private val getNearbyHelpersUseCase: GetNearbyHelpersUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SOSUiState())
    val uiState: StateFlow<SOSUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        loadEmergencyContacts()
        loadCurrentLocation()
        checkActiveAlerts()
    }

    private fun loadEmergencyContacts() {
        viewModelScope.launch {
            emergencyContactRepository.getContacts().collect { contacts ->
                _uiState.update {
                    it.copy(emergencyContactsCount = contacts.size)
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
                        loadNearbyHelpers(location)
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(locationError = result.message) }
                }
                else -> {}
            }
        }
    }

    private fun loadNearbyHelpers(location: Location) {
        viewModelScope.launch {
            getNearbyHelpersUseCase(
                latitude = location.latitude,
                longitude = location.longitude,
                radiusKm = 10.0
            ).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(nearbyHelpersCount = result.data.size)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun checkActiveAlerts() {
        viewModelScope.launch {
            sosRepository.getActiveSOSAlerts().collect { result ->
                if (result is Resource.Success) {
                    _uiState.update {
                        it.copy(hasActiveAlert = result.data.isNotEmpty())
                    }
                }
            }
        }
    }

    fun setEmergencyType(type: AlertType) {
        _uiState.update { it.copy(emergencyType = type) }
    }

    fun setSilentMode(enabled: Boolean) {
        _uiState.update { it.copy(isSilentMode = enabled) }
    }

    fun setContactsOnly(enabled: Boolean) {
        _uiState.update { it.copy(isContactsOnly = enabled) }
    }

    fun setAdditionalMessage(message: String) {
        _uiState.update { it.copy(additionalMessage = message) }
    }

    fun updatePermissionStatus(hasPermission: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = hasPermission) }
        if (hasPermission) {
            loadCurrentLocation()
        }
    }

    fun refreshLocation() {
        loadCurrentLocation()
    }

    fun initiateSOSTrigger() {
        val state = _uiState.value

        if (state.currentLocation == null) {
            showToast("Unable to get your location. Please try again.")
            return
        }

        if (state.hasActiveAlert) {
            showToast("You already have an active SOS alert.")
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        showToast("Preparing SOS...")
        // Navigate manually in fragment
    }

    fun sendTestSOS() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Placeholder
            _uiState.update { it.copy(isLoading = false) }
            showToast("Test SOS feature coming soon")
        }
    }
}
