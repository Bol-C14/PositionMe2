package com.example.positionme2.service

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.positionme2.domain.initialization.InitializationResult
import com.example.positionme2.domain.initialization.PdrInitializationService
import com.example.positionme2.domain.initialization.strategies.ManualInitializationStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified facade for PDR initialization that provides a clean API for UI layer
 */
@Singleton
class PdrInitializationFacade @Inject constructor(
    private val pdrInitializationService: PdrInitializationService,
    private val manualInitializationStrategy: ManualInitializationStrategy
) {

    private val _initializationStatus = MutableLiveData<InitializationResult>()
    val initializationStatus: LiveData<InitializationResult> get() = _initializationStatus

    private val _isInitializing = MutableLiveData(false)
    val isInitializing: LiveData<Boolean> get() = _isInitializing

    private var initializationJob: Job? = null

    /**
     * Start PDR initialization using available strategies
     */
    fun initializePdr() {
        if (_isInitializing.value == true) {
            return
        }

        _isInitializing.postValue(true)

        initializationJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = pdrInitializationService.initializePdr()
                _initializationStatus.postValue(result)
            } catch (e: Exception) {
                _initializationStatus.postValue(
                    InitializationResult(
                        success = false,
                        position = null,
                        accuracy = null,
                        strategyUsed = "Error",
                        message = "Initialization failed: ${e.message}"
                    )
                )
            } finally {
                _isInitializing.postValue(false)
            }
        }
    }

    /**
     * Set manual position for initialization
     */
    fun setManualPosition(latitude: Double, longitude: Double, altitude: Double = 0.0) {
        manualInitializationStrategy.setManualPosition(latitude, longitude, altitude)
    }

    /**
     * Cancel ongoing initialization
     */
    fun cancelInitialization() {
        initializationJob?.cancel()
        _isInitializing.postValue(false)
    }

    /**
     * Get simple status message for UI
     */
    fun getStatusMessage(): String {
        return when {
            _isInitializing.value == true -> "Searching for your location..."
            _initializationStatus.value?.success == true -> "PDR ready and tracking your position."
            else -> "PDR not initialized. Please start initialization."
        }
    }
}
