package com.safeguard.sos.presentation.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.databinding.FragmentSettingsBinding
import com.safeguard.sos.presentation.components.dialogs.ConfirmationDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingsBinding {
        return FragmentSettingsBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupToolbar()
        setupSwitches()
        setupSliders()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupSwitches() {
        binding.apply {
            // Notification switches
            switchSOSNotifications.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onSOSNotificationsChanged(isChecked)
            }
            switchHelperAlerts.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onHelperAlertsChanged(isChecked)
            }
            switchSound.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onSoundChanged(isChecked)
            }
            switchVibration.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onVibrationChanged(isChecked)
            }

            // Privacy switches
            switchShareLocationHelpers.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onShareLocationWithHelpersChanged(isChecked)
            }
            switchShareLocationContacts.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onShareLocationWithContactsChanged(isChecked)
            }
            switchShowProfile.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onShowProfileToHelpersChanged(isChecked)
            }

            // SOS switches
            switchAutoRecord.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onAutoRecordAudioChanged(isChecked)
            }
            switchShakeToSOS.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onShakeToSOSChanged(isChecked)
            }
            switchPowerButtonSOS.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onPowerButtonSOSChanged(isChecked)
            }

            // App switches
            switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onDarkModeChanged(isChecked)
            }
            switchAutoStart.setOnCheckedChangeListener { _, isChecked ->
                viewModel.onAutoStartOnBootChanged(isChecked)
            }
        }
    }

    private fun setupSliders() {
        binding.apply {
            // SOS Countdown Duration Slider
            sliderCountdown.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {}
                override fun onStopTrackingTouch(slider: Slider) {
                    viewModel.onSOSCountdownDurationChanged(slider.value.toInt())
                }
            })
            sliderCountdown.addOnChangeListener { _, value, _ ->
                textCountdownValue.text = "${value.toInt()} seconds"
            }

            // Helper Radius Slider
            sliderHelperRadius.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {}
                override fun onStopTrackingTouch(slider: Slider) {
                    viewModel.onHelperRadiusChanged(slider.value.toInt())
                }
            })
            sliderHelperRadius.addOnChangeListener { _, value, _ ->
                textRadiusValue.text = "${value.toInt()} km"
            }
        }
    }

    override fun setupClickListeners() {
        binding.apply {
            // Clear Cache
            cardClearCache.setOnClickListener {
                showClearCacheDialog()
            }

            // Reset Settings
            cardResetSettings.setOnClickListener {
                showResetSettingsDialog()
            }

            // Privacy Policy
            cardPrivacyPolicy.setOnClickListener {
                openUrl("https://safeguardsos.com/privacy")
            }

            // Terms of Service
            cardTermsOfService.setOnClickListener {
                openUrl("https://safeguardsos.com/terms")
            }

            // Rate App
            cardRateApp.setOnClickListener {
                openPlayStore()
            }

            // Share App
            cardShareApp.setOnClickListener {
                shareApp()
            }

            // About
            cardAbout.setOnClickListener {
                findNavController().navigate(R.id.action_settings_to_about)
            }

            // Contact Support
            cardContactSupport.setOnClickListener {
                sendSupportEmail()
            }
        }
    }

    private fun showClearCacheDialog() {
        ConfirmationDialog.show(
            requireContext(),
            ConfirmationDialog.Config(
                title = "Clear Cache",
                message = "This will clear all cached data. Are you sure?",
                positiveButtonText = "Clear",
                negativeButtonText = "Cancel",
                onPositiveClick = {
                    viewModel.clearCache()
                }
            )
        )
    }

    private fun showResetSettingsDialog() {
        ConfirmationDialog.show(
            requireContext(),
            ConfirmationDialog.Config(
                title = "Reset Settings",
                message = "This will reset all settings to their default values. This action cannot be undone.",
                positiveButtonText = "Reset",
                negativeButtonText = "Cancel",
                onPositiveClick = {
                    viewModel.resetSettings()
                }
            )
        )
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Unable to open link")
        }
    }

    private fun openPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=${requireContext().packageName}")
            }
            startActivity(intent)
        } catch (e: Exception) {
            openUrl("https://play.google.com/store/apps/details?id=${requireContext().packageName}")
        }
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "SafeGuard SOS")
            putExtra(Intent.EXTRA_TEXT,
                "Download SafeGuard SOS - Your Personal Safety Companion\n\n" +
                        "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun sendSupportEmail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@safeguardsos.com"))
                putExtra(Intent.EXTRA_SUBJECT, "SafeGuard SOS Support Request")
            }
            startActivity(intent)
        } catch (e: Exception) {
            showToast("No email app found")
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    updateUI(state)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is SettingsEvent.ShowMessage -> {
                            showToast(event.message)
                        }
                        is SettingsEvent.CacheCleared -> {
                            // Already handled by ShowMessage
                        }
                        is SettingsEvent.SettingsReset -> {
                            // Already handled by ShowMessage
                        }
                        is SettingsEvent.NavigateToAbout -> {
                            findNavController().navigate(R.id.action_settings_to_about)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateUI(state: SettingsUiState) {
        binding.apply {
            progressBar.isVisible = state.isLoading
            contentLayout.isVisible = !state.isLoading

            // Notification settings
            switchSOSNotifications.isChecked = state.sosNotificationsEnabled
            switchHelperAlerts.isChecked = state.helperAlertsEnabled
            switchSound.isChecked = state.soundEnabled
            switchVibration.isChecked = state.vibrationEnabled

            // Privacy settings
            switchShareLocationHelpers.isChecked = state.shareLocationWithHelpers
            switchShareLocationContacts.isChecked = state.shareLocationWithContacts
            switchShowProfile.isChecked = state.showProfileToHelpers

            // SOS settings
            sliderCountdown.value = state.sosCountdownDuration.toFloat()
            textCountdownValue.text = "${state.sosCountdownDuration} seconds"
            switchAutoRecord.isChecked = state.autoRecordAudio
            switchShakeToSOS.isChecked = state.shakeToSOS
            switchPowerButtonSOS.isChecked = state.powerButtonSOS
            sliderHelperRadius.value = state.helperRadius.toFloat()
            textRadiusValue.text = "${state.helperRadius} km"

            // App settings
            switchDarkMode.isChecked = state.darkModeEnabled
            switchAutoStart.isChecked = state.autoStartOnBoot

            // Cache
            textCacheSize.text = state.cacheSize

            // Version
            textAppVersion.text = "Version ${state.appVersion}"
        }
    }
}
