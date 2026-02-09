package com.safeguard.sos.presentation.helper

import android.content.Intent
import android.net.Uri
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
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.databinding.FragmentAlertDetailBinding
import com.safeguard.sos.domain.model.SOSStatus
import com.safeguard.sos.domain.model.AlertType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlertDetailFragment : BaseFragment<FragmentAlertDetailBinding>(), OnMapReadyCallback {

    private val viewModel: AlertDetailViewModel by viewModels()
    private val args: AlertDetailFragmentArgs by navArgs()

    private var googleMap: GoogleMap? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAlertDetailBinding {
        return FragmentAlertDetailBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        viewModel.loadAlertDetail(args.sosId)
        setupMap()
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isMyLocationButtonEnabled = false
            isScrollGesturesEnabled = false
            isZoomGesturesEnabled = false
        }

        // Update map with current location if available
        viewModel.uiState.value.alert?.location?.let { location ->
            updateMapLocation(location.latitude, location.longitude)
        }
    }

    private fun updateMapLocation(latitude: Double, longitude: Double) {
        googleMap?.let { map ->
            val position = LatLng(latitude, longitude)

            map.clear()
            map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(getString(R.string.victim_location))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )

            map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
        }
    }

    override fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                navController.navigateUp()
            }

            // Respond button
            btnRespond.setOnClickListener {
                viewModel.respondToAlert()
            }

            // Navigate button
            btnNavigate.setOnClickListener {
                navigateToVictim()
            }

            // Call victim
            btnCall.setOnClickListener {
                viewModel.uiState.value.alert?.userPhone?.let { phone ->
                    callPhone(phone)
                }
            }

            // Message victim
            btnMessage.setOnClickListener {
                viewModel.uiState.value.alert?.userPhone?.let { phone ->
                    messagePhone(phone)
                }
            }

            // Arrived button
            btnArrived.setOnClickListener {
                showArrivedConfirmation()
            }

            // Cancel response
            btnCancelResponse.setOnClickListener {
                showCancelConfirmation()
            }

            // Open full map
            cardMap.setOnClickListener {
                navigateToFullMap()
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

    private fun updateUI(state: AlertDetailUiState) {
        binding.apply {
            progressBar.isVisible = state.isLoading
            contentContainer.isVisible = !state.isLoading && state.alert != null

            state.alert?.let { alert ->
                // User info
                tvUserName.text = alert.userName
                tvUserInitials.text = alert.userName.split(" ")
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")

                // Emergency type
                tvEmergencyType.text = alert.alertType.name

                // Emergency type icon and color
                when (alert.alertType) {
                    AlertType.MEDICAL -> {
                        ivEmergencyIcon.setImageResource(android.R.drawable.ic_menu_my_calendar) // Placeholder
                        cardEmergencyType.setCardBackgroundColor(
                            requireContext().getColor(android.R.color.holo_red_light)
                        )
                    }
                    AlertType.EMERGENCY -> {
                        ivEmergencyIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                        cardEmergencyType.setCardBackgroundColor(
                            requireContext().getColor(android.R.color.holo_orange_light)
                        )
                    }
                    else -> {
                        ivEmergencyIcon.setImageResource(android.R.drawable.ic_dialog_info)
                        cardEmergencyType.setCardBackgroundColor(
                            requireContext().getColor(android.R.color.holo_blue_light)
                        )
                    }
                }

                // Status
                updateStatusUI(alert.status)

                // Time ago
                tvTimeAgo.text = formatTimeAgo(alert.createdAt)

                // Distance (Mocked or passed via args if available)
                // In a real app, distance would be calculated between current user and victim
                tvDistance.isVisible = false

                // Message
                if (!alert.message.isNullOrBlank()) {
                    cardMessage.isVisible = true
                    tvMessage.text = alert.message
                } else {
                    cardMessage.isVisible = false
                }

                // Location
                tvAddress.text = alert.location.displayAddress

                // Update map
                updateMapLocation(alert.location.latitude, alert.location.longitude)

                // Button states based on current response status
                when {
                    state.isResponding -> {
                        btnRespond.isVisible = false
                        btnNavigate.isVisible = true
                        btnArrived.isVisible = true
                        btnCancelResponse.isVisible = true
                        respondingBanner.isVisible = true
                    }
                    state.hasArrived -> {
                        btnRespond.isVisible = false
                        btnNavigate.isVisible = false
                        btnArrived.isVisible = false
                        btnCancelResponse.isVisible = false
                        arrivedBanner.isVisible = true
                        respondingBanner.isVisible = false
                    }
                    else -> {
                        btnRespond.isVisible = true
                        btnNavigate.isVisible = false
                        btnArrived.isVisible = false
                        btnCancelResponse.isVisible = false
                        respondingBanner.isVisible = false
                        arrivedBanner.isVisible = false
                    }
                }

                // Other responders
                if (alert.respondersCount > 0) {
                    cardOtherResponders.isVisible = true
                    tvRespondersCount.text = getString(
                        R.string.other_responders_count,
                        alert.respondersCount
                    )
                } else {
                    cardOtherResponders.isVisible = false
                }
            }
        }
    }

    private fun updateStatusUI(status: SOSStatus) {
        binding.apply {
            when (status) {
                SOSStatus.ACTIVE, SOSStatus.PENDING -> {
                    tvStatus.text = getString(R.string.needs_help)
                    tvStatus.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                    // statusDot.setBackgroundResource(R.drawable.bg_status_dot_urgent)
                }
                SOSStatus.HELP_ON_WAY -> {
                    tvStatus.text = getString(R.string.help_coming)
                    tvStatus.setTextColor(requireContext().getColor(android.R.color.holo_orange_dark))
                    // statusDot.setBackgroundResource(R.drawable.bg_status_dot_busy)
                }
                SOSStatus.RESPONDED -> {
                    tvStatus.text = getString(R.string.helper_there)
                    tvStatus.setTextColor(requireContext().getColor(android.R.color.holo_blue_dark))
                    // statusDot.setBackgroundResource(R.drawable.bg_status_dot_online)
                }
                SOSStatus.RESOLVED -> {
                    tvStatus.text = getString(R.string.resolved)
                    tvStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
                    // statusDot.setBackgroundResource(R.drawable.bg_status_dot_online)
                }
                SOSStatus.CANCELLED, SOSStatus.FALSE_ALARM -> {
                    tvStatus.text = getString(R.string.cancelled)
                    tvStatus.setTextColor(requireContext().getColor(android.R.color.darker_gray))
                    // statusDot.setBackgroundResource(R.drawable.bg_status_dot_offline)
                }
            }
        }
    }

    private fun navigateToVictim() {
        val alert = viewModel.uiState.value.alert ?: return
        try {
           val action = AlertDetailFragmentDirections.actionAlertDetailToNavigation(
               sosId = alert.id,
               destinationLat = alert.location.latitude.toFloat(),
               destinationLng = alert.location.longitude.toFloat()
           )
           navController.navigate(action)
        } catch (e: Exception) {}
    }

    private fun navigateToFullMap() {
        val alert = viewModel.uiState.value.alert ?: return

        // Open in external maps app
        val uri = Uri.parse("geo:${alert.location.latitude},${alert.location.longitude}?q=${alert.location.latitude},${alert.location.longitude}(${alert.userName})")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback to browser
            val browserUri = Uri.parse("https://www.google.com/maps?q=${alert.location.latitude},${alert.location.longitude}")
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

    private fun callPhone(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
        }
        startActivity(intent)
    }

    private fun messagePhone(phone: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            putExtra("sms_body", getString(R.string.helper_message_template, "10 min"))
        }
        startActivity(intent)
    }

    private fun showArrivedConfirmation() {
        showAlertDialog(
            title = getString(R.string.confirm_arrival),
            message = getString(R.string.confirm_arrival_message),
            positiveButton = getString(R.string.yes_arrived),
            negativeButton = getString(R.string.cancel),
            onPositiveClick = { viewModel.markAsArrived() }
        )
    }

    private fun showCancelConfirmation() {
        showAlertDialog(
            title = getString(R.string.cancel_response),
            message = getString(R.string.cancel_response_message),
            positiveButton = getString(R.string.yes_cancel),
            negativeButton = getString(R.string.no),
            onPositiveClick = { viewModel.cancelResponse() }
        )
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> getString(R.string.just_now)
            diff < 3600_000 -> getString(R.string.minutes_ago, diff / 60_000)
            diff < 86400_000 -> getString(R.string.hours_ago, diff / 3600_000)
            else -> getString(R.string.days_ago, diff / 86400_000)
        }
    }
}
