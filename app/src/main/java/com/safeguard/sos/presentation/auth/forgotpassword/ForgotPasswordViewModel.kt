package com.safeguard.sos.presentation.auth.forgotpassword

import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            authRepository.sendPasswordResetEmail(email).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isResetSent = true,
                                sentTo = email,
                                resetMethod = ResetMethod.EMAIL,
                                errorMessage = null
                            )
                        }
                        sendUiEvent(UiEvent.Success)
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message
                            )
                        }
                        sendUiEvent(UiEvent.Error(result.message))
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun sendPasswordResetSms(phone: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            authRepository.sendPasswordResetSms(phone).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isResetSent = true,
                                sentTo = phone,
                                resetMethod = ResetMethod.SMS,
                                errorMessage = null
                            )
                        }
                        sendUiEvent(UiEvent.Success)
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message
                            )
                        }
                        sendUiEvent(UiEvent.Error(result.message))
                    }
                    else -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            }
        }
    }

    fun resetState() {
        _uiState.update { ForgotPasswordUiState() }
    }
}

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val isResetSent: Boolean = false,
    val sentTo: String = "",
    val resetMethod: ResetMethod = ResetMethod.EMAIL,
    val errorMessage: String? = null
)

enum class ResetMethod {
    EMAIL,
    SMS
}
