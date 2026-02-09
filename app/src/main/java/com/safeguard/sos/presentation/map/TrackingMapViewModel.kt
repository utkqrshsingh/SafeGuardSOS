package com.safeguard.sos.presentation.map

import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.HelperResponse
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.repository.SOSRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackingMapViewModel @Inject constructor(
    private val sosRepository: SOSRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(TrackingMapUiState())
    val uiState: StateFlow<TrackingMapUiState> = _uiState.asStateFlow()

    fun loadSOSData(sosId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Observe SOS Alert for location updates
            launch {
                sosRepository.observeSOSAlert(sosId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            _uiState.update { it.copy(userLocation = result.data.location) }
                        }
                        is Resource.Error -> {
                            // Error handling could be added here
                        }
                        else -> {}
                    }
                }
            }

            // Observe responding helpers
            launch {
                sosRepository.getSOSResponders(sosId).collect { responders ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            respondingHelpers = responders,
                            nearestHelper = responders.minByOrNull { h -> h.estimatedArrivalMinutes ?: Int.MAX_VALUE }
                        )
                    }
                }
            }
        }
    }
}

data class TrackingMapUiState(
    val isLoading: Boolean = false,
    val userLocation: Location? = null,
    val respondingHelpers: List<HelperResponse> = emptyList(),
    val nearestHelper: HelperResponse? = null
)
