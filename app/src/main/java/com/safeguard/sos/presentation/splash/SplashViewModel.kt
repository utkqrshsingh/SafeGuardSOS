package com.safeguard.sos.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguard.sos.data.local.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            // Minimum splash duration for branding
            delay(2000)

            // Correcting property names based on expected UserPreferences
            val isOnboardingCompleted = userPreferences.onboardingCompletedFlow.first()
            val isLoggedIn = !userPreferences.userIdFlow.first().isNullOrEmpty()

            _isLoading.value = false

            when {
                !isOnboardingCompleted -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToOnboarding)
                }
                !isLoggedIn -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToAuth)
                }
                else -> {
                    _navigationEvent.emit(NavigationEvent.NavigateToMain)
                }
            }
        }
    }

    sealed class NavigationEvent {
        data object NavigateToOnboarding : NavigationEvent()
        data object NavigateToAuth : NavigationEvent()
        data object NavigateToMain : NavigationEvent()
    }
}
