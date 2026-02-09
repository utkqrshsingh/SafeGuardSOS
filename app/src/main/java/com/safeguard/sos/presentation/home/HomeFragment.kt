package com.safeguard.sos.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.databinding.FragmentHomeBinding
import com.safeguard.sos.domain.model.SOSStatus
import com.safeguard.sos.presentation.components.adapters.SOSHistoryAdapter
import com.safeguard.sos.presentation.components.adapters.NearbyHelpersAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var recentAlertsAdapter: SOSHistoryAdapter
    private lateinit var nearbyHelpersAdapter: NearbyHelpersAdapter

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupRecyclerViews()
        updateLocationStatus()
        startAnimations()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }

    private fun setupRecyclerViews() {
        // Recent Alerts Adapter
        recentAlertsAdapter = SOSHistoryAdapter { alert ->
            // try {
            //    val action = HomeFragmentDirections.actionHomeFragmentToSosDetailFragment(alert.id)
            //    navController.navigate(action)
            // } catch (e: Exception) {}
        }

        binding.rvRecentAlerts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentAlertsAdapter
            isNestedScrollingEnabled = false
        }

        // Nearby Helpers Adapter
        nearbyHelpersAdapter = NearbyHelpersAdapter { helper ->
            showToast("${helper.name} - nearby")
        }

        binding.rvNearbyHelpers.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = nearbyHelpersAdapter
        }
    }

    override fun setupClickListeners() {
        binding.apply {
            // Main SOS Button
            btnSOS.setOnClickListener {
                navigateToSOS()
            }

            // Quick Actions
            cardQuickCall.setOnClickListener {
                viewModel.quickCallEmergencyContact()
            }

            cardQuickLocation.setOnClickListener {
                viewModel.shareCurrentLocation()
            }

            cardQuickAlert.setOnClickListener {
                navigateToSOS()
            }

            cardNearbyHelpers.setOnClickListener {
                // navController.navigate(R.id.action_homeFragment_to_nearbyHelpersMapFragment)
            }

            // View All History
            tvViewAllHistory.setOnClickListener {
                // navController.navigate(R.id.action_homeFragment_to_sosHistoryFragment)
            }

            // Profile Avatar
            ivProfileAvatar.setOnClickListener {
                navController.navigate(R.id.profileFragment)
            }

            // Notification Bell
            ivNotification.setOnClickListener {
                showToast("Notifications coming soon")
            }

            // Active SOS Card
            cardActiveAlert.setOnClickListener {
                viewModel.uiState.value.activeSOSId?.let { sosId ->
                    // try {
                    //    val action = HomeFragmentDirections.actionHomeFragmentToSosActiveFragment(sosId)
                    //    navController.navigate(action)
                    // } catch (e: Exception) {}
                }
            }

            // Safety Tips
            cardSafetyTip.setOnClickListener {
                viewModel.loadNextSafetyTip()
            }

            // Swipe refresh
            swipeRefresh.setOnRefreshListener {
                viewModel.refreshData()
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

    private fun updateUI(state: HomeUiState) {
        binding.apply {
            // Loading state
            swipeRefresh.isRefreshing = state.isLoading

            // User greeting
            tvGreeting.text = getGreeting()
            tvUserName.text = state.userName

            // Profile avatar initials
            tvAvatarInitials.text = state.userInitials

            // Verification badge
            ivVerifiedBadge.isVisible = state.isVerified

            // Active SOS Alert Card
            cardActiveAlert.isVisible = state.hasActiveAlert
            if (state.hasActiveAlert) {
                tvActiveAlertStatus.text = when (state.activeSOSStatus) {
                    SOSStatus.PENDING -> getString(R.string.alert_status_pending)
                    SOSStatus.ACTIVE -> getString(R.string.alert_status_active)
                    SOSStatus.RESPONDED -> getString(R.string.help_arrived)
                    else -> getString(R.string.alert_status_active)
                }
                tvActiveAlertTime.text = state.activeSOSTime
                tvHelpersResponding.text = getString(R.string.helpers_responding_count, state.respondingHelpersCount)
            }

            // Safety Status Card
            updateSafetyStatus(state)

            // Stats
            tvTotalAlerts.text = state.totalAlerts.toString()
            tvHelpersNearby.text = state.nearbyHelpersCount.toString()
            tvPeopleHelped.text = state.peopleHelped.toString()

            // Recent Alerts
            if (state.recentAlerts.isEmpty()) {
                rvRecentAlerts.isVisible = false
                tvNoRecentAlerts.isVisible = true
            } else {
                rvRecentAlerts.isVisible = true
                tvNoRecentAlerts.isVisible = false
                recentAlertsAdapter.submitList(state.recentAlerts)
            }

            // Nearby Helpers
            if (state.nearbyHelpers.isEmpty()) {
                rvNearbyHelpers.isVisible = false
                tvNoNearbyHelpers.isVisible = true
            } else {
                rvNearbyHelpers.isVisible = true
                tvNoNearbyHelpers.isVisible = false
                nearbyHelpersAdapter.submitList(state.nearbyHelpers)
            }

            // Safety Tip
            state.currentSafetyTip?.let { tip ->
                tvSafetyTipTitle.text = tip.title
                tvSafetyTipContent.text = tip.content
                ivSafetyTipIcon.setImageResource(tip.iconRes)
            }

            // Helper Mode Toggle (for users who are also helpers)
            switchHelperMode.isVisible = state.isHelper
            switchHelperMode.isChecked = state.isHelperModeActive
            tvHelperModeStatus.text = if (state.isHelperModeActive) {
                getString(R.string.helper_mode_active)
            } else {
                getString(R.string.helper_mode_inactive)
            }
        }
    }

    private fun updateSafetyStatus(state: HomeUiState) {
        binding.apply {
            when {
                state.hasActiveAlert -> {
                    cardSafetyStatus.setCardBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.error_alpha_10)
                    )
                    ivSafetyIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                    ivSafetyIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error))
                    tvSafetyStatus.text = getString(R.string.alert_active)
                    tvSafetyStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
                    tvSafetyMessage.text = getString(R.string.help_is_on_the_way)
                }
                !state.hasLocationPermission -> {
                    cardSafetyStatus.setCardBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.warning_alpha_10)
                    )
                    // Use a default icon if ic_location_off is missing
                    ivSafetyIcon.setImageResource(android.R.drawable.ic_menu_mylocation) 
                    ivSafetyIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.warning))
                    tvSafetyStatus.text = getString(R.string.limited_protection)
                    tvSafetyStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning))
                    tvSafetyMessage.text = getString(R.string.enable_location_message)
                }
                else -> {
                    cardSafetyStatus.setCardBackgroundColor(
                        ContextCompat.getColor(requireContext(), R.color.success_alpha_10)
                    )
                    ivSafetyIcon.setImageResource(android.R.drawable.checkbox_on_background)
                    ivSafetyIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
                    tvSafetyStatus.text = getString(R.string.you_are_protected)
                    tvSafetyStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
                    tvSafetyMessage.text = getString(R.string.protection_message, state.nearbyHelpersCount)
                }
            }
        }
    }

    private fun updateLocationStatus() {
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.updateLocationPermissionStatus(hasPermission)
    }

    private fun startAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        binding.headerContainer.startAnimation(fadeIn)
    }

    private fun getGreeting(): String {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 0..11 -> getString(R.string.good_morning)
            in 12..16 -> getString(R.string.good_afternoon)
            in 17..20 -> getString(R.string.good_evening)
            else -> getString(R.string.good_night)
        }
    }

    private fun navigateToSOS() {
        // navController.navigate(R.id.action_homeFragment_to_sosFragment)
    }
}
