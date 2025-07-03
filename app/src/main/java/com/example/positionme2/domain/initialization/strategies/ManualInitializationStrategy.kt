package com.example.positionme2.domain.initialization.strategies

import com.example.positionme2.domain.initialization.InitializationResult
import com.example.positionme2.domain.initialization.PdrInitializationStrategy
import com.example.positionme2.utils.CoordinateTransform
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManualInitializationStrategy @Inject constructor() : PdrInitializationStrategy {
    private var manualPosition: CoordinateTransform.GpsCoordinate? = null

    fun setManualPosition(latitude: Double, longitude: Double, altitude: Double = 0.0) {
        manualPosition = CoordinateTransform.GpsCoordinate(latitude, longitude, altitude)
    }

    override suspend fun initialize(): InitializationResult {
        return manualPosition?.let { position ->
            InitializationResult(
                success = true,
                position = position,
                accuracy = null,
                strategyUsed = getStrategyName(),
                message = "Position set manually to (${position.latitude}, ${position.longitude})"
            )
        } ?: InitializationResult(
            success = false,
            position = null,
            accuracy = null,
            strategyUsed = getStrategyName(),
            message = "Manual position not set"
        )
    }

    override fun getStrategyName(): String = "Manual"
    override fun getPriority(): Int = 10 // Lowest priority
}

