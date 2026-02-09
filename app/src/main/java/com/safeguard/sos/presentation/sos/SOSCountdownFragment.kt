package com.safeguard.sos.presentation.sos

import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.databinding.FragmentSosCountdownBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SOSCountdownFragment : BaseFragment<FragmentSosCountdownBinding>() {

    private val viewModel: SOSCountdownViewModel by activityViewModels()

    private var countDownTimer: CountDownTimer? = null
    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null

    private var isCancelled = false

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSosCountdownBinding {
        return FragmentSosCountdownBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        vibrator = requireContext().getSystemService()
        setupUI()
        startCountdown()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancelCountdown()
        releaseMediaPlayer()
    }

    private fun setupUI() {
        binding.apply {
            val countdownSeconds = 5 // Default if args not working
            tvCountdown.text = countdownSeconds.toString()

            // Initial progress
            circularProgress.max = countdownSeconds * 1000
            circularProgress.progress = countdownSeconds * 1000

            // Start pulse animation
            startPulseAnimation()
        }
    }

    override fun setupClickListeners() {
        binding.apply {
            // Cancel button
            btnCancel.setOnClickListener {
                cancelSOS()
            }

            // Trigger immediately
            btnTriggerNow.setOnClickListener {
                triggerSOSImmediately()
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
                    handleUiEvent(event)
                }
            }
        }
    }

    private fun updateUI(state: SOSCountdownUiState) {
        binding.apply {
            // Emergency type
            tvEmergencyType.text = state.emergencyType.name

            // Location status
            if (state.hasLocation) {
                tvLocationStatus.text = getString(R.string.location_ready)
                tvLocationStatus.setCompoundDrawablesWithIntrinsicBounds(
                    android.R.drawable.presence_online, 0, 0, 0
                )
            } else {
                tvLocationStatus.text = getString(R.string.acquiring_location)
            }

            // Silent mode indicator
            cardSilentMode.isVisible = state.isSilentMode

            // Loading state
            progressSending.isVisible = state.isSending
            btnTriggerNow.isEnabled = !state.isSending
            btnCancel.isEnabled = !state.isSending
        }
    }

    private fun startCountdown() {
        val totalMillis = 5000L

        // Play countdown sound if not silent mode
        if (!viewModel.uiState.value.isSilentMode) {
            playCountdownSound()
        }

        countDownTimer = object : CountDownTimer(totalMillis, 100) {
            override fun onTick(millisUntilFinished: Long) {
                if (isCancelled) return

                val secondsRemaining = (millisUntilFinished / 1000).toInt() + 1

                binding.apply {
                    tvCountdown.text = secondsRemaining.toString()
                    circularProgress.progress = millisUntilFinished.toInt()
                }

                // Vibrate each second
                if (millisUntilFinished % 1000 < 100) {
                    vibrateDevice()
                }

                // Update countdown color
                when {
                    secondsRemaining <= 2 -> {
                        binding.tvCountdown.setTextColor(
                            requireContext().getColor(android.R.color.holo_red_light)
                        )
                    }
                    secondsRemaining <= 3 -> {
                        binding.tvCountdown.setTextColor(
                            requireContext().getColor(android.R.color.holo_orange_light)
                        )
                    }
                }
            }

            override fun onFinish() {
                if (!isCancelled) {
                    triggerSOS()
                }
            }
        }.start()
    }

    private fun cancelCountdown() {
        isCancelled = true
        countDownTimer?.cancel()
        countDownTimer = null
        vibrator?.cancel()
    }

    private fun cancelSOS() {
        cancelCountdown()
        showToast(getString(R.string.sos_cancelled))
        navController.navigateUp()
    }

    private fun triggerSOSImmediately() {
        cancelCountdown()
        triggerSOS()
    }

    private fun triggerSOS() {
        // Strong vibration pattern
        vibrateDevice(longVibration = true)

        // Show sending state
        binding.apply {
            tvCountdown.text = "!"
            tvCountdownLabel.text = getString(R.string.sending_alert)
            circularProgress.isVisible = false
            progressSending.isVisible = true
        }

        // Trigger the SOS
        viewModel.triggerSOS()
    }

    private fun startPulseAnimation() {
        val pulseAnim = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        binding.sosIconContainer.startAnimation(pulseAnim)
    }

    private fun vibrateDevice(longVibration: Boolean = false) {
        vibrator?.let { v ->
            val duration = if (longVibration) 500L else 100L
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val amplitude = if (longVibration) VibrationEffect.DEFAULT_AMPLITUDE else 100
                v.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(duration)
            }
        }
    }

    private fun playCountdownSound() {
        // try {
        //    mediaPlayer = MediaPlayer.create(requireContext(), R.raw.countdown_beep)
        //    mediaPlayer?.isLooping = true
        //    mediaPlayer?.start()
        // } catch (e: Exception) {}
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
