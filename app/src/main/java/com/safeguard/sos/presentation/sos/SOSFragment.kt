package com.safeguard.sos.presentation.sos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.databinding.FragmentSosBinding
import com.safeguard.sos.domain.model.AlertType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SOSFragment : BaseFragment<FragmentSosBinding>() {

    private val viewModel: SOSViewModel by viewModels()
    private var vibrator: Vibrator? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSosBinding {
        return FragmentSosBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        vibrator = requireContext().getSystemService()

        // Check permissions
        updatePermissionStatus()

        // Setup emergency type chips
        binding.chipEmergencyMedical.isChecked = true
        
        startAnimations()
    }

    override fun setupClickListeners() {
        binding.apply {
            // Back button
            btnBack.setOnClickListener {
                findNavController().navigateUp()
            }

            // Main SOS Button - Long press to trigger
            btnSOS.setOnLongClickListener {
                if (hasRequiredPermissions()) {
                    vibrateDevice()
                    showSOSConfirmation()
                } else {
                    showPermissionDialog()
                }
                true
            }

            // Quick tap shows hint
            btnSOS.setOnClickListener {
                vibrateDevice(shortVibration = true)
                showToast(getString(R.string.long_press_to_trigger))
            }

            // Silent Mode Toggle
            switchSilentMode.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setSilentMode(isChecked)
                if (isChecked) {
                    showToast(getString(R.string.silent_mode_enabled))
                }
            }

            // Emergency Type Chips
            chipGroupEmergency.setOnCheckedStateChangeListener { _, checkedIds ->
                val alertType = when {
                    checkedIds.contains(R.id.chipEmergencyMedical) -> AlertType.MEDICAL
                    checkedIds.contains(R.id.chipEmergencySafety) -> AlertType.EMERGENCY
                    checkedIds.contains(R.id.chipEmergencyAccident) -> AlertType.ACCIDENT
                    checkedIds.contains(R.id.chipEmergencyOther) -> AlertType.OTHER
                    else -> AlertType.OTHER
                }
                viewModel.setEmergencyType(alertType)
            }

            // Send to contacts only
            switchContactsOnly.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setContactsOnly(isChecked)
            }

            // Add message
            etAdditionalMessage.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    viewModel.setAdditionalMessage(etAdditionalMessage.text.toString())
                }
            }

            // Test SOS (Debug only)
            btnTestSOS.setOnClickListener {
                showTestSOSDialog()
            }

            // View Contacts
            tvManageContacts.setOnClickListener {
                findNavController().navigate(R.id.contactsFragment)
            }
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is UiEvent.Success -> {
                            // Navigate to countdown
                            navigateToCountdown()
                        }
                        is UiEvent.Error -> {
                            showToast(event.message)
                        }
                        is UiEvent.ShowToast -> {
                            showToast(event.message)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateUI(state: SOSUiState) {
        binding.apply {
            // Update emergency contacts count
            tvContactsCount.text = getString(R.string.contacts_will_be_notified, state.emergencyContactsCount)

            // Update nearby helpers count
            tvHelpersCount.text = getString(R.string.helpers_in_range, state.nearbyHelpersCount)

            // Location status
            if (state.currentLocation != null) {
                tvLocationStatus.text = getString(R.string.location_acquired)
                tvLocationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
                ivLocationStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
            } else {
                tvLocationStatus.text = getString(R.string.acquiring_location)
                tvLocationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning))
                ivLocationStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.warning))
            }

            // Active SOS warning
            cardActiveWarning.isVisible = state.hasActiveAlert

            // Silent mode
            switchSilentMode.isChecked = state.isSilentMode

            // Contacts only
            switchContactsOnly.isChecked = state.isContactsOnly

            // Loading state
            progressBar.isVisible = state.isLoading
        }
    }

    private fun showSOSConfirmation() {
        val state = viewModel.uiState.value

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm SOS")
            .setMessage("Are you sure you want to trigger SOS?")
            .setPositiveButton("Trigger") { _, _ ->
                viewModel.initiateSOSTrigger()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTestSOSDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.test_sos_title)
            .setMessage(R.string.test_sos_message)
            .setIcon(R.drawable.ic_warning)
            .setPositiveButton(R.string.send_test) { _, _ ->
                viewModel.sendTestSOS()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permissions_required)
            .setMessage(R.string.sos_permissions_message)
            .setIcon(R.drawable.ic_location)
            .setPositiveButton(R.string.grant_permissions) { _, _ ->
                requestPermissions()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startAnimations() {
        binding.apply {
            // Pulse animation for SOS button
            startSOSButtonPulse()

            // Fade in content
            val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            contentContainer.startAnimation(fadeIn)
        }
    }

    private fun startSOSButtonPulse() {
        binding.apply {
            val scaleUp = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_animation)
            btnSOS.startAnimation(scaleUp)

            // Outer rings animation
            pulseRing1.animate()
                .scaleX(2f)
                .scaleY(2f)
                .alpha(0f)
                .setDuration(2000)
                .withEndAction {
                    pulseRing1.scaleX = 1f
                    pulseRing1.scaleY = 1f
                    pulseRing1.alpha = 0.4f
                    startSOSButtonPulse()
                }
                .start()

            pulseRing2.postDelayed({
                pulseRing2.animate()
                    .scaleX(2f)
                    .scaleY(2f)
                    .alpha(0f)
                    .setDuration(2000)
                    .withEndAction {
                        pulseRing2.scaleX = 1f
                        pulseRing2.scaleY = 1f
                        pulseRing2.alpha = 0.3f
                    }
                    .start()
            }, 500)

            pulseRing3.postDelayed({
                pulseRing3.animate()
                    .scaleX(2f)
                    .scaleY(2f)
                    .alpha(0f)
                    .setDuration(2000)
                    .withEndAction {
                        pulseRing3.scaleX = 1f
                        pulseRing3.scaleY = 1f
                        pulseRing3.alpha = 0.2f
                    }
                    .start()
            }, 1000)
        }
    }

    private fun vibrateDevice(shortVibration: Boolean = false) {
        vibrator?.let { v ->
            val duration = if (shortVibration) 50L else 200L
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(duration)
            }
        }
    }

    private fun updatePermissionStatus() {
        val hasLocation = hasRequiredPermissions()
        viewModel.updatePermissionStatus(hasLocation)

        binding.apply {
            if (!hasLocation) {
                cardPermissionWarning.isVisible = true
                btnSOS.alpha = 0.7f
            } else {
                cardPermissionWarning.isVisible = false
                btnSOS.alpha = 1f
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updatePermissionStatus()
                viewModel.refreshLocation()
            } else {
                showToast(getString(R.string.location_permission_denied))
            }
        }
    }

    private fun navigateToCountdown() {
        // findNavController().navigate(R.id.action_sosFragment_to_sosCountdownFragment)
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}
