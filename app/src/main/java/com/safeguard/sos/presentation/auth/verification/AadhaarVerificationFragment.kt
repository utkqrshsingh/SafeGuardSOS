// presentation/auth/verification/AadhaarVerificationFragment.kt
package com.safeguard.sos.presentation.auth.verification

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.core.utils.AadhaarValidator
import com.safeguard.sos.databinding.FragmentAadhaarVerificationBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AadhaarVerificationFragment : BaseFragment<FragmentAadhaarVerificationBinding>() {

    private val verificationViewModel: VerificationViewModel by activityViewModels()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAadhaarVerificationBinding {
        return FragmentAadhaarVerificationBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        binding.btnVerifyAadhaar.isEnabled = false
        binding.btnVerifyAadhaar.alpha = 0.5f
        binding.tilAadhaarNumber.requestFocus()
        setupTextWatchers()
    }

    private fun setupTextWatchers() {
        binding.etAadhaarNumber.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true

                // Remove all spaces first
                val digits = s.toString().replace(" ", "")

                // Limit to 12 digits
                val trimmed = if (digits.length > 12) digits.substring(0, 12) else digits

                // Format as XXXX XXXX XXXX
                val formatted = StringBuilder()
                for (i in trimmed.indices) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(' ')
                    }
                    formatted.append(trimmed[i])
                }

                binding.etAadhaarNumber.setText(formatted.toString())
                binding.etAadhaarNumber.setSelection(formatted.length)

                // Validate
                val isValid = AadhaarValidator.isValidFormat(trimmed)
                updateValidationUI(trimmed, isValid)

                isFormatting = false
            }
        })
    }

    private fun updateValidationUI(aadhaarDigits: String, isValid: Boolean) {
        when {
            aadhaarDigits.isEmpty() -> {
                binding.tilAadhaarNumber.error = null
                binding.tilAadhaarNumber.isErrorEnabled = false
                binding.ivValidationIcon.isVisible = false
                binding.btnVerifyAadhaar.isEnabled = false
                binding.btnVerifyAadhaar.alpha = 0.5f
            }
            aadhaarDigits.length < 12 -> {
                binding.tilAadhaarNumber.error = null
                binding.tilAadhaarNumber.isErrorEnabled = false
                binding.ivValidationIcon.isVisible = false
                binding.tvCharCount.text = getString(
                    R.string.aadhaar_digits_remaining,
                    12 - aadhaarDigits.length
                )
                binding.tvCharCount.isVisible = true
                binding.btnVerifyAadhaar.isEnabled = false
                binding.btnVerifyAadhaar.alpha = 0.5f
            }
            aadhaarDigits.length == 12 && isValid -> {
                binding.tilAadhaarNumber.error = null
                binding.tilAadhaarNumber.isErrorEnabled = false
                binding.ivValidationIcon.isVisible = true
                // Using a common check drawable if specific one is missing
                binding.ivValidationIcon.setImageResource(android.R.drawable.presence_online)
                binding.tvCharCount.isVisible = false
                binding.btnVerifyAadhaar.isEnabled = true
                binding.btnVerifyAadhaar.alpha = 1.0f
            }
            aadhaarDigits.length == 12 && !isValid -> {
                binding.tilAadhaarNumber.error = getString(R.string.aadhaar_invalid)
                binding.ivValidationIcon.isVisible = true
                binding.ivValidationIcon.setImageResource(android.R.drawable.stat_notify_error)
                binding.tvCharCount.isVisible = false
                binding.btnVerifyAadhaar.isEnabled = false
                binding.btnVerifyAadhaar.alpha = 0.5f
            }
        }
    }

    override fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            navController.navigateUp()
        }

        binding.btnVerifyAadhaar.setOnClickListener {
            val aadhaarNumber = binding.etAadhaarNumber.text.toString().replace(" ", "")
            if (AadhaarValidator.isValidFormat(aadhaarNumber)) {
                verificationViewModel.initiateAadhaarVerification(aadhaarNumber)
            } else {
                binding.tilAadhaarNumber.error = getString(R.string.aadhaar_invalid)
            }
        }

        binding.tvWhyAadhaar.setOnClickListener {
            showAadhaarInfoDialog()
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                verificationViewModel.verificationState.collect { state ->
                    handleVerificationState(state)
                }
            }
        }
    }

    private fun handleVerificationState(state: VerificationUiState) {
        binding.progressBar.isVisible = state.isLoading

        if (state.isLoading) {
            binding.btnVerifyAadhaar.isEnabled = false
            binding.btnVerifyAadhaar.text = getString(R.string.verifying)
            binding.etAadhaarNumber.isEnabled = false
        } else {
            binding.btnVerifyAadhaar.text = getString(R.string.verify_send_otp)
            binding.etAadhaarNumber.isEnabled = true
            
            if (state.isOtpSent) {
                navigateTo(R.id.action_aadhaarVerification_to_otpVerification)
                verificationViewModel.resetOtpState() // Clear state to avoid multiple navigations
            }
            
            state.error?.let {
                showSnackbar(it)
                verificationViewModel.clearError()
            }
        }
    }

    private fun showAadhaarInfoDialog() {
        showAlertDialog(
            title = getString(R.string.why_aadhaar_title),
            message = getString(R.string.why_aadhaar_message),
            positiveButton = getString(R.string.understood)
        )
    }
}
