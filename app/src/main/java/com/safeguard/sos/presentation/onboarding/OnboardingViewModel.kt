// app/src/main/java/com/safeguard/sos/presentation/onboarding/OnboardingViewModel.kt

package com.safeguard.sos.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguard.sos.data.local.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _navigateToAuth = MutableSharedFlow<Boolean>()
    val navigateToAuth: SharedFlow<Boolean> = _navigateToAuth.asSharedFlow()

    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferences.setOnboardingCompleted(true)
            _navigateToAuth.emit(true)
        }
    }
}