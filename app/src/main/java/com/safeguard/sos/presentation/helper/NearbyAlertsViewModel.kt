package com.safeguard.sos.presentation.helper

import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.repository.LocationRepository
import com.safeguard.sos.domain.usecase.helper.GetNearbySOSAlertsUseCase
import com.safeguard.sos.domain.usecase.helper.RespondToSOSUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyAlertsViewModel @Inject constructor(
    private val getNearbySOSAlertsUseCase: GetNearbySOSAlertsUseCase,
    private val respondToSOSUseCase: RespondToSOSUseCase,
    private val locationRepository: LocationRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(NearbyAlertsUiState())
    val uiState: StateFlow<NearbyAlertsUiState> = _uiState.asStateFlow()

    private var allAlerts: List<SOSAlert> = emptyList()

    init {
        loadAlerts()
    }

    private fun loadAlerts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val locationResult = locationRepository.getCurrentLocation()
            when (locationResult) {
                is Resource.Success -> {
                    val location = locationResult.data
                    getNearbySOSAlertsUseCase(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        radiusKm = _uiState.value.radiusKm
                    ).collect { alertsResult ->
                        when (alertsResult) {
                            is Resource.Success -> {
                                allAlerts = alertsResult.data
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        alerts = allAlerts
                                    )
                                }
                            }
                            is Resource.Error -> {
                                _uiState.update { it.copy(isLoading = false) }
                                showToast(alertsResult.message)
                            }
                            is Resource.Loading -> {
                                _uiState.update { it.copy(isLoading = true) }
                            }
                            is Resource.Empty -> {
                                _uiState.update { it.copy(isLoading = false, alerts = emptyList()) }
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    showToast("Unable to get location")
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
                is Resource.Empty -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun refreshAlerts() {
        loadAlerts()
    }

    fun filterAlerts(type: String?) {
        val filtered = if (type == null) {
            allAlerts
        } else {
            allAlerts.filter { it.alertType.name.equals(type, ignoreCase = true) }
        }
        _uiState.update { it.copy(alerts = filtered, currentFilter = type) }
    }

    fun sortByDistance() {
        // SOSAlert doesn't have distance field, usually this would be computed
        // Sorting by id as placeholder or keeping original order if distance not available
        _uiState.update { it.copy(sortBy = SortBy.DISTANCE) }
    }

    fun sortByTime() {
        val sorted = _uiState.value.alerts.sortedByDescending { it.createdAt }
        _uiState.update { it.copy(alerts = sorted, sortBy = SortBy.TIME) }
    }

    fun respondToAlert(alertId: String) {
        viewModelScope.launch {
            respondToSOSUseCase(alertId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        showToast("Responding to alert")
                        loadAlerts()
                    }
                    is Resource.Error -> {
                        showToast(result.message)
                    }
                    else -> {}
                }
            }
        }
    }
}

data class NearbyAlertsUiState(
    val isLoading: Boolean = false,
    val alerts: List<SOSAlert> = emptyList(),
    val radiusKm: Double = 10.0,
    val currentFilter: String? = null,
    val sortBy: SortBy = SortBy.DISTANCE
)

enum class SortBy {
    DISTANCE,
    TIME
}
