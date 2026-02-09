package com.safeguard.sos.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        // User Keys
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_PHONE = stringPreferencesKey("user_phone")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val IS_HELPER = booleanPreferencesKey("is_helper")
        val IS_VERIFIED = booleanPreferencesKey("is_verified")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")

        // Onboarding
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        // Notification Settings
        val SOS_NOTIFICATIONS_ENABLED = booleanPreferencesKey("sos_notifications_enabled")
        val HELPER_ALERTS_ENABLED = booleanPreferencesKey("helper_alerts_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")

        // Privacy Settings
        val SHARE_LOCATION_HELPERS = booleanPreferencesKey("share_location_helpers")
        val SHARE_LOCATION_CONTACTS = booleanPreferencesKey("share_location_contacts")
        val SHOW_PROFILE_TO_HELPERS = booleanPreferencesKey("show_profile_to_helpers")
        val ALLOW_ANONYMOUS_SOS = booleanPreferencesKey("allow_anonymous_sos")

        // SOS Settings
        val SOS_COUNTDOWN_DURATION = intPreferencesKey("sos_countdown_duration")
        val AUTO_RECORD_AUDIO = booleanPreferencesKey("auto_record_audio")
        val SHAKE_TO_SOS = booleanPreferencesKey("shake_to_sos")
        val POWER_BUTTON_SOS = booleanPreferencesKey("power_button_sos")
        val HELPER_RADIUS = intPreferencesKey("helper_radius")

        // App Settings
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")

        // Stats
        val EMERGENCY_CONTACT_COUNT = intPreferencesKey("emergency_contact_count")
    }

    // User Flows
    val userIdFlow: Flow<String?> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[USER_ID] }

    val isLoggedInFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[IS_LOGGED_IN] ?: false }

    val isHelperFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[IS_HELPER] ?: false }

    val authTokenFlow: Flow<String?> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[AUTH_TOKEN] }

    val onboardingCompletedFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[ONBOARDING_COMPLETED] ?: false }

    // Notification Settings Flows
    val sosNotificationsEnabledFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[SOS_NOTIFICATIONS_ENABLED] ?: true }

    val helperAlertsEnabledFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[HELPER_ALERTS_ENABLED] ?: true }

    val soundEnabledFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[SOUND_ENABLED] ?: true }

    val vibrationEnabledFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[VIBRATION_ENABLED] ?: true }

    // Privacy Settings Flows
    val shareLocationWithHelpersFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[SHARE_LOCATION_HELPERS] ?: true }

    val shareLocationWithContactsFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[SHARE_LOCATION_CONTACTS] ?: true }

    val showProfileToHelpersFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[SHOW_PROFILE_TO_HELPERS] ?: true }

    val allowAnonymousSOSFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[ALLOW_ANONYMOUS_SOS] ?: false }

    // SOS Settings Flows
    val sosCountdownDurationFlow: Flow<Int> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[SOS_COUNTDOWN_DURATION] ?: 5 }

    val autoRecordAudioFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[AUTO_RECORD_AUDIO] ?: true }

    val shakeToSOSFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[SHAKE_TO_SOS] ?: false }

    val powerButtonSOSFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[POWER_BUTTON_SOS] ?: false }

    val helperRadiusFlow: Flow<Int> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[HELPER_RADIUS] ?: 10 }

    // App Settings Flows
    val darkModeFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[DARK_MODE] ?: true }

    val autoStartOnBootFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[AUTO_START_ON_BOOT] ?: true }

    val keepScreenOnFlow: Flow<Boolean> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[KEEP_SCREEN_ON] ?: false }

    // Stats Flows
    val emergencyContactCountFlow: Flow<Int> = dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[EMERGENCY_CONTACT_COUNT] ?: 0 }

    // User Setters
    suspend fun setUserData(userId: String, name: String, phone: String, email: String?) {
        dataStore.edit { preferences ->
            preferences[USER_ID] = userId
            preferences[USER_NAME] = name
            preferences[USER_PHONE] = phone
            email?.let { preferences[USER_EMAIL] = it }
            preferences[IS_LOGGED_IN] = true
        }
    }

    suspend fun setAuthToken(token: String) {
        dataStore.edit { it[AUTH_TOKEN] = token }
    }

    suspend fun setRefreshToken(token: String) {
        dataStore.edit { it[REFRESH_TOKEN] = token }
    }

    suspend fun setIsHelper(isHelper: Boolean) {
        dataStore.edit { it[IS_HELPER] = isHelper }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    // Notification Settings Setters
    suspend fun setSOSNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[SOS_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setHelperAlertsEnabled(enabled: Boolean) {
        dataStore.edit { it[HELPER_ALERTS_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }

    // Privacy Settings Setters
    suspend fun setShareLocationWithHelpers(enabled: Boolean) {
        dataStore.edit { it[SHARE_LOCATION_HELPERS] = enabled }
    }

    suspend fun setShareLocationWithContacts(enabled: Boolean) {
        dataStore.edit { it[SHARE_LOCATION_CONTACTS] = enabled }
    }

    suspend fun setShowProfileToHelpers(enabled: Boolean) {
        dataStore.edit { it[SHOW_PROFILE_TO_HELPERS] = enabled }
    }

    suspend fun setAllowAnonymousSOS(enabled: Boolean) {
        dataStore.edit { it[ALLOW_ANONYMOUS_SOS] = enabled }
    }

    // SOS Settings Setters
    suspend fun setSOSCountdownDuration(duration: Int) {
        dataStore.edit { it[SOS_COUNTDOWN_DURATION] = duration }
    }

    suspend fun setAutoRecordAudio(enabled: Boolean) {
        dataStore.edit { it[AUTO_RECORD_AUDIO] = enabled }
    }

    suspend fun setShakeToSOS(enabled: Boolean) {
        dataStore.edit { it[SHAKE_TO_SOS] = enabled }
    }

    suspend fun setPowerButtonSOS(enabled: Boolean) {
        dataStore.edit { it[POWER_BUTTON_SOS] = enabled }
    }

    suspend fun setHelperRadius(radius: Int) {
        dataStore.edit { it[HELPER_RADIUS] = radius }
    }

    // App Settings Setters
    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setAutoStartOnBoot(enabled: Boolean) {
        dataStore.edit { it[AUTO_START_ON_BOOT] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[KEEP_SCREEN_ON] = enabled }
    }

    // Stats Setters
    suspend fun setEmergencyContactCount(count: Int) {
        dataStore.edit { it[EMERGENCY_CONTACT_COUNT] = count }
    }

    // Clear and Reset
    suspend fun clearUserData() {
        dataStore.edit { preferences ->
            preferences.remove(USER_ID)
            preferences.remove(USER_NAME)
            preferences.remove(USER_PHONE)
            preferences.remove(USER_EMAIL)
            preferences.remove(AUTH_TOKEN)
            preferences.remove(REFRESH_TOKEN)
            preferences[IS_LOGGED_IN] = false
            preferences[IS_HELPER] = false
            preferences[IS_VERIFIED] = false
        }
    }

    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences[SOS_NOTIFICATIONS_ENABLED] = true
            preferences[HELPER_ALERTS_ENABLED] = true
            preferences[SOUND_ENABLED] = true
            preferences[VIBRATION_ENABLED] = true
            preferences[SHARE_LOCATION_HELPERS] = true
            preferences[SHARE_LOCATION_CONTACTS] = true
            preferences[SHOW_PROFILE_TO_HELPERS] = true
            preferences[ALLOW_ANONYMOUS_SOS] = false
            preferences[SOS_COUNTDOWN_DURATION] = 5
            preferences[AUTO_RECORD_AUDIO] = true
            preferences[SHAKE_TO_SOS] = false
            preferences[POWER_BUTTON_SOS] = false
            preferences[HELPER_RADIUS] = 10
            preferences[DARK_MODE] = true
            preferences[AUTO_START_ON_BOOT] = true
            preferences[KEEP_SCREEN_ON] = false
        }
    }
}