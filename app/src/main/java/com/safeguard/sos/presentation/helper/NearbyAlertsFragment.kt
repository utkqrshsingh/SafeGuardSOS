package com.safeguard.sos.presentation.helper

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.databinding.FragmentNearbyAlertsBinding
import com.safeguard.sos.presentation.components.adapters.SOSHistoryAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NearbyAlertsFragment : BaseFragment<FragmentNearbyAlertsBinding>() {

    private val viewModel: NearbyAlertsViewModel by viewModels()
    private lateinit var alertsAdapter: SOSHistoryAdapter

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNearbyAlertsBinding {
        return FragmentNearbyAlertsBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupRecyclerView()
        binding.chipAll.isChecked = true
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshAlerts()
    }

    private fun setupRecyclerView() {
        alertsAdapter = SOSHistoryAdapter(
            onItemClick = { alert ->
                // try {
                //    val action = NearbyAlertsFragmentDirections
                //        .actionNearbyAlertsFragmentToAlertDetailFragment(alert.id)
                //    navController.navigate(action)
                // } catch (e: Exception) {}
            }
        )

        binding.rvAlerts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = alertsAdapter
        }
    }

    override fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                navController.navigateUp()
            }

            swipeRefresh.setOnRefreshListener {
                viewModel.refreshAlerts()
            }

            // Sort options
            chipNearest.setOnClickListener {
                viewModel.sortByDistance()
            }

            chipRecent.setOnClickListener {
                viewModel.sortByTime()
            }

            // Map view
            btnMapView.setOnClickListener {
                // navController.navigate(R.id.nearbyHelpersMapFragment)
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

    private fun updateUI(state: NearbyAlertsUiState) {
        binding.apply {
            swipeRefresh.isRefreshing = state.isLoading

            // Alerts count
            tvAlertsCount.text = getString(R.string.alerts_count, state.alerts.size)

            // Location radius
            tvRadius.text = getString(R.string.within_km, state.radiusKm.toInt())

            if (state.alerts.isEmpty()) {
                rvAlerts.isVisible = false
                emptyContainer.isVisible = true
            } else {
                rvAlerts.isVisible = true
                emptyContainer.isVisible = false
                alertsAdapter.submitList(state.alerts)
            }
        }
    }
}
