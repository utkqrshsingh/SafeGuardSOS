package com.safeguard.sos.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) : LocationRepository {

    private val _isTracking = MutableStateFlow(false)
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Resource<Location> {
        if (!hasLocationPermission()) {
            return Resource.Error("Location permission not granted")
        }

        return try {
            val location = suspendCancellableCoroutine<android.location.Location> { continuation ->
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            continuation.resume(loc)
                        } else {
                            continuation.resumeWithException(Exception("Location not available"))
                        }
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
                    .addOnCanceledListener {
                        continuation.cancel()
                    }
            }
            val address = getAddressFromLocationInternal(location.latitude, location.longitude)
            Resource.Success(
                Location(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = address,
                    accuracy = location.accuracy,
                    timestamp = location.time
                )
            )
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get location")
        }
    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close(Exception("Location permission not granted"))
            return@callbackFlow
        }

        startLocationTracking()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setMinUpdateIntervalMillis(5000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(
                        Location(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            timestamp = location.time
                        )
                    )
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())

        awaitClose {
            stopLocationTracking()
        }
    }


    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }
        return try {
            val location = suspendCancellableCoroutine<android.location.Location?> { continuation ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { loc ->
                        continuation.resume(loc)
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
            }
            location?.let {
                Location(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy,
                    timestamp = it.time
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getAddressFromLocation(latitude: Double, longitude: Double): Resource<String> {
        return try {
            val address = getAddressFromLocationInternal(latitude, longitude)
            Resource.Success(address)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get address")
        }
    }

    @SuppressLint("MissingPermission")
    override fun startLocationTracking() {
        if (!hasLocationPermission()) return
        _isTracking.value = true
    }

    override fun stopLocationTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        _isTracking.value = false
        locationCallback = null
    }

    override fun isLocationTrackingActive(): Flow<Boolean> {
        return _isTracking.asStateFlow()
    }

    override suspend fun saveLocation(location: Location) {
        // TODO: Implement saving location to a local database if needed
    }

    override fun calculateDistance(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[0]
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getAddressFromLocationInternal(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                buildString {
                    address.thoroughfare?.let { append(it) }
                    address.subLocality?.let {
                        if (isNotEmpty()) append(", ")
                        append(it)
                    }
                    address.locality?.let {
                        if (isNotEmpty()) append(", ")
                        append(it)
                    }
                    address.adminArea?.let {
                        if (isNotEmpty()) append(", ")
                        append(it)
                    }
                    address.postalCode?.let {
                        if (isNotEmpty()) append(" - ")
                        append(it)
                    }
                }.ifEmpty { "Location: $latitude, $longitude" }
            } else {
                "Location: $latitude, $longitude"
            }
        } catch (e: Exception) {
            "Location: $latitude, $longitude"
        }
    }
}