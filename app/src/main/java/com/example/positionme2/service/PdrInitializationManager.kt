package com.example.positionme2.service

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.positionme2.domain.pdr.PdrProcessor
import com.example.positionme2.utils.CoordinateTransform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlin.coroutines.suspendCoroutine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages PDR initialization using multiple strategies to find the best starting position
 */
@Singleton
class PdrInitializationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationService: LocationService,
    private val pdrProcessor: PdrProcessor
) {

    companion object {
        private const val TAG = "PdrInitManager"
        private const val GPS_TIMEOUT_MS = 15000L // 15 seconds
        private const val GPS_HIGH_ACCURACY_THRESHOLD = 5.0f // 5 meters
        private const val GPS_GOOD_ACCURACY_THRESHOLD = 10.0f // 10 meters
        private const val GPS_ACCEPTABLE_ACCURACY_THRESHOLD = 20.0f // 20 meters
    }

    data class InitializationResult(
        val success: Boolean,
        val method: InitializationMethod,
        val position: CoordinateTransform.GpsCoordinate?,
        val accuracy: Float?,
        val message: String
    )

    enum class InitializationMethod {
        HIGH_ACCURACY_GPS,    // GPS with <5m accuracy
        GOOD_GPS,            // GPS with 5-10m accuracy
        ACCEPTABLE_GPS,      // GPS with 10-20m accuracy
        LAST_KNOWN_LOCATION, // Best available last known location
        WIFI_BASED,          // WiFi fingerprinting (if available)
        MANUAL_INPUT,        // User manually sets position
        DEFAULT_FALLBACK     // Default to (0,0) with warning
    }

    private val _initializationStatus = MutableLiveData<InitializationResult>()
    val initializationStatus: LiveData<InitializationResult> get() = _initializationStatus

    private val _isInitializing = MutableLiveData(false)
    val isInitializing: LiveData<Boolean> get() = _isInitializing

    private var initializationJob: Job? = null

    /**
     * Initialize PDR with the best available method
     */
    fun initializePdr() {
        if (_isInitializing.value == true) {
            Log.w(TAG, "Initialization already in progress")
            return
        }

        _isInitializing.postValue(true)

        initializationJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = findBestStartingPosition()
                handleInitializationResult(result)
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed", e)
                val fallbackResult = InitializationResult(
                    success = false,
                    method = InitializationMethod.DEFAULT_FALLBACK,
                    position = null,
                    accuracy = null,
                    message = "Initialization failed: ${e.message}"
                )
                handleInitializationResult(fallbackResult)
            } finally {
                _isInitializing.postValue(false)
            }
        }
    }

    /**
     * Try different methods to find the best starting position
     */
    private suspend fun findBestStartingPosition(): InitializationResult {
        Log.d(TAG, "Starting PDR initialization process...")

        // Method 1: Try to get high-accuracy GPS
        val gpsResult = tryGpsInitialization()
        if (gpsResult.success && gpsResult.accuracy != null && gpsResult.accuracy <= GPS_HIGH_ACCURACY_THRESHOLD) {
            Log.d(TAG, "High accuracy GPS available: ${gpsResult.accuracy}m")
            return gpsResult
        }

        // Method 2: Try last known location
        val lastKnownResult = tryLastKnownLocation()
        if (lastKnownResult.success) {
            Log.d(TAG, "Using last known location")
            return lastKnownResult
        }

        // Method 3: Wait a bit longer for GPS if we got a poor fix
        if (gpsResult.success && gpsResult.accuracy != null && gpsResult.accuracy <= GPS_ACCEPTABLE_ACCURACY_THRESHOLD) {
            Log.d(TAG, "Using acceptable GPS accuracy: ${gpsResult.accuracy}m")
            return gpsResult
        }

        // Method 4: WiFi-based positioning (placeholder for future implementation)
        val wifiResult = tryWifiBasedPositioning()
        if (wifiResult.success) {
            Log.d(TAG, "Using WiFi-based positioning")
            return wifiResult
        }

        // Method 5: Request manual input from user
        return requestManualInput()
    }

    /**
     * Try GPS-based initialization with timeout
     */
    private suspend fun tryGpsInitialization(): InitializationResult {
        return withTimeoutOrNull(GPS_TIMEOUT_MS) {
            suspendCoroutine<InitializationResult> { continuation ->
                var resumed = false
                var locationObserver: androidx.lifecycle.Observer<CoordinateTransform.GpsCoordinate>? = null

                // Start location updates
                if (!locationService.startLocationUpdates()) {
                    if (!resumed) {
                        resumed = true
                        continuation.resumeWith(Result.success(InitializationResult(
                            success = false,
                            method = InitializationMethod.HIGH_ACCURACY_GPS,
                            position = null,
                            accuracy = null,
                            message = "Location permission not granted or GPS not available"
                        )))
                    }
                    return@suspendCoroutine
                }

                // Observe location updates
                locationObserver = androidx.lifecycle.Observer<CoordinateTransform.GpsCoordinate> { gpsCoordinate ->
                    val accuracy = locationService.locationAccuracy.value
                    if (!resumed && accuracy != null && accuracy <= GPS_ACCEPTABLE_ACCURACY_THRESHOLD) {
                        resumed = true
                        locationService.currentLocation.removeObserver(locationObserver!!)
                        val method = when {
                            accuracy <= GPS_HIGH_ACCURACY_THRESHOLD -> InitializationMethod.HIGH_ACCURACY_GPS
                            accuracy <= GPS_GOOD_ACCURACY_THRESHOLD -> InitializationMethod.GOOD_GPS
                            else -> InitializationMethod.ACCEPTABLE_GPS
                        }
                        continuation.resumeWith(Result.success(InitializationResult(
                            success = true,
                            method = method,
                            position = gpsCoordinate,
                            accuracy = accuracy,
                            message = "GPS initialized with ${accuracy}m accuracy"
                        )))
                    }
                }

                locationService.currentLocation.observeForever(locationObserver)
            }
        } ?: InitializationResult(
            success = false,
            method = InitializationMethod.HIGH_ACCURACY_GPS,
            position = null,
            accuracy = null,
            message = "GPS timeout after ${GPS_TIMEOUT_MS}ms"
        )
    }

    /**
     * Try to use last known location
     */
    private fun tryLastKnownLocation(): InitializationResult {
        // This would typically use LocationManager.getLastKnownLocation()
        // For now, return failure - implement based on your needs
        return InitializationResult(
            success = false,
            method = InitializationMethod.LAST_KNOWN_LOCATION,
            position = null,
            accuracy = null,
            message = "No reliable last known location available"
        )
    }

    /**
     * Try WiFi-based positioning (placeholder for future implementation)
     */
    private fun tryWifiBasedPositioning(): InitializationResult {
        // Placeholder for WiFi fingerprinting or other indoor positioning
        return InitializationResult(
            success = false,
            method = InitializationMethod.WIFI_BASED,
            position = null,
            accuracy = null,
            message = "WiFi positioning not implemented"
        )
    }

    /**
     * Request manual input from user
     */
    private fun requestManualInput(): InitializationResult {
        return InitializationResult(
            success = false,
            method = InitializationMethod.MANUAL_INPUT,
            position = null,
            accuracy = null,
            message = "Please manually set your starting position"
        )
    }

    /**
     * Handle the initialization result
     */
    private fun handleInitializationResult(result: InitializationResult) {
        _initializationStatus.postValue(result)

        when {
            result.success && result.position != null -> {
                Log.i(TAG, "PDR initialized successfully using ${result.method}: ${result.message}")
                pdrProcessor.initializeWithGps(result.position)
                locationService.setReferencePoint(result.position)
            }
            result.method == InitializationMethod.MANUAL_INPUT -> {
                Log.w(TAG, "Manual input required: ${result.message}")
                // UI should handle this case
            }
            else -> {
                Log.w(TAG, "PDR initialization failed: ${result.message}")
                // Fallback to default position with warning
                val defaultPosition = CoordinateTransform.GpsCoordinate(0.0, 0.0, 0.0)
                pdrProcessor.initializeWithManualPosition(0f, 0f, defaultPosition)
            }
        }
    }

    /**
     * Manually set starting position (called from UI)
     */
    fun setManualStartingPosition(latitude: Double, longitude: Double, altitude: Double = 0.0) {
        val gpsCoordinate = CoordinateTransform.GpsCoordinate(latitude, longitude, altitude)
        pdrProcessor.initializeWithGps(gpsCoordinate)
        locationService.setReferencePoint(gpsCoordinate)

        val result = InitializationResult(
            success = true,
            method = InitializationMethod.MANUAL_INPUT,
            position = gpsCoordinate,
            accuracy = null,
            message = "Position set manually to ($latitude, $longitude)"
        )
        _initializationStatus.postValue(result)
        Log.i(TAG, "Manual position set: ${result.message}")
    }

    /**
     * Cancel ongoing initialization
     */
    fun cancelInitialization() {
        initializationJob?.cancel()
        _isInitializing.postValue(false)
    }

    /**
     * Check if PDR needs initialization
     */
    fun needsInitialization(): Boolean {
        return pdrProcessor.isInitialized.value != true
    }

    /**
     * Get current initialization status message for UI
     */
    fun getStatusMessage(): String {
        return when {
            _isInitializing.value == true -> "Searching for your location..."
            needsInitialization() -> "PDR not initialized. Please start initialization."
            else -> "PDR ready and tracking your position."
        }
    }
}
