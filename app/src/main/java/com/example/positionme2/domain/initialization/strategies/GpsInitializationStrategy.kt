package com.example.positionme2.domain.initialization.strategies

import com.example.positionme2.domain.initialization.InitializationResult
import com.example.positionme2.domain.initialization.PdrInitializationStrategy
import com.example.positionme2.service.location.GpsLocationProvider
import javax.inject.Inject

/**
 * GPS-based initialization strategy
 */
class GpsInitializationStrategy @Inject constructor(
    private val gpsLocationProvider: GpsLocationProvider
) : PdrInitializationStrategy {

    companion object {
        private const val HIGH_ACCURACY_THRESHOLD = 5.0f
        private const val GOOD_ACCURACY_THRESHOLD = 10.0f
        private const val ACCEPTABLE_ACCURACY_THRESHOLD = 20.0f
        private const val GPS_TIMEOUT_MS = 15000L
    }

    override suspend fun initialize(): InitializationResult {
        return try {
            val locationResult = gpsLocationProvider.waitForAccurateLocation(
                maxAccuracyMeters = ACCEPTABLE_ACCURACY_THRESHOLD,
                timeoutMs = GPS_TIMEOUT_MS
            )

            if (locationResult.isSuccess) {
                val location = locationResult.getOrThrow()
                val accuracy = gpsLocationProvider.locationAccuracy.value ?: Float.MAX_VALUE
                InitializationResult(
                    success = true,
                    position = location,
                    accuracy = accuracy,
                    strategyUsed = getStrategyName(),
                    message = "GPS initialized with ${accuracy}m accuracy"
                )
            } else {
                InitializationResult(
                    success = false,
                    position = null,
                    accuracy = null,
                    strategyUsed = getStrategyName(),
                    message = "GPS initialization failed: ${locationResult.exceptionOrNull()?.message}"
                )
            }
        } catch (e: Exception) {
            InitializationResult(
                success = false,
                position = null,
                accuracy = null,
                strategyUsed = getStrategyName(),
                message = "GPS initialization error: ${e.message}"
            )
        }
    }

    override fun getStrategyName(): String = "GPS"

    override fun getPriority(): Int = 1 // Highest priority
}
