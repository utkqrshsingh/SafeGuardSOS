package com.safeguard.sos.presentation.profile

import android.content.Intent
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
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.databinding.FragmentProfileBinding
import com.safeguard.sos.domain.model.VerificationStatus
import com.safeguard.sos.presentation.auth.AuthActivity
import com.safeguard.sos.presentation.components.dialogs.ConfirmationDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentProfileBinding {
        return FragmentProfileBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupToolbar()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    findNavController().navigate(R.id.action_profile_to_settings)
                    true
                }
                else -> false
            }
        }
    }

    override fun setupClickListeners() {
        binding.apply {
            // Edit Profile
            cardEditProfile.setOnClickListener {
                viewModel.onEditProfileClick()
            }

            buttonEditProfile.setOnClickListener {
                viewModel.onEditProfileClick()
            }

            // Medical Info
            cardMedicalInfo.setOnClickListener {
                viewModel.onMedicalInfoClick()
            }

            // Verification
            cardVerification.setOnClickListener {
                if (!viewModel.uiState.value.isVerified) {
                    viewModel.onVerificationClick()
                }
            }

            // Emergency Contacts
            cardEmergencyContacts.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_contacts)
            }

            // SOS History
            cardSOSHistory.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_sosHistory)
            }

            // Helper Stats (if visible)
            cardHelperStats.setOnClickListener {
                findNavController().navigate(R.id.action_profile_to_helperDashboard)
            }

            // Logout
            buttonLogout.setOnClickListener {
                showLogoutConfirmation()
            }

            // Swipe to refresh
            swipeRefreshLayout.setOnRefreshListener {
                viewModel.loadProfile()
            }
        }
    }

    private fun showLogoutConfirmation() {
        ConfirmationDialog.show(
            requireContext(),
            ConfirmationDialog.Config(
                title = "Logout",
                message = "Are you sure you want to logout?",
                positiveButtonText = "Logout",
                negativeButtonText = "Cancel",
                onPositiveClick = {
                    viewModel.logout()
                }
            )
        )
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
                        is ProfileEvent.ShowError -> {
                            showSnackbar(event.message)
                        }
                        is ProfileEvent.ShowSuccess -> {
                            showSnackbar(event.message)
                        }
                        is ProfileEvent.NavigateToEditProfile -> {
                            findNavController().navigate(R.id.action_profile_to_editProfile)
                        }
                        is ProfileEvent.NavigateToMedicalInfo -> {
                            findNavController().navigate(R.id.action_profile_to_medicalInfo)
                        }
                        is ProfileEvent.NavigateToVerification -> {
                            findNavController().navigate(R.id.action_profile_to_verification)
                        }
                        is ProfileEvent.ProfileUpdated -> {
                            // Already handled by loadProfile()
                        }
                        is ProfileEvent.LoggedOut -> {
                            navigateToLogin()
                        }
                    }
                }
            }
        }
    }

    private fun updateUI(state: ProfileUiState) {
        binding.apply {
            swipeRefreshLayout.isRefreshing = state.isLoading
            progressBar.isVisible = state.isLoading && state.user == null
            contentLayout.isVisible = state.user != null

            state.user?.let { user ->
                // Profile Header
                textUserName.text = user.name
                textUserPhone.text = user.phone
                textUserEmail.text = user.email ?: "Email not added"
                textUserEmail.isVisible = !user.email.isNullOrEmpty()

                // Profile Image
                if (!user.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(user.profileImageUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(imageProfile)
                } else {
                    imageProfile.setImageResource(R.drawable.ic_person)
                }

                // Verification Status
                updateVerificationStatus(state.verificationStatus)

                // Stats
                textContactCount.text = state.emergencyContactCount.toString()
                textSOSCount.text = state.sosCount.toString()
                textMemberSince.text = state.memberSince

                // Helper Section
                cardHelperStats.isVisible = state.isHelper
                if (state.isHelper) {
                    textHelpsProvided.text = state.helpsProvided.toString()
                }

                // User Type Badge
                badgeUserType.text = if (state.isHelper) "Helper" else "User"
                badgeUserType.setBackgroundResource(
                    if (state.isHelper) R.drawable.bg_badge_helper
                    else R.drawable.bg_badge_user
                )
            }

            // Show error
            state.error?.let { error ->
                showSnackbar(error)
            }
        }
    }

    private fun updateVerificationStatus(status: VerificationStatus) {
        binding.apply {
            when (status) {
                VerificationStatus.VERIFIED -> {
                    iconVerification.setImageResource(R.drawable.ic_verified)
                    iconVerification.setColorFilter(requireContext().getColor(R.color.success))
                    textVerificationStatus.text = "Verified"
                    textVerificationStatus.setTextColor(requireContext().getColor(R.color.success))
                    textVerificationHint.text = "Your identity has been verified"
                    cardVerification.isClickable = false
                }
                VerificationStatus.PENDING -> {
                    iconVerification.setImageResource(R.drawable.ic_pending)
                    iconVerification.setColorFilter(requireContext().getColor(R.color.warning))
                    textVerificationStatus.text = "Pending"
                    textVerificationStatus.setTextColor(requireContext().getColor(R.color.warning))
                    textVerificationHint.text = "Verification in progress"
                    cardVerification.isClickable = false
                }
                VerificationStatus.NOT_VERIFIED -> {
                    iconVerification.setImageResource(R.drawable.ic_warning)
                    iconVerification.setColorFilter(requireContext().getColor(R.color.error))
                    textVerificationStatus.text = "Not Verified"
                    textVerificationStatus.setTextColor(requireContext().getColor(R.color.error))
                    textVerificationHint.text = "Tap to verify your identity"
                    cardVerification.isClickable = true
                }
                VerificationStatus.FAILED -> {
                    iconVerification.setImageResource(R.drawable.ic_error)
                    iconVerification.setColorFilter(requireContext().getColor(R.color.error))
                    textVerificationStatus.text = "Verification Failed"
                    textVerificationStatus.setTextColor(requireContext().getColor(R.color.error))
                    textVerificationHint.text = "Tap to retry verification"
                    cardVerification.isClickable = true
                }
                VerificationStatus.UNVERIFIED -> {
                    iconVerification.setImageResource(R.drawable.ic_warning)
                    iconVerification.setColorFilter(requireContext().getColor(R.color.error))
                    textVerificationStatus.text = "Unverified"
                    textVerificationStatus.setTextColor(requireContext().getColor(R.color.error))
                    textVerificationHint.text = "Tap to verify your identity"
                    cardVerification.isClickable = true
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile()
    }
}
