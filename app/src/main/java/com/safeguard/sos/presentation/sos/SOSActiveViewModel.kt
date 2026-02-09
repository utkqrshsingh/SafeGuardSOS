package com.safeguard.sos.presentation.sos

import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.domain.model.Helper
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.model.SOSStatus
import com.safeguard.sos.domain.repository.SOSRepository
import com.safeguard.sos.domain.usecase.sos.CancelSOSUseCase
import com.safeguard.sos.domain.usecase.sos.UpdateSOSStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SOSActiveViewModel @Inject constructor(
    private val sosRepository: SOSRepository,
    private val cancelSOSUseCase: CancelSOSUseCase,
    private val updateSOSStatusUseCase: UpdateSOSStatusUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SOSActiveUiState())
    val uiState: StateFlow<SOSActiveUiState> = _uiState.asStateFlow()

    private var sosId: String? = null

    fun loadSOSAlert(id: String) {
        sosId = id

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Observe SOS alert updates in real-time
            sosRepository.observeSOSAlert(id).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val alert = result.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                sosId = alert.id,
                                status = alert.status,
                                startTime = alert.createdAt,
                                location = alert.location,
                                respondingHelpers = emptyList(), // No list in SOSAlert model
                                notifiedContactsCount = 0, // No list in SOSAlert model
                                smsSentCount = 0,
                                nearestHelperEta = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        sendUiEvent(UiEvent.Error(result.message))
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun cancelSOS() {
        val id = sosId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            cancelSOSUseCase(id).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                status = SOSStatus.CANCELLED
                            )
                        }
                        sendUiEvent(UiEvent.Success)
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        sendUiEvent(UiEvent.Error(result.message))
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun markAsSafe() {
        val id = sosId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            updateSOSStatusUseCase(id, SOSStatus.RESOLVED).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                status = SOSStatus.RESOLVED
                            )
                        }
                        showToast("You've been marked as safe!")
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        sendUiEvent(UiEvent.Error(result.message))
                    }
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun sendUpdateMessage(message: String) {
        val id = sosId ?: return

        viewModelScope.launch {
            sosRepository.sendUpdateMessage(id, message).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        showToast("Message sent to helpers")
                    }
                    is Resource.Error -> {
                        sendUiEvent(UiEvent.Error(result.message))
                    }
                    else -> {}
                }
            }
        }
    }
}

data class SOSActiveUiState(
    val isLoading: Boolean = false,
    val sosId: String? = null,
    val status: SOSStatus = SOSStatus.PENDING,
    val startTime: Long? = null,
    val location: Location? = null,
    val respondingHelpers: List<Helper> = emptyList(),
    val notifiedContactsCount: Int = 0,
    val smsSentCount: Int = 0,
    val nearestHelperEta: String? = null,
    val errorMessage: String? = null
)
