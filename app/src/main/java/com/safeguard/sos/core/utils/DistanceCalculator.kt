// app/src/main/java/com/safeguard/sos/core/utils/DistanceCalculator.kt

package com.safeguard.sos.core.utils

import android.location.Location
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility class for calculating distances between geographic coordinates
 */
object DistanceCalculator {

    private const val EARTH_RADIUS_KM = 6371.0
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculate distance between two points using Haversine formula
     * @return Distance in kilometers
     */
    fun calculateDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * Calculate distance between two points
     * @return Distance in meters
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        return calculateDistanceKm(lat1, lon1, lat2, lon2) * 1000
    }

    /**
     * Calculate distance using Android Location class
     * @return Distance in meters
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * Calculate distance between two Location objects
     * @return Distance in meters
     */
    fun calculateDistance(location1: Location, location2: Location): Float {
        return location1.distanceTo(location2)
    }

    /**
     * Calculate bearing between two points
     * @return Bearing in degrees (0-360)
     */
    fun calculateBearing(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)

        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)

        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360

        return bearing
    }

    /**
     * Check if a point is within a given radius of another point
     */
    fun isWithinRadius(
        centerLat: Double,
        centerLon: Double,
        pointLat: Double,
        pointLon: Double,
        radiusKm: Double
    ): Boolean {
        return calculateDistanceKm(centerLat, centerLon, pointLat, pointLon) <= radiusKm
    }

    /**
     * Format distance for display
     */
    fun formatDistance(distanceMeters: Float): String {
        return when {
            distanceMeters < 1000 -> "${distanceMeters.toInt()} m"
            else -> String.format("%.1f km", distanceMeters / 1000)
        }
    }

    /**
     * Format distance for display
     */
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 1 -> "${(distanceKm * 1000).toInt()} m"
            else -> String.format("%.1f km", distanceKm)
        }
    }

    /**
     * Calculate estimated time of arrival
     * @param distanceMeters Distance in meters
     * @param speedKmh Speed in km/h (default: 30 km/h for city traffic)
     * @return ETA in minutes
     */
    fun calculateETA(distanceMeters: Float, speedKmh: Double = 30.0): Int {
        val distanceKm = distanceMeters / 1000
        val timeHours = distanceKm / speedKmh
        return (timeHours * 60).toInt().coerceAtLeast(1)
    }

    /**
     * Format ETA for display
     */
    fun formatETA(minutes: Int): String {
        return when {
            minutes < 1 -> "< 1 min"
            minutes == 1 -> "1 min"
            minutes < 60 -> "$minutes mins"
            else -> {
                val hours = minutes / 60
                val remainingMins = minutes % 60
                if (remainingMins == 0) "$hours hr" else "$hours hr $remainingMins min"
            }
        }
    }

    /**
     * Calculate bounding box for a given center and radius
     */
    fun calculateBoundingBox(
        centerLat: Double,
        centerLon: Double,
        radiusKm: Double
    ): BoundingBox {
        val latDelta = radiusKm / 111.0 // 1 degree latitude ≈ 111 km
        val lonDelta = radiusKm / (111.0 * cos(Math.toRadians(centerLat)))

        return BoundingBox(
            minLat = centerLat - latDelta,
            maxLat = centerLat + latDelta,
            minLon = centerLon - lonDelta,
            maxLon = centerLon + lonDelta
        )
    }

    /**
     * Get direction text from bearing
     */
    fun getDirectionFromBearing(bearing: Double): String {
        return when {
            bearing >= 337.5 || bearing < 22.5 -> "N"
            bearing >= 22.5 && bearing < 67.5 -> "NE"
            bearing >= 67.5 && bearing < 112.5 -> "E"
            bearing >= 112.5 && bearing < 157.5 -> "SE"
            bearing >= 157.5 && bearing < 202.5 -> "S"
            bearing >= 202.5 && bearing < 247.5 -> "SW"
            bearing >= 247.5 && bearing < 292.5 -> "W"
            bearing >= 292.5 && bearing < 337.5 -> "NW"
            else -> ""
        }
    }

    data class BoundingBox(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    )
}