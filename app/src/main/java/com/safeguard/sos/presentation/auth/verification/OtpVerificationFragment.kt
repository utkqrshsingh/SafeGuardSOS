package com.safeguard.sos.presentation.auth.verification

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.databinding.FragmentOtpVerificationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class OtpVerificationFragment : BaseFragment<FragmentOtpVerificationBinding>() {

    private val viewModel: VerificationViewModel by activityViewModels()

    private var countDownTimer: CountDownTimer? = null
    private lateinit var otpEditTexts: List<EditText>

    companion object {
        private const val RESEND_TIMEOUT_MILLIS = 60000L // 60 seconds
        private const val COUNTDOWN_INTERVAL = 1000L
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentOtpVerificationBinding {
        return FragmentOtpVerificationBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupOtpInputs()
        setupUI()
        startResendTimer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }

    private fun setupUI() {
        binding.apply {
            // Display masked Aadhaar number
            tvAadhaarMasked.text = getString(R.string.otp_sent_to_aadhaar, viewModel.getMaskedAadhaar())

            btnVerifyOtp.isEnabled = false
            btnVerifyOtp.alpha = 0.5f
        }
    }

    private fun setupOtpInputs() {
        binding.apply {
            otpEditTexts = listOf(etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6)

            otpEditTexts.forEachIndexed { index, editText ->
                editText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(s: Editable?) {
                        if (s?.length == 1 && index < otpEditTexts.size - 1) {
                            // Move to next field
                            otpEditTexts[index + 1].requestFocus()
                        }

                        // Update verification button state
                        updateVerifyButtonState()
                    }
                })

                editText.setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_DEL &&
                        event.action == KeyEvent.ACTION_DOWN &&
                        editText.text.isNullOrEmpty() &&
                        index > 0
                    ) {
                        // Move to previous field on backspace
                        otpEditTexts[index - 1].apply {
                            requestFocus()
                            text?.clear()
                        }
                        true
                    } else {
                        false
                    }
                }
            }

            // Focus on first input
            etOtp1.requestFocus()
        }
    }

    private fun updateVerifyButtonState() {
        val otp = getEnteredOtp()
        val isComplete = otp.length == 6

        binding.apply {
            btnVerifyOtp.isEnabled = isComplete
            btnVerifyOtp.alpha = if (isComplete) 1f else 0.5f
        }
    }

    private fun getEnteredOtp(): String {
        return otpEditTexts.joinToString("") { it.text.toString() }
    }

    private fun clearOtpInputs() {
        otpEditTexts.forEach { it.text?.clear() }
        otpEditTexts.first().requestFocus()
    }

    override fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                navController.navigateUp()
            }

            btnVerifyOtp.setOnClickListener {
                val otp = getEnteredOtp()
                hideKeyboard()
                viewModel.verifyOtp(otp)
            }

            tvResendOtp.setOnClickListener {
                if (tvResendOtp.isEnabled) {
                    clearOtpInputs()
                    viewModel.resendOtp()
                    startResendTimer()
                }
            }

            tvChangeAadhaar.setOnClickListener {
                navController.navigateUp()
            }
        }
    }

    private fun startResendTimer() {
        binding.apply {
            tvResendOtp.isEnabled = false
            tvResendOtp.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        }

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(RESEND_TIMEOUT_MILLIS, COUNTDOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)
                binding.tvResendOtp.text = getString(R.string.resend_otp_timer, seconds)
            }

            override fun onFinish() {
                binding.apply {
                    tvResendOtp.text = getString(R.string.resend_otp)
                    tvResendOtp.isEnabled = true
                    tvResendOtp.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent))
                }
            }
        }.start()
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.verificationState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: VerificationUiState) {
        binding.apply {
            progressBar.isVisible = state.isLoading
            btnVerifyOtp.isEnabled = !state.isLoading && getEnteredOtp().length == 6
            btnVerifyOtp.text = if (state.isLoading) getString(R.string.verifying) else getString(R.string.verify_otp)

            // Lock OTP inputs during loading
            otpEditTexts.forEach { it.isEnabled = !state.isLoading }

            if (state.isVerified) {
                showSuccessAnimation()
            }

            state.error?.let {
                showSnackbar(it)
                viewModel.clearError()
                clearOtpInputs()
            }
        }
    }

    private fun showSuccessAnimation() {
        binding.apply {
            // In a real app, maybe show a success container or navigate
            showToast(getString(R.string.verification_successful))
            
            // Navigate after small delay
            view?.postDelayed({
                navigateToMain()
            }, 1000)
        }
    }

    private fun navigateToMain() {
        // Navigate to registration complete or main
        // This depends on the navigation graph
        // For now, let's assume there's a direction
        try {
            // findNavController().navigate(R.id.action_otpVerification_to_registrationComplete)
            showToast(getString(R.string.redirecting))
        } catch (e: Exception) {
            // Fallback
        }
    }
}
