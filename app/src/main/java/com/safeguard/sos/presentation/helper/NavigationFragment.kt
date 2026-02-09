package com.safeguard.sos.presentation.helper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.databinding.FragmentNavigationBinding
import com.safeguard.sos.domain.model.HelperStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NavigationFragment : BaseFragment<FragmentNavigationBinding>(), OnMapReadyCallback {

    private val viewModel: NavigationViewModel by viewModels()
    private val args: NavigationFragmentArgs by navArgs()

    private var googleMap: GoogleMap? = null
    private var isMapReady = false

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNavigationBinding {
        return FragmentNavigationBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        viewModel.initialize(args.sosId, args.destinationLat.toDouble(), args.destinationLng.toDouble())

        setupUI()
        setupMap()
    }

    override fun onResume() {
        super.onResume()
        viewModel.startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopLocationUpdates()
    }

    private fun setupUI() {
        binding.apply {
            toolbar.setNavigationOnClickListener {
                showExitConfirmation()
            }
        }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true

        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = false
            isMapToolbarEnabled = false
        }

        // Set map style for dark mode
        try {
            map.setMapStyle(
                com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style_dark
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Enable my location if permission granted
        if (hasLocationPermission()) {
            try {
                map.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        // Update map with current state
        viewModel.uiState.value.let { state ->
            if (state.destinationLocation != null) {
                updateMapMarkers(state)
            }
        }
    }

    override fun setupClickListeners() {
        binding.apply {
            // Open in Google Maps
            btnOpenMaps.setOnClickListener {
                openInGoogleMaps()
            }

            // Call user
            btnCallUser.setOnClickListener {
                viewModel.uiState.value.userPhone?.let { phone ->
                    callUser(phone)
                }
            }

            // Message user
            btnMessageUser.setOnClickListener {
                viewModel.uiState.value.userPhone?.let { phone ->
                    messageUser(phone)
                }
            }

            // Mark as arrived
            btnMarkArrived.setOnClickListener {
                viewModel.markAsArrived()
            }

            // Cancel response
            btnCancelResponse.setOnClickListener {
                showCancelConfirmation()
            }

            // Re-center map
            fabRecenter.setOnClickListener {
                recenterMap()
            }

            // Toggle traffic
            fabTraffic.setOnClickListener {
                toggleTraffic()
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
                    when (event) {
                        is UiEvent.Success -> {
                            showToast(getString(R.string.status_updated))
                        }
                        is UiEvent.Error -> {
                            showToast(event.message)
                        }
                        is UiEvent.NavigateBack -> {
                            findNavController().navigateUp()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateUI(state: NavigationUiState) {
        binding.apply {
            // Loading state
            progressBar.isVisible = state.isLoading

            // User info
            tvUserName.text = state.userName
            tvEmergencyType.text = state.emergencyType?.replaceFirstChar { it.uppercase() }

            // Distance and ETA
            state.distance?.let { distance ->
                tvDistance.text = formatDistance(distance)
            }
            state.eta?.let { eta ->
                tvEta.text = eta
            }

            // Status
            updateStatusUI(state.status)

            // Update map
            if (isMapReady && state.currentLocation != null && state.destinationLocation != null) {
                updateMapMarkers(state)
                state.routePoints?.let { points ->
                    drawRoute(points)
                }
            }

            // Button states
            btnMarkArrived.isVisible = state.status == HelperStatus.RESPONDING
            btnCancelResponse.isVisible = state.status != HelperStatus.OFFLINE
        }
    }

    private fun updateStatusUI(status: HelperStatus) {
        binding.apply {
            when (status) {
                HelperStatus.RESPONDING -> {
                    tvStatus.text = getString(R.string.navigating)
                    tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.info))
                    statusIndicator.setColorFilter(ContextCompat.getColor(requireContext(), R.color.info))
                    cardStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.info_alpha_10))
                }
                HelperStatus.BUSY -> {
                    tvStatus.text = getString(R.string.arrived)
                    tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
                    statusIndicator.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
                    cardStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success_alpha_10))
                }
                HelperStatus.OFFLINE -> {
                    tvStatus.text = getString(R.string.offline)
                    tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
                    statusIndicator.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_hint))
                    cardStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_elevated))
                }
                else -> {
                    tvStatus.text = status.name
                }
            }
        }
    }

    private fun updateMapMarkers(state: NavigationUiState) {
        googleMap?.let { map ->
            map.clear()

            // Destination marker (SOS location)
            state.destinationLocation?.let { dest ->
                val destLatLng = LatLng(dest.latitude, dest.longitude)
                map.addMarker(
                    MarkerOptions()
                        .position(destLatLng)
                        .title(state.userName ?: getString(R.string.sos_location))
                        .snippet(state.emergencyType)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
            }

            // Current location marker
            state.currentLocation?.let { current ->
                val currentLatLng = LatLng(current.latitude, current.longitude)
                map.addMarker(
                    MarkerOptions()
                        .position(currentLatLng)
                        .title(getString(R.string.your_location))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )

                // Fit both markers in view
                state.destinationLocation?.let { dest ->
                    val destLatLng = LatLng(dest.latitude, dest.longitude)
                    val bounds = LatLngBounds.Builder()
                        .include(currentLatLng)
                        .include(destLatLng)
                        .build()

                    try {
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun drawRoute(routePoints: List<LatLng>) {
        googleMap?.let { map ->
            val polylineOptions = PolylineOptions()
                .addAll(routePoints)
                .width(12f)
                .color(ContextCompat.getColor(requireContext(), R.color.accent_cyan))
                .geodesic(true)

            map.addPolyline(polylineOptions)
        }
    }

    private fun recenterMap() {
        val state = viewModel.uiState.value

        googleMap?.let { map ->
            state.currentLocation?.let { current ->
                val currentLatLng = LatLng(current.latitude, current.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }

    private fun toggleTraffic() {
        googleMap?.let { map ->
            map.isTrafficEnabled = !map.isTrafficEnabled

            binding.fabTraffic.setImageResource(
                if (map.isTrafficEnabled) R.drawable.ic_traffic_on else R.drawable.ic_traffic_off
            )
        }
    }

    private fun openInGoogleMaps() {
        val state = viewModel.uiState.value
        val destination = state.destinationLocation ?: return

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("google.navigation:q=${destination.latitude},${destination.longitude}&mode=d")
            setPackage("com.google.android.apps.maps")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Google Maps not installed, open in browser
            val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&destination=${destination.latitude},${destination.longitude}"
                )
            }
            startActivity(browserIntent)
        }
    }

    private fun callUser(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
        }
        startActivity(intent)
    }

    private fun messageUser(phone: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("smsto:$phone")
        }
        startActivity(intent)
    }

    private fun showExitConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.exit_navigation)
            .setMessage(R.string.exit_navigation_message)
            .setPositiveButton(R.string.yes_exit) { _, _ ->
                findNavController().navigateUp()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCancelConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.cancel_response)
            .setMessage(R.string.cancel_response_message)
            .setPositiveButton(R.string.yes_cancel) { _, _ ->
                viewModel.cancelResponse()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun formatDistance(meters: Float): String {
        return if (meters < 1000) {
            "${meters.toInt()} m"
        } else {
            String.format("%.1f km", meters / 1000f)
        }
    }
}
