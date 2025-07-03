package com.example.positionme2.domain.location

import com.example.positionme2.utils.CoordinateTransform

/**
 * Domain interface for location services
 */
interface LocationProvider {
    suspend fun getCurrentLocation(): Result<CoordinateTransform.GpsCoordinate>
    suspend fun waitForAccurateLocation(
        maxAccuracyMeters: Float = 20.0f,
        timeoutMs: Long = 15000L
    ): Result<CoordinateTransform.GpsCoordinate>

    fun startLocationUpdates(): Boolean
    fun stopLocationUpdates()
    fun isLocationEnabled(): Boolean
}
