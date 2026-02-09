// app/src/main/java/com/safeguard/sos/presentation/auth/register/RegisterFragment.kt

package com.safeguard.sos.presentation.auth.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.databinding.FragmentRegisterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : BaseFragment<FragmentRegisterBinding>() {

    private val viewModel: RegisterViewModel by viewModels()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentRegisterBinding {
        return FragmentRegisterBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupTextWatchers()
    }

    protected override fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnRegister.setOnClickListener {
            hideKeyboard()
            attemptRegister()
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.tilPassword.setEndIconOnClickListener {
            viewModel.togglePasswordVisibility()
        }

        binding.tilConfirmPassword.setEndIconOnClickListener {
            viewModel.toggleConfirmPasswordVisibility()
        }
    }

    override fun observeData() {
        collectFlow(viewModel.uiState) { state ->
            updateUI(state)
        }

        collectFlow(viewModel.uiEvent) { event ->
            handleUiEvent(event)
        }

        collectFlow(viewModel.navigateToUserType) { shouldNavigate ->
            if (shouldNavigate) {
                val action = RegisterFragmentDirections.actionRegisterToUserTypeSelection()
                findNavController().navigate(action)
            }
        }
    }

    private fun setupTextWatchers() {
        binding.etFullName.doAfterTextChanged {
            binding.tilFullName.error = null
        }
        binding.etPhone.doAfterTextChanged {
            binding.tilPhone.error = null
        }
        binding.etEmail.doAfterTextChanged {
            binding.tilEmail.error = null
        }
        binding.etPassword.doAfterTextChanged {
            binding.tilPassword.error = null
        }
        binding.etConfirmPassword.doAfterTextChanged {
            binding.tilConfirmPassword.error = null
        }
    }

    private fun attemptRegister() {
        val fullName = binding.etFullName.text?.toString()?.trim() ?: ""
        val phone = binding.etPhone.text?.toString()?.trim() ?: ""
        val email = binding.etEmail.text?.toString()?.trim()
        val password = binding.etPassword.text?.toString() ?: ""
        val confirmPassword = binding.etConfirmPassword.text?.toString() ?: ""

        var isValid = true

        if (fullName.isBlank()) {
            binding.tilFullName.error = getString(R.string.error_empty_field)
            isValid = false
        } else if (fullName.length < 2) {
            binding.tilFullName.error = getString(R.string.error_invalid_name)
            isValid = false
        }

        if (phone.isBlank()) {
            binding.tilPhone.error = getString(R.string.error_empty_field)
            isValid = false
        } else if (phone.length != 10) {
            binding.tilPhone.error = getString(R.string.error_invalid_phone)
            isValid = false
        }

        if (!email.isNullOrBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        }

        if (password.isBlank()) {
            binding.tilPassword.error = getString(R.string.error_empty_field)
            isValid = false
        } else if (password.length < 8) {
            binding.tilPassword.error = getString(R.string.error_invalid_password)
            isValid = false
        }

        if (confirmPassword != password) {
            binding.tilConfirmPassword.error = getString(R.string.error_password_mismatch)
            isValid = false
        }

        if (isValid) {
            viewModel.saveRegistrationData(fullName, phone, email, password)
        }
    }

    private fun updateUI(state: RegisterUiState) {
        binding.apply {
            btnRegister.isEnabled = !state.isLoading
            progressIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // Password visibility
            etPassword.inputType = if (state.isPasswordVisible) {
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etPassword.setSelection(etPassword.text?.length ?: 0)

            etConfirmPassword.inputType = if (state.isConfirmPasswordVisible) {
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etConfirmPassword.setSelection(etConfirmPassword.text?.length ?: 0)

            state.error?.let { error ->
                showSnackbar(error)
                viewModel.clearError()
            }
        }
    }
}
