package com.safeguard.sos.presentation.main

import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.core.utils.NetworkUtil
import com.safeguard.sos.domain.model.User
import com.safeguard.sos.domain.model.UserType
import com.safeguard.sos.domain.repository.AuthRepository
import com.safeguard.sos.domain.repository.SOSRepository
import com.safeguard.sos.domain.repository.UserRepository
import com.safeguard.sos.domain.usecase.sos.TriggerSOSUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val sosRepository: SOSRepository,
    private val triggerSOSUseCase: TriggerSOSUseCase,
    private val networkUtil: NetworkUtil
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
        observeActiveSOSStatus()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            userType = user.userType,
                            isVerified = user.isVerified
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "User session not found"
                        )
                    }
                }
            }
        }
    }

    private fun observeActiveSOSStatus() {
        viewModelScope.launch {
            sosRepository.getActiveSOSAlerts().collect { result ->
                if (result is Resource.Success) {
                    val activeSOSList = result.data
                    _uiState.update {
                        it.copy(
                            isSOSActive = activeSOSList.isNotEmpty(),
                            activeSOSCount = activeSOSList.size
                        )
                    }
                }
            }
        }
    }

    fun triggerSOSFromWidget() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTriggering = true) }
            val result = triggerSOSUseCase()
            _uiState.update { it.copy(isTriggering = false) }
            
            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSOSActive = true) }
                    showToast("SOS Triggered")
                }
                is Resource.Error -> {
                    showToast(result.message ?: "Failed to trigger SOS")
                }
                else -> {}
            }
        }
    }

    fun onPermissionsGranted() {
        _uiState.update { it.copy(hasLocationPermission = true) }
    }

    fun onPermissionsDenied() {
        _uiState.update { it.copy(hasLocationPermission = false) }
        showToast("Some features may be limited without location permission")
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            val result = authRepository.logout()
            _uiState.update { it.copy(isLoggingOut = false) }
            
            when (result) {
                is Resource.Success -> {
                    _uiState.update { MainUiState() }
                    navigateTo("login")
                }
                is Resource.Error -> {
                    showToast(result.message ?: "Failed to logout")
                }
                else -> {}
            }
        }
    }

    fun refreshUserData() {
        loadUserData()
    }
}
