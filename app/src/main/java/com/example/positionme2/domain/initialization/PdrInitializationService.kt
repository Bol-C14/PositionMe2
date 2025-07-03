package com.example.positionme2.domain.initialization

import com.example.positionme2.utils.CoordinateTransform

/**
 * Domain interface for PDR initialization strategies
 */
interface PdrInitializationStrategy {
    suspend fun initialize(): InitializationResult
    fun getStrategyName(): String
    fun getPriority(): Int
}

/**
 * Result of PDR initialization attempt
 */
data class InitializationResult(
    val success: Boolean,
    val position: CoordinateTransform.GpsCoordinate?,
    val accuracy: Float?,
    val strategyUsed: String,
    val message: String
)

/**
 * Domain service for managing PDR initialization
 */
interface PdrInitializationService {
    suspend fun initializePdr(): InitializationResult
    fun addStrategy(strategy: PdrInitializationStrategy)
    fun removeStrategy(strategy: PdrInitializationStrategy)
}
