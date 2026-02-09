package com.safeguard.sos.presentation.sos

import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.domain.model.AlertType
import com.safeguard.sos.domain.repository.SOSRepository
import com.safeguard.sos.domain.usecase.sos.TriggerSOSUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SOSCountdownViewModel @Inject constructor(
    private val sosRepository: SOSRepository,
    private val triggerSOSUseCase: TriggerSOSUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SOSCountdownUiState())
    val uiState: StateFlow<SOSCountdownUiState> = _uiState.asStateFlow()

    init {
        // loadPreparedSOSData() // Placeholder as it might not exist
    }

    fun triggerSOS() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }

            val result = triggerSOSUseCase()
            when (result) {
                is Resource.Success -> {
                    val sosAlert = result.data
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            sosId = sosAlert.id,
                            isTriggered = true
                        )
                    }
                    showToast("SOS Triggered")
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isSending = false) }
                    showToast(result.message ?: "Failed to send SOS")
                }
                else -> {
                    _uiState.update { it.copy(isSending = false) }
                }
            }
        }
    }

    fun cancelSOS() {
        navigateBack()
    }
}

data class SOSCountdownUiState(
    val isSending: Boolean = false,
    val isTriggered: Boolean = false,
    val sosId: String? = null,
    val emergencyType: AlertType = AlertType.EMERGENCY,
    val message: String = "",
    val isSilentMode: Boolean = false,
    val isContactsOnly: Boolean = false,
    val hasLocation: Boolean = false,
    val errorMessage: String? = null
)
