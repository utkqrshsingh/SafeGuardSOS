package com.safeguard.sos.presentation.helper

import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.core.utils.DistanceCalculator
import com.safeguard.sos.domain.model.HelperResponse
import com.safeguard.sos.domain.model.HelperStatus
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.model.ResponseStatus
import com.safeguard.sos.domain.repository.HelperRepository
import com.safeguard.sos.domain.repository.LocationRepository
import com.safeguard.sos.domain.repository.SOSRepository
import com.safeguard.sos.service.location.LocationTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor(
    private val sosRepository: SOSRepository,
    private val helperRepository: HelperRepository,
    private val locationRepository: LocationRepository,
    private val locationTracker: LocationTracker
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    private var alertId: String? = null
    private var responseId: String? = null
    private var locationUpdateJob: Job? = null

    fun initialize(alertId: String, latitude: Double, longitude: Double) {
        this.alertId = alertId

        _uiState.update {
            it.copy(
                destinationLocation = Location(
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = null,
                    timestamp = System.currentTimeMillis(),
                    address = null
                )
            )
        }

        loadAlertDetails(alertId)
        loadCurrentLocation()
    }

    private fun loadAlertDetails(alertId: String) {
        viewModelScope.launch {
            sosRepository.observeSOSAlert(alertId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data.let { alert ->
                            _uiState.update {
                                it.copy(
                                    userName = alert.userName,
                                    userPhone = alert.userPhone,
                                    emergencyType = alert.alertType.name,
                                    destinationLocation = alert.location
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // Load helper response status
        viewModelScope.launch {
            helperRepository.getActiveResponse().collect { resource ->
                val response = resource.getOrNull()
                if (response != null && response.alertId == alertId) {
                    responseId = response.id
                    _uiState.update { 
                        it.copy(
                            status = if (response.status == ResponseStatus.ARRIVED) 
                                HelperStatus.BUSY else HelperStatus.RESPONDING 
                        ) 
                    }
                }
            }
        }
    }

    private fun loadCurrentLocation() {
        viewModelScope.launch {
            val location = locationTracker.getCurrentLocation()
            location?.let { loc ->
                val currentLoc = Location(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy,
                    timestamp = loc.time,
                    address = null
                )
                _uiState.update { it.copy(currentLocation = currentLoc) }
                calculateDistanceAndEta()
                loadRoute()
            }
        }
    }

    fun startLocationUpdates() {
        locationUpdateJob = viewModelScope.launch {
            locationTracker.getLocationUpdates(5000L).collect { location ->
                val currentLoc = Location(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    timestamp = location.time,
                    address = null
                )
                _uiState.update { it.copy(currentLocation = currentLoc) }
                calculateDistanceAndEta()

                // Update location on server
                helperRepository.updateHelperLocation(currentLoc)

                // Check if arrived (within 50 meters)
                checkIfArrived()
            }
        }
    }

    fun stopLocationUpdates() {
        locationUpdateJob?.cancel()
        locationUpdateJob = null
    }

    private fun calculateDistanceAndEta() {
        val current = _uiState.value.currentLocation ?: return
        val destination = _uiState.value.destinationLocation ?: return

        val distance = DistanceCalculator.calculateDistance(
            current.latitude,
            current.longitude,
            destination.latitude,
            destination.longitude
        )

        // Estimate ETA based on average speed (30 km/h for city driving)
        val etaMinutes = ((distance / 1000) / 30 * 60).toInt()
        val eta = when {
            etaMinutes < 1 -> "< 1 min"
            etaMinutes < 60 -> "$etaMinutes min"
            else -> "${etaMinutes / 60}h ${etaMinutes % 60}m"
        }

        _uiState.update {
            it.copy(
                distance = distance,
                eta = eta
            )
        }
    }

    private fun loadRoute() {
        val current = _uiState.value.currentLocation ?: return
        val destination = _uiState.value.destinationLocation ?: return

        viewModelScope.launch {
            // In a real app, you would call Directions API here
            // For now, we'll just create a straight line
            val routePoints = listOf(
                LatLng(current.latitude, current.longitude),
                LatLng(destination.latitude, destination.longitude)
            )

            _uiState.update { it.copy(routePoints = routePoints) }
        }
    }

    private fun checkIfArrived() {
        val distance = _uiState.value.distance ?: return

        if (distance <= 50 && _uiState.value.status != HelperStatus.BUSY) {
            // Auto prompt to mark as arrived
            _uiState.update { it.copy(showArrivalPrompt = true) }
        }
    }

    fun markAsArrived() {
        val id = responseId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = helperRepository.markArrived(id)
            _uiState.update { it.copy(isLoading = false) }
            
            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            status = HelperStatus.BUSY,
                            showArrivalPrompt = false
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

    fun cancelResponse() {
        val id = responseId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = helperRepository.cancelResponse(id)
            _uiState.update { it.copy(isLoading = false) }
            
            when (result) {
                is Resource.Success -> {
                    navigateBack()
                }
                is Resource.Error -> {
                    showToast(result.message ?: "Failed to cancel response")
                }
                else -> {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}

data class NavigationUiState(
    val isLoading: Boolean = false,
    val status: HelperStatus = HelperStatus.AVAILABLE,
    val userName: String? = null,
    val userPhone: String? = null,
    val emergencyType: String? = null,
    val currentLocation: Location? = null,
    val destinationLocation: Location? = null,
    val distance: Float? = null,
    val eta: String? = null,
    val routePoints: List<LatLng>? = null,
    val showArrivalPrompt: Boolean = false,
    val errorMessage: String? = null
)
