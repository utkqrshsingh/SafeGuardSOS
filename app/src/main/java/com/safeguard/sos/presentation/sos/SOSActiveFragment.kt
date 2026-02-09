package com.safeguard.sos.presentation.sos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.databinding.FragmentSosActiveBinding
import com.safeguard.sos.domain.model.SOSStatus
import com.safeguard.sos.presentation.components.adapters.NearbyHelpersAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SOSActiveFragment : BaseFragment<FragmentSosActiveBinding>() {

    private val viewModel: SOSActiveViewModel by viewModels()
    private val args: SOSActiveFragmentArgs by navArgs()

    private lateinit var helpersAdapter: NearbyHelpersAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var elapsedSeconds = 0

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSosActiveBinding {
        return FragmentSosActiveBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        viewModel.loadSOSAlert(args.sosId)
        setupRecyclerView()
        startElapsedTimer()
        startPulseAnimation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }

    private fun setupRecyclerView() {
        helpersAdapter = NearbyHelpersAdapter { helper ->
            showToast("Helper: ${helper.name}")
        }

        binding.rvRespondingHelpers.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = helpersAdapter
        }
    }

    override fun setupClickListeners() {
        binding.apply {
            // Cancel SOS
            btnCancelSOS.setOnClickListener {
                showCancelConfirmation()
            }

            // Mark Safe
            btnMarkSafe.setOnClickListener {
                showMarkSafeConfirmation()
            }

            // View on Map
            btnViewMap.setOnClickListener {
                navigateToMap()
            }

            // Call Emergency Services
            btnCallEmergency.setOnClickListener {
                callEmergencyServices()
            }

            // Share Location
            btnShareLocation.setOnClickListener {
                shareLocation()
            }

            // Update Message
            btnUpdateMessage.setOnClickListener {
                showUpdateMessageDialog()
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

    private fun updateUI(state: SOSActiveUiState) {
        binding.apply {
            // Loading state
            progressBar.isVisible = state.isLoading
            contentContainer.isVisible = !state.isLoading

            // Status updates
            updateStatusUI(state.status)

            // Elapsed time
            state.startTime?.let { startTime ->
                elapsedSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            }

            // Location
            state.location?.let { location ->
                tvLocation.text = location.coordinates
                tvLocationAddress.text = location.displayAddress
            }

            // Responding helpers
            if (state.respondingHelpers.isEmpty()) {
                rvRespondingHelpers.isVisible = false
                tvNoHelpers.isVisible = true
                tvHelpersCount.text = getString(R.string.waiting_for_helpers)
            } else {
                rvRespondingHelpers.isVisible = true
                tvNoHelpers.isVisible = false
                tvHelpersCount.text = getString(R.string.helpers_responding_count, state.respondingHelpers.size)
                helpersAdapter.submitList(state.respondingHelpers)
            }

            // Contacted emergency contacts
            tvContactsNotified.text = getString(
                R.string.contacts_notified_count,
                state.notifiedContactsCount
            )

            // SMS sent status
            if (state.smsSentCount > 0) {
                tvSmsSent.isVisible = true
                tvSmsSent.text = getString(R.string.sms_sent_count, state.smsSentCount)
            }

            // Nearest helper ETA
            state.nearestHelperEta?.let { eta ->
                cardEta.isVisible = true
                tvEta.text = eta
            } ?: run {
                cardEta.isVisible = false
            }

            // Update buttons based on status
            when (state.status) {
                SOSStatus.RESOLVED, SOSStatus.CANCELLED, SOSStatus.FALSE_ALARM -> {
                    btnCancelSOS.isVisible = false
                    btnMarkSafe.isVisible = false
                }
                else -> {
                    btnCancelSOS.isVisible = true
                    btnMarkSafe.isVisible = true
                }
            }
        }
    }

    private fun updateStatusUI(status: SOSStatus) {
        binding.apply {
            when (status) {
                SOSStatus.PENDING -> {
                    tvStatus.text = getString(R.string.alert_pending)
                    tvStatusDesc.text = getString(R.string.sending_notifications)
                    // statusIndicator.setBackgroundResource(R.drawable.bg_status_pending)
                }
                SOSStatus.ACTIVE -> {
                    tvStatus.text = getString(R.string.alert_active)
                    tvStatusDesc.text = getString(R.string.helpers_notified)
                    // statusIndicator.setBackgroundResource(R.drawable.bg_status_active)
                }
                SOSStatus.HELP_ON_WAY -> {
                    tvStatus.text = getString(R.string.help_on_the_way)
                    tvStatusDesc.text = getString(R.string.helpers_coming)
                    // statusIndicator.setBackgroundResource(R.drawable.bg_status_responding)
                }
                SOSStatus.RESPONDED -> {
                    tvStatus.text = getString(R.string.help_arrived)
                    tvStatusDesc.text = getString(R.string.helper_with_you)
                    // statusIndicator.setBackgroundResource(R.drawable.bg_status_arrived)
                }
                SOSStatus.RESOLVED -> {
                    tvStatus.text = getString(R.string.alert_resolved)
                    tvStatusDesc.text = getString(R.string.you_are_safe)
                    // statusIndicator.setBackgroundResource(R.drawable.bg_status_resolved)
                }
                SOSStatus.CANCELLED, SOSStatus.FALSE_ALARM -> {
                    tvStatus.text = getString(R.string.alert_cancelled)
                    tvStatusDesc.text = getString(R.string.cancelled_by_you)
                    // statusIndicator.setBackgroundResource(R.drawable.bg_status_cancelled)
                }
            }
        }
    }

    private fun startElapsedTimer() {
        val updateRunnable = object : Runnable {
            override fun run() {
                elapsedSeconds++
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                binding.tvElapsedTime.text = String.format("%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(updateRunnable)
    }

    private fun startPulseAnimation() {
        val pulse = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        binding.statusIndicator.startAnimation(pulse)
    }

    private fun showCancelConfirmation() {
        showAlertDialog(
            title = getString(R.string.cancel_sos_title),
            message = getString(R.string.cancel_sos_message),
            positiveButton = getString(R.string.yes_cancel),
            negativeButton = getString(R.string.no_keep_active),
            onPositiveClick = { viewModel.cancelSOS() }
        )
    }

    private fun showMarkSafeConfirmation() {
        showAlertDialog(
            title = getString(R.string.mark_safe_title),
            message = getString(R.string.mark_safe_message),
            positiveButton = getString(R.string.yes_im_safe),
            negativeButton = getString(R.string.cancel),
            onPositiveClick = { viewModel.markAsSafe() }
        )
    }

    private fun showUpdateMessageDialog() {
        // Implementation for update message dialog
    }

    private fun navigateToMap() {
        // val action = SOSActiveFragmentDirections
        //    .actionSosActiveFragmentToTrackingMapFragment(args.sosId)
        // navController.navigate(action)
    }

    private fun callEmergencyServices() {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:112")
        }
        startActivity(intent)
    }

    private fun shareLocation() {
        val state = viewModel.uiState.value
        state.location?.let { location ->
            val shareText = getString(
                R.string.share_location_text,
                location.latitude,
                location.longitude
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_location)))
        }
    }
}
