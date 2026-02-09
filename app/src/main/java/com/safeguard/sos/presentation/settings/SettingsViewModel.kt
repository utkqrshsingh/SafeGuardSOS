package com.safeguard.sos.presentation.settings

import androidx.lifecycle.viewModelScope
import com.safeguard.sos.core.base.BaseViewModel
import com.safeguard.sos.data.local.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Load all preferences
                val sosNotifications = userPreferences.sosNotificationsEnabledFlow.first()
                val helperAlerts = userPreferences.helperAlertsEnabledFlow.first()
                val sound = userPreferences.soundEnabledFlow.first()
                val vibration = userPreferences.vibrationEnabledFlow.first()
                val shareLocationHelpers = userPreferences.shareLocationWithHelpersFlow.first()
                val shareLocationContacts = userPreferences.shareLocationWithContactsFlow.first()
                val showProfile = userPreferences.showProfileToHelpersFlow.first()
                val countdown = userPreferences.sosCountdownDurationFlow.first()
                val autoRecord = userPreferences.autoRecordAudioFlow.first()
                val shakeSOS = userPreferences.shakeToSOSFlow.first()
                val powerSOS = userPreferences.powerButtonSOSFlow.first()
                val radius = userPreferences.helperRadiusFlow.first()
                val darkMode = userPreferences.darkModeFlow.first()
                val autoStart = userPreferences.autoStartOnBootFlow.first()

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        sosNotificationsEnabled = sosNotifications,
                        helperAlertsEnabled = helperAlerts,
                        soundEnabled = sound,
                        vibrationEnabled = vibration,
                        shareLocationWithHelpers = shareLocationHelpers,
                        shareLocationWithContacts = shareLocationContacts,
                        showProfileToHelpers = showProfile,
                        sosCountdownDuration = countdown,
                        autoRecordAudio = autoRecord,
                        shakeToSOS = shakeSOS,
                        powerButtonSOS = powerSOS,
                        helperRadius = radius,
                        darkModeEnabled = darkMode,
                        autoStartOnBoot = autoStart
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(SettingsEvent.ShowMessage("Failed to load settings"))
            }
        }
    }

    // Notification Settings
    fun onSOSNotificationsChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setSOSNotificationsEnabled(enabled)
            _uiState.update { it.copy(sosNotificationsEnabled = enabled) }
        }
    }

    fun onHelperAlertsChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setHelperAlertsEnabled(enabled)
            _uiState.update { it.copy(helperAlertsEnabled = enabled) }
        }
    }

    fun onSoundChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setSoundEnabled(enabled)
            _uiState.update { it.copy(soundEnabled = enabled) }
        }
    }

    fun onVibrationChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setVibrationEnabled(enabled)
            _uiState.update { it.copy(vibrationEnabled = enabled) }
        }
    }

    // Privacy Settings
    fun onShareLocationWithHelpersChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setShareLocationWithHelpers(enabled)
            _uiState.update { it.copy(shareLocationWithHelpers = enabled) }
        }
    }

    fun onShareLocationWithContactsChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setShareLocationWithContacts(enabled)
            _uiState.update { it.copy(shareLocationWithContacts = enabled) }
        }
    }

    fun onShowProfileToHelpersChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setShowProfileToHelpers(enabled)
            _uiState.update { it.copy(showProfileToHelpers = enabled) }
        }
    }

    fun onAllowAnonymousSOSChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAllowAnonymousSOS(enabled)
            _uiState.update { it.copy(allowAnonymousSOS = enabled) }
        }
    }

    // SOS Settings
    fun onSOSCountdownDurationChanged(duration: Int) {
        viewModelScope.launch {
            userPreferences.setSOSCountdownDuration(duration)
            _uiState.update { it.copy(sosCountdownDuration = duration) }
        }
    }

    fun onAutoRecordAudioChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoRecordAudio(enabled)
            _uiState.update { it.copy(autoRecordAudio = enabled) }
        }
    }

    fun onShakeToSOSChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setShakeToSOS(enabled)
            _uiState.update { it.copy(shakeToSOS = enabled) }
        }
    }

    fun onPowerButtonSOSChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setPowerButtonSOS(enabled)
            _uiState.update { it.copy(powerButtonSOS = enabled) }
        }
    }

    fun onHelperRadiusChanged(radius: Int) {
        viewModelScope.launch {
            userPreferences.setHelperRadius(radius)
            _uiState.update { it.copy(helperRadius = radius) }
        }
    }

    // App Settings
    fun onDarkModeChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setDarkMode(enabled)
            _uiState.update { it.copy(darkModeEnabled = enabled) }
        }
    }

    fun onAutoStartOnBootChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoStartOnBoot(enabled)
            _uiState.update { it.copy(autoStartOnBoot = enabled) }
        }
    }

    fun onKeepScreenOnChanged(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setKeepScreenOn(enabled)
            _uiState.update { it.copy(keepScreenOn = enabled) }
        }
    }

    // Actions
    fun clearCache() {
        viewModelScope.launch {
            // Implement cache clearing logic
            _uiState.update { it.copy(cacheSize = "0 MB") }
            _events.emit(SettingsEvent.CacheCleared)
            _events.emit(SettingsEvent.ShowMessage("Cache cleared successfully"))
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            userPreferences.resetToDefaults()
            loadSettings()
            _events.emit(SettingsEvent.SettingsReset)
            _events.emit(SettingsEvent.ShowMessage("Settings reset to defaults"))
        }
    }

    fun navigateToPrivacy() {
        viewModelScope.launch {
            _events.emit(SettingsEvent.NavigateToPrivacy)
        }
    }

    fun navigateToNotifications() {
        viewModelScope.launch {
            _events.emit(SettingsEvent.NavigateToNotifications)
        }
    }

    fun navigateToAbout() {
        viewModelScope.launch {
            _events.emit(SettingsEvent.NavigateToAbout)
        }
    }
}