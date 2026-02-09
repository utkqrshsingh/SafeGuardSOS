package com.safeguard.sos.presentation.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.databinding.FragmentTrackingMapBinding
import com.safeguard.sos.domain.model.HelperResponse
import com.safeguard.sos.domain.model.ResponseStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrackingMapFragment : BaseFragment<FragmentTrackingMapBinding>(), OnMapReadyCallback {

    private val viewModel: TrackingMapViewModel by viewModels()
    private val args: TrackingMapFragmentArgs by navArgs()

    private var googleMap: GoogleMap? = null
    private val helperMarkers = mutableMapOf<String, Marker>()
    private var userMarker: Marker? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTrackingMapBinding {
        return FragmentTrackingMapBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadSOSData(args.sosId)

        setupUI()
        setupMap()
        setupClickListeners()
        observeData()
    }

    private fun setupUI() {
        binding.apply {
            toolbar.setNavigationOnClickListener {
                findNavController().navigateUp()
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

        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMapToolbarEnabled = false
        }

        // Set dark map style
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

        // Update with current state
        viewModel.uiState.value.let { state ->
            state.userLocation?.let { location ->
                addUserMarker(LatLng(location.latitude, location.longitude))
            }
            updateHelperMarkers(state.respondingHelpers)
        }
    }

    override fun setupClickListeners() {
        binding.apply {
            fabRecenter.setOnClickListener {
                recenterMap()
            }

            // Helper card clicks
            cardNearestHelper.setOnClickListener {
                viewModel.uiState.value.nearestHelper?.let { helper ->
                    helper.location?.let { location ->
                        centerOnLocation(LatLng(location.latitude, location.longitude))
                    }
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
    }

    private fun updateUI(state: TrackingMapUiState) {
        binding.apply {
            progressBar.isVisible = state.isLoading

            // Update helpers count
            tvHelpersCount.text = getString(R.string.helpers_tracking, state.respondingHelpers.size)

            // Update nearest helper info
            state.nearestHelper?.let { helper ->
                cardNearestHelper.isVisible = true
                tvNearestHelperName.text = helper.helperName
                tvNearestHelperStatus.text = when (helper.status) {
                    ResponseStatus.RESPONDING -> getString(R.string.on_the_way)
                    ResponseStatus.ARRIVED -> getString(R.string.arrived)
                    else -> getString(R.string.responding)
                }
                helper.estimatedArrivalMinutes?.let { eta ->
                    tvNearestHelperEta.text = getString(R.string.eta_format, eta.toString())
                    tvNearestHelperEta.isVisible = true
                } ?: run {
                    tvNearestHelperEta.isVisible = false
                }

                // Set status color
                val statusColor = when (helper.status) {
                    ResponseStatus.ARRIVED -> R.color.success
                    else -> R.color.info
                }
                tvNearestHelperStatus.setTextColor(ContextCompat.getColor(requireContext(), statusColor))
            } ?: run {
                cardNearestHelper.isVisible = false
            }

            // Update map markers
            state.userLocation?.let { location ->
                addUserMarker(LatLng(location.latitude, location.longitude))
            }
            updateHelperMarkers(state.respondingHelpers)

            // Auto-fit all markers
            if (state.respondingHelpers.isNotEmpty() && state.userLocation != null) {
                fitAllMarkers(state)
            }
        }
    }

    private fun addUserMarker(position: LatLng) {
        googleMap?.let { map ->
            userMarker?.remove()
            userMarker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(getString(R.string.your_location))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }
    }

    private fun updateHelperMarkers(helpers: List<HelperResponse>) {
        googleMap?.let { map ->
            // Remove markers for helpers no longer responding
            val currentHelperIds = helpers.map { it.id }.toSet()
            helperMarkers.keys.filter { it !in currentHelperIds }.forEach { id ->
                helperMarkers[id]?.remove()
                helperMarkers.remove(id)
            }

            // Add or update helper markers
            helpers.forEach { helper ->
                helper.location?.let { location ->
                    val position = LatLng(location.latitude, location.longitude)

                    val hue = when (helper.status) {
                        ResponseStatus.ARRIVED -> BitmapDescriptorFactory.HUE_GREEN
                        else -> BitmapDescriptorFactory.HUE_AZURE
                    }

                    if (helperMarkers.containsKey(helper.id)) {
                        // Update existing marker position
                        helperMarkers[helper.id]?.position = position
                    } else {
                        // Add new marker
                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title(helper.helperName)
                                .snippet(getHelperSnippet(helper))
                                .icon(BitmapDescriptorFactory.defaultMarker(hue))
                        )
                        marker?.let { helperMarkers[helper.id] = it }
                    }
                }
            }
        }
    }

    private fun getHelperSnippet(helper: HelperResponse): String {
        val status = when (helper.status) {
            ResponseStatus.ARRIVED -> getString(R.string.arrived)
            else -> getString(R.string.on_the_way)
        }
        val eta = helper.estimatedArrivalMinutes?.let { " • ETA: $it" } ?: ""
        return "$status$eta"
    }

    private fun fitAllMarkers(state: TrackingMapUiState) {
        googleMap?.let { map ->
            val boundsBuilder = LatLngBounds.Builder()

            state.userLocation?.let { location ->
                boundsBuilder.include(LatLng(location.latitude, location.longitude))
            }

            state.respondingHelpers.forEach { helper ->
                helper.location?.let { location ->
                    boundsBuilder.include(LatLng(location.latitude, location.longitude))
                }
            }

            try {
                val bounds = boundsBuilder.build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (e: Exception) {
                // Not enough points
            }
        }
    }

    private fun recenterMap() {
        viewModel.uiState.value.userLocation?.let { location ->
            centerOnLocation(LatLng(location.latitude, location.longitude))
        }
    }

    private fun centerOnLocation(position: LatLng) {
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
    }
}
