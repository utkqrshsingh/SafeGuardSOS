// presentation/auth/register/RegistrationCompleteFragment.kt
package com.safeguard.sos.presentation.auth.register

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.safeguard.sos.R
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.presentation.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegistrationCompleteFragment : Fragment() {

    private val viewModel: RegisterViewModel by viewModels()

    private lateinit var ivSuccess: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var btnGetStarted: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_registration_complete, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        completeRegistration()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        ivSuccess = view.findViewById(R.id.iv_success)
        tvTitle = view.findViewById(R.id.tv_title)
        tvSubtitle = view.findViewById(R.id.tv_subtitle)
        btnGetStarted = view.findViewById(R.id.btn_get_started)
    }

    private fun completeRegistration() {
        arguments?.let { args ->
            val fullName = args.getString("fullName", "")
            val email = args.getString("email", "")
            val phone = args.getString("phone", "")
            val password = args.getString("password", "")
            val userType = args.getString("userType", "USER")
            val aadhaarNumber = args.getString("aadhaarNumber", "")

            viewModel.completeRegistration(
                fullName = fullName,
                email = email,
                phone = phone,
                password = password,
                userType = userType,
                aadhaarNumber = aadhaarNumber
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registerState.collect { state ->
                    when (state) {
                        is Resource.Loading -> {
                            btnGetStarted.isEnabled = false
                            tvTitle.text = getString(R.string.setting_up)
                            tvSubtitle.text = getString(R.string.please_wait)
                        }
                        is Resource.Success -> {
                            tvTitle.text = getString(R.string.welcome_aboard)
                            tvSubtitle.text = getString(R.string.account_created_successfully)
                            btnGetStarted.isEnabled = true

                            // Animate success
                            ivSuccess.animate()
                                .scaleX(1.2f)
                                .scaleY(1.2f)
                                .setDuration(300)
                                .withEndAction {
                                    ivSuccess.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(200)
                                        .start()
                                }
                                .start()
                        }
                        is Resource.Error -> {
                            tvTitle.text = getString(R.string.registration_failed)
                            tvSubtitle.text = state.message
                            btnGetStarted.isEnabled = true
                            btnGetStarted.text = getString(R.string.try_again)
                        }
                        is Resource.Empty -> {
                            // Do nothing
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnGetStarted.setOnClickListener {
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        val intent = Intent(requireActivity(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }
}
