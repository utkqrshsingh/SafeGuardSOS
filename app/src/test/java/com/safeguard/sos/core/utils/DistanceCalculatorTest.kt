package com.safeguard.sos.core.utils

import org.junit.Assert.*
import org.junit.Test

class DistanceCalculatorTest {

    @Test
    fun `calculateDistanceKm returns 0 for same location`() {
        val distance = DistanceCalculator.calculateDistanceKm(
            12.9716, 77.5946,
            12.9716, 77.5946
        )
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `calculateDistanceKm returns correct distance between Bangalore and Chennai`() {
        // Bangalore: 12.9716, 77.5946
        // Chennai: 13.0827, 80.2707
        val distance = DistanceCalculator.calculateDistanceKm(
            12.9716, 77.5946,
            13.0827, 80.2707
        )
        // Approximate distance is ~290 km
        assertTrue("Distance $distance should be between 280 and 300", distance > 280 && distance < 300)
    }

    @Test
    fun `calculateDistanceKm returns correct distance for short distances`() {
        // Two points ~1km apart
        val distance = DistanceCalculator.calculateDistanceKm(
            12.9716, 77.5946,
            12.9806, 77.5946  // ~1km north
        )
        assertTrue("Distance $distance should be around 1.0 km", distance > 0.9 && distance < 1.1)
    }

    @Test
    fun `isWithinRadius returns true when within radius`() {
        val isWithin = DistanceCalculator.isWithinRadius(
            12.9716, 77.5946,
            12.9720, 77.5950,
            1.0  // 1 km radius
        )
        assertTrue(isWithin)
    }

    @Test
    fun `isWithinRadius returns false when outside radius`() {
        val isWithin = DistanceCalculator.isWithinRadius(
            12.9716, 77.5946,
            13.0827, 80.2707,  // Chennai
            10.0  // 10 km radius
        )
        assertFalse(isWithin)
    }

    @Test
    fun `formatDistance returns meters for short distances`() {
        val formatted = DistanceCalculator.formatDistance(0.5) // 0.5 km
        assertTrue(formatted.contains("m") && !formatted.contains("km"))
        assertEquals("500 m", formatted)
    }

    @Test
    fun `formatDistance returns km for longer distances`() {
        val formatted = DistanceCalculator.formatDistance(5.5) // 5.5 km
        assertTrue(formatted.contains("km"))
        assertEquals("5.5 km", formatted)
    }

    @Test
    fun `calculateBearing returns correct bearing`() {
        // North should be close to 0 degrees
        val bearingNorth = DistanceCalculator.calculateBearing(
            12.9716, 77.5946,
            13.9716, 77.5946  // Due north
        )
        assertTrue("Bearing $bearingNorth should be near 0 or 360", bearingNorth < 5 || bearingNorth > 355)

        // East should be close to 90 degrees
        val bearingEast = DistanceCalculator.calculateBearing(
            12.9716, 77.5946,
            12.9716, 78.5946  // Due east
        )
        assertTrue("Bearing $bearingEast should be around 90", bearingEast > 85 && bearingEast < 95)
    }
}