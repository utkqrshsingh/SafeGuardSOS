package com.safeguard.sos.presentation.settings

data class SettingsUiState(
    val isLoading: Boolean = false,

    // Notification Settings
    val sosNotificationsEnabled: Boolean = true,
    val helperAlertsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationSound: String = "Default",

    // Privacy Settings
    val shareLocationWithHelpers: Boolean = true,
    val shareLocationWithContacts: Boolean = true,
    val showProfileToHelpers: Boolean = true,
    val allowAnonymousSOS: Boolean = false,

    // SOS Settings
    val sosCountdownDuration: Int = 5,
    val autoRecordAudio: Boolean = true,
    val shakeToSOS: Boolean = false,
    val powerButtonSOS: Boolean = false,
    val helperRadius: Int = 10,

    // App Settings
    val darkModeEnabled: Boolean = true,
    val language: String = "English",
    val autoStartOnBoot: Boolean = true,
    val keepScreenOn: Boolean = false,
    val batteryOptimization: Boolean = true,

    // Cache
    val cacheSize: String = "0 MB",

    // App Info
    val appVersion: String = "1.0.0"
)

sealed class SettingsEvent {
    data class ShowMessage(val message: String) : SettingsEvent()
    object CacheCleared : SettingsEvent()
    object SettingsReset : SettingsEvent()
    object NavigateToPrivacy : SettingsEvent()
    object NavigateToNotifications : SettingsEvent()
    object NavigateToAbout : SettingsEvent()
}