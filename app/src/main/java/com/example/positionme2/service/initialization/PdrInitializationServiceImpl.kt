package com.example.positionme2.service.initialization

import com.example.positionme2.domain.initialization.InitializationResult
import com.example.positionme2.domain.initialization.PdrInitializationService
import com.example.positionme2.domain.initialization.PdrInitializationStrategy
import com.example.positionme2.domain.pdr.PdrProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PDR initialization service using strategy pattern
 */
@Singleton
class PdrInitializationServiceImpl @Inject constructor(
    private val pdrProcessor: PdrProcessor,
    strategies: Set<@JvmSuppressWildcards PdrInitializationStrategy>
) : PdrInitializationService {

    private val strategies = strategies.toMutableSet()

    override suspend fun initializePdr(): InitializationResult = withContext(Dispatchers.IO) {
        // Sort strategies by priority (lowest number = highest priority)
        val sortedStrategies = strategies.sortedBy { it.getPriority() }

        for (strategy in sortedStrategies) {
            val result = strategy.initialize()
            if (result.success && result.position != null) {
                // Initialize PDR with the successful result
                pdrProcessor.initializeWithGps(result.position)
                return@withContext result
            }
        }

        // If all strategies fail, return failure result
        InitializationResult(
            success = false,
            position = null,
            accuracy = null,
            strategyUsed = "None",
            message = "All initialization strategies failed"
        )
    }

    override fun addStrategy(strategy: PdrInitializationStrategy) {
        strategies.add(strategy)
    }

    override fun removeStrategy(strategy: PdrInitializationStrategy) {
        strategies.remove(strategy)
    }
}
