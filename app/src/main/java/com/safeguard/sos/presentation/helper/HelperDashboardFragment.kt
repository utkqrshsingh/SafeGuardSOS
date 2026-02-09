package com.safeguard.sos.presentation.helper

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
import com.safeguard.sos.databinding.FragmentHelperDashboardBinding
import com.safeguard.sos.domain.model.HelperStatus
import com.safeguard.sos.presentation.components.adapters.SOSHistoryAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HelperDashboardFragment : BaseFragment<FragmentHelperDashboardBinding>() {

    private val viewModel: HelperDashboardViewModel by viewModels()
    private lateinit var alertsAdapter: SOSHistoryAdapter

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHelperDashboardBinding {
        return FragmentHelperDashboardBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupRecyclerView()
        viewModel.checkHelperRegistration()
        startAnimations()
    }

    private fun setupRecyclerView() {
        alertsAdapter = SOSHistoryAdapter(
            onItemClick = { alert ->
                // try {
                //    val action = HelperDashboardFragmentDirections
                //        .actionHelperDashboardFragmentToAlertDetailFragment(alert.id)
                //    navController.navigate(action)
                // } catch (e: Exception) {}
            }
        )

        binding.rvNearbyAlerts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = alertsAdapter
        }
    }

    override fun setupClickListeners() {
        binding.apply {
            // Helper mode toggle
            switchHelperMode.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked && !hasLocationPermission()) {
                    switchHelperMode.isChecked = false
                    showLocationPermissionDialog()
                } else {
                    viewModel.toggleHelperMode(isChecked)
                }
            }

            // Availability status
            cardAvailabilityOnline.setOnClickListener {
                viewModel.setAvailabilityStatus(HelperStatus.AVAILABLE)
            }

            cardAvailabilityBusy.setOnClickListener {
                viewModel.setAvailabilityStatus(HelperStatus.BUSY)
            }

            cardAvailabilityOffline.setOnClickListener {
                viewModel.setAvailabilityStatus(HelperStatus.OFFLINE)
            }

            // View all alerts
            tvViewAllAlerts.setOnClickListener {
                // navController.navigate(R.id.action_helperDashboardFragment_to_nearbyAlertsFragment)
            }

            // View history
            cardHistory.setOnClickListener {
                // navController.navigate(R.id.helperHistoryFragment)
            }

            // Register as helper (if not registered)
            btnRegisterHelper.setOnClickListener {
                // navController.navigate(R.id.action_helperDashboardFragment_to_helperRegistrationFragment)
            }

            // Swipe refresh
            swipeRefresh.setOnRefreshListener {
                viewModel.refreshData()
            }

            // Active response card
            cardActiveResponse.setOnClickListener {
                viewModel.uiState.value.activeResponseAlertId?.let { alertId ->
                    // try {
                    //    val action = HelperDashboardFragmentDirections
                    //        .actionHelperDashboardFragmentToAlertDetailFragment(alertId)
                    //    navController.navigate(action)
                    // } catch (e: Exception) {}
                }
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

    private fun updateUI(state: HelperDashboardUiState) {
        binding.apply {
            swipeRefresh.isRefreshing = state.isLoading

            // Show registration prompt if not registered
            if (!state.isRegisteredHelper) {
                helperContent.isVisible = false
                registrationPrompt.isVisible = true
                return
            }

            helperContent.isVisible = true
            registrationPrompt.isVisible = false

            // Helper mode toggle
            switchHelperMode.isChecked = state.isHelperModeActive
            tvHelperModeStatus.text = if (state.isHelperModeActive) {
                getString(R.string.helper_mode_active_status)
            } else {
                getString(R.string.helper_mode_inactive_status)
            }

            // Availability status
            updateAvailabilityCards(state.availabilityStatus)

            // Stats
            tvPeopleHelped.text = state.peopleHelpedCount.toString()
            tvResponseTime.text = state.averageResponseTime
            tvRating.text = String.format("%.1f", state.rating)
            tvTotalResponses.text = state.totalResponses.toString()

            // Active response
            if (state.hasActiveResponse) {
                cardActiveResponse.isVisible = true
                tvActiveResponseUser.text = state.activeResponseUserName
                tvActiveResponseType.text = state.activeResponseType
                tvActiveResponseTime.text = state.activeResponseTime
            } else {
                cardActiveResponse.isVisible = false
            }

            // Nearby alerts
            if (state.nearbyAlerts.isEmpty()) {
                rvNearbyAlerts.isVisible = false
                emptyAlertsContainer.isVisible = true
                tvEmptyMessage.text = if (state.isHelperModeActive) {
                    getString(R.string.no_nearby_alerts)
                } else {
                    getString(R.string.enable_helper_mode_message)
                }
            } else {
                rvNearbyAlerts.isVisible = true
                emptyAlertsContainer.isVisible = false
                alertsAdapter.submitList(state.nearbyAlerts)
            }

            // Alerts count badge
            if (state.nearbyAlerts.isNotEmpty()) {
                badgeAlertCount.isVisible = true
                tvAlertCount.text = state.nearbyAlerts.size.toString()
            } else {
                badgeAlertCount.isVisible = false
            }

            // Location status
            if (state.currentLocation != null) {
                tvLocationStatus.text = getString(R.string.location_active)
                tvLocationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
                ivLocationStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
            } else {
                tvLocationStatus.text = getString(R.string.location_unavailable)
                tvLocationStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning))
                ivLocationStatus.setColorFilter(ContextCompat.getColor(requireContext(), R.color.warning))
            }

            // Verification badge
            ivVerifiedBadge.isVisible = state.isVerified
        }
    }

    private fun updateAvailabilityCards(status: HelperStatus) {
        binding.apply {
            // Reset all cards
            listOf(cardAvailabilityOnline, cardAvailabilityBusy, cardAvailabilityOffline).forEach { card ->
                card.strokeWidth = 0
            }

            // Highlight selected
            when (status) {
                HelperStatus.AVAILABLE -> {
                    cardAvailabilityOnline.strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_selected)
                    cardAvailabilityOnline.strokeColor = ContextCompat.getColor(requireContext(), R.color.success)
                }
                HelperStatus.BUSY -> {
                    cardAvailabilityBusy.strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_selected)
                    cardAvailabilityBusy.strokeColor = ContextCompat.getColor(requireContext(), R.color.warning)
                }
                HelperStatus.OFFLINE -> {
                    cardAvailabilityOffline.strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_selected)
                    cardAvailabilityOffline.strokeColor = ContextCompat.getColor(requireContext(), R.color.text_hint)
                }
                else -> {}
            }
        }
    }

    private fun startAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        binding.headerContainer.startAnimation(fadeIn)
    }

    private fun showLocationPermissionDialog() {
        showAlertDialog(
            title = getString(R.string.location_required),
            message = getString(R.string.helper_location_permission_message),
            positiveButton = getString(R.string.grant_permission),
            negativeButton = getString(R.string.cancel),
            onPositiveClick = {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST
                )
            }
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.toggleHelperMode(true)
                binding.switchHelperMode.isChecked = true
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 2001
    }
}
