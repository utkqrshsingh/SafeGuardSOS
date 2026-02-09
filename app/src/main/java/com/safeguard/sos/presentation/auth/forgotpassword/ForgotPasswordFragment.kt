package com.safeguard.sos.presentation.auth.forgotpassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.core.extensions.isValidEmail
import com.safeguard.sos.core.extensions.isValidPhoneNumber
import com.safeguard.sos.databinding.FragmentForgotPasswordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgotPasswordFragment : BaseFragment<FragmentForgotPasswordBinding>() {

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentForgotPasswordBinding {
        return FragmentForgotPasswordBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupInputListeners()
        observeState()
        observeEvents()
    }

    private fun setupUI() {
        binding.apply {
            btnSubmit.isEnabled = false
            btnSubmit.alpha = 0.5f

            // Default to email tab
            tabEmail.isSelected = true
            tilEmail.isVisible = true
            tilPhone.isVisible = false
        }
    }

    protected override fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                findNavController().navigateUp()
            }

            tabEmail.setOnClickListener {
                selectTab(isEmail = true)
            }

            tabPhone.setOnClickListener {
                selectTab(isEmail = false)
            }

            btnSubmit.setOnClickListener {
                hideKeyboard()

                if (tabEmail.isSelected) {
                    val email = etEmail.text.toString().trim()
                    viewModel.sendPasswordResetEmail(email)
                } else {
                    val phone = etPhone.text.toString().trim()
                    viewModel.sendPasswordResetSms(phone)
                }
            }

            tvBackToLogin.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun selectTab(isEmail: Boolean) {
        binding.apply {
            tabEmail.isSelected = isEmail
            tabPhone.isSelected = !isEmail

            tilEmail.isVisible = isEmail
            tilPhone.isVisible = !isEmail

            // Update hint text
            tvInputHint.text = if (isEmail) {
                getString(R.string.forgot_password_email_hint)
            } else {
                getString(R.string.forgot_password_phone_hint)
            }

            // Clear inputs and errors
            etEmail.text?.clear()
            etPhone.text?.clear()
            tilEmail.error = null
            tilPhone.error = null

            // Disable button
            btnSubmit.isEnabled = false
            btnSubmit.alpha = 0.5f
        }
    }

    private fun setupInputListeners() {
        binding.apply {
            etEmail.doAfterTextChanged { text ->
                val email = text.toString().trim()
                if (email.isEmpty()) {
                    tilEmail.error = null
                    btnSubmit.isEnabled = false
                    btnSubmit.alpha = 0.5f
                } else if (!email.isValidEmail()) {
                    tilEmail.error = getString(R.string.error_invalid_email)
                    btnSubmit.isEnabled = false
                    btnSubmit.alpha = 0.5f
                } else {
                    tilEmail.error = null
                    btnSubmit.isEnabled = true
                    btnSubmit.alpha = 1f
                }
            }

            etPhone.doAfterTextChanged { text ->
                val phone = text.toString().trim()
                if (phone.isEmpty()) {
                    tilPhone.error = null
                    btnSubmit.isEnabled = false
                    btnSubmit.alpha = 0.5f
                } else if (!phone.isValidPhoneNumber()) {
                    tilPhone.error = getString(R.string.error_invalid_phone)
                    btnSubmit.isEnabled = false
                    btnSubmit.alpha = 0.5f
                } else {
                    tilPhone.error = null
                    btnSubmit.isEnabled = true
                    btnSubmit.alpha = 1f
                }
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collectLatest { event ->
                    when (event) {
                        is UiEvent.Success -> {
                            showSuccessState()
                        }
                        is UiEvent.Error -> {
                            showErrorDialog(message = event.message)
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

    private fun updateUI(state: ForgotPasswordUiState) {
        binding.apply {
            progressBar.isVisible = state.isLoading
            btnSubmit.text = if (state.isLoading) "" else getString(R.string.send_reset_link)

            // Disable inputs during loading
            etEmail.isEnabled = !state.isLoading
            etPhone.isEnabled = !state.isLoading
            tabEmail.isEnabled = !state.isLoading
            tabPhone.isEnabled = !state.isLoading
        }
    }

    private fun showSuccessState() {
        binding.apply {
            // Hide input form
            inputContainer.isVisible = false

            // Show success state
            successContainer.isVisible = true

            val successMessage = if (tabEmail.isSelected) {
                getString(R.string.reset_email_sent, etEmail.text.toString().trim())
            } else {
                getString(R.string.reset_sms_sent, etPhone.text.toString().trim())
            }
            tvSuccessMessage.text = successMessage
        }
    }
}
