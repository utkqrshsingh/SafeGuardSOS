package com.safeguard.sos.presentation.helper

import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.repository.HelperRepository
import com.safeguard.sos.domain.repository.SOSRepository
import com.safeguard.sos.domain.usecase.helper.RespondToSOSUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertDetailViewModel @Inject constructor(
    private val sosRepository: SOSRepository,
    private val helperRepository: HelperRepository,
    private val respondToSOSUseCase: RespondToSOSUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(AlertDetailUiState())
    val uiState: StateFlow<AlertDetailUiState> = _uiState.asStateFlow()

    private var alertId: String? = null
    private var responseId: String? = null

    fun loadAlertDetail(id: String) {
        alertId = id

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load alert details
            sosRepository.observeSOSAlert(id).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                alert = result.data
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        showToast(result.message)
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

        // Check if already responding
        viewModelScope.launch {
            helperRepository.getActiveResponse().collect { result ->
                if (result is Resource.Success) {
                    val response = result.data
                    if (response != null && response.sosAlertId == id) {
                        responseId = response.id
                        _uiState.update { it.copy(isResponding = true) }
                    } else {
                        _uiState.update { it.copy(isResponding = false) }
                    }
                }
            }
        }
    }

    fun respondToAlert() {
        val id = alertId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            respondToSOSUseCase(id).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isResponding = true
                            )
                        }
                        showToast("You are now responding to this alert")
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        showToast(result.message)
                    }
                    else -> {}
                }
            }
        }
    }

    fun markAsArrived() {
        val resId = responseId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = helperRepository.markArrived(resId)
            _uiState.update { it.copy(isLoading = false) }
            
            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(hasArrived = true) }
                    showToast("You have been marked as arrived")
                }
                is Resource.Error -> {
                    showToast(result.message ?: "Failed to update status")
                }
                else -> {}
            }
        }
    }

    fun cancelResponse() {
        val resId = responseId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = helperRepository.cancelResponse(resId)
            _uiState.update { it.copy(isLoading = false) }

            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isResponding = false,
                            hasArrived = false
                        )
                    }
                    showToast("Response cancelled")
                }
                is Resource.Error -> {
                    showToast(result.message ?: "Failed to cancel response")
                }
                else -> {}
            }
        }
    }
}

data class AlertDetailUiState(
    val isLoading: Boolean = false,
    val alert: SOSAlert? = null,
    val isResponding: Boolean = false,
    val hasArrived: Boolean = false
)
