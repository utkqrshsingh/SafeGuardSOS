// app/src/main/java/com/safeguard/sos/presentation/auth/login/LoginFragment.kt

package com.safeguard.sos.presentation.auth.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.databinding.FragmentLoginBinding
import com.safeguard.sos.presentation.auth.AuthActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding>() {

    private val viewModel: LoginViewModel by viewModels()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLoginBinding {
        return FragmentLoginBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupTextWatchers()
    }

    protected override fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            hideKeyboard()
            attemptLogin()
        }

        binding.btnLoginWithGoogle.setOnClickListener {
            viewModel.loginWithGoogle()
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgotPassword)
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.tilPassword.setEndIconOnClickListener {
            viewModel.togglePasswordVisibility()
        }
    }

    override fun observeData() {
        collectFlow(viewModel.uiState) { state ->
            updateUI(state)
        }

        collectFlow(viewModel.uiEvent) { event ->
            handleUiEvent(event)
        }

        collectFlow(viewModel.loginSuccess) { success ->
            if (success) {
                (activity as? AuthActivity)?.navigateToMain()
            }
        }
    }

    private fun setupTextWatchers() {
        binding.etPhone.doAfterTextChanged { text ->
            viewModel.onPhoneChanged(text?.toString() ?: "")
            binding.tilPhone.error = null
        }

        binding.etPassword.doAfterTextChanged { text ->
            viewModel.onPasswordChanged(text?.toString() ?: "")
            binding.tilPassword.error = null
        }
    }

    private fun attemptLogin() {
        val phone = binding.etPhone.text?.toString() ?: ""
        val password = binding.etPassword.text?.toString() ?: ""

        // Validate fields
        var isValid = true

        if (phone.isBlank()) {
            binding.tilPhone.error = getString(R.string.error_empty_field)
            isValid = false
        } else if (phone.length != 10) {
            binding.tilPhone.error = getString(R.string.error_invalid_phone)
            isValid = false
        }

        if (password.isBlank()) {
            binding.tilPassword.error = getString(R.string.error_empty_field)
            isValid = false
        } else if (password.length < 8) {
            binding.tilPassword.error = getString(R.string.error_invalid_password)
            isValid = false
        }

        if (isValid) {
            viewModel.login(phone, password)
        }
    }

    private fun updateUI(state: LoginUiState) {
        binding.apply {
            // Loading state
            btnLogin.isEnabled = !state.isLoading
            btnLoginWithGoogle.isEnabled = !state.isLoading
            progressIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            // Password visibility
            etPassword.inputType = if (state.isPasswordVisible) {
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            etPassword.setSelection(etPassword.text?.length ?: 0)

            tilPassword.setEndIconDrawable(
                if (state.isPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye
            )

            // Error state
            state.error?.let { error ->
                showSnackbar(error)
                viewModel.clearError()
            }
        }
    }
}
