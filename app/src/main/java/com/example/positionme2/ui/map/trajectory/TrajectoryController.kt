package com.example.positionme2.ui.map.trajectory

import com.example.positionme2.ui.map.line.UserTrajectoryLineProvider
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recording states for the trajectory controller
 */
enum class TrajectoryRecordingState {
    IDLE,
    RECORDING,
    PAUSED
}

/**
 * Controller for managing user trajectory recording.
 * Follows the Controller pattern from MVC architecture.
 */
@Singleton
class TrajectoryController @Inject constructor(
    private val userTrajectoryLineProvider: UserTrajectoryLineProvider
) {
    // Current recording state
    private val _recordingState = MutableStateFlow(TrajectoryRecordingState.IDLE)
    val recordingState: StateFlow<TrajectoryRecordingState> = _recordingState.asStateFlow()

    // Recording duration (in seconds)
    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    // Recording distance (in meters)
    private val _recordingDistance = MutableStateFlow(0.0)
    val recordingDistance: StateFlow<Double> = _recordingDistance.asStateFlow()

    // Last recording time
    private var lastRecordingTimeMs = 0L

    // Recording timer thread
    private var timerThread: Thread? = null

    /**
     * Start recording a new trajectory
     */
    fun startRecording() {
        if (_recordingState.value == TrajectoryRecordingState.IDLE) {
            // Reset counters
            _recordingDuration.value = 0L
            _recordingDistance.value = 0.0
            lastRecordingTimeMs = System.currentTimeMillis()

            // Start the actual recording
            userTrajectoryLineProvider.startRecording()
            _recordingState.value = TrajectoryRecordingState.RECORDING

            // Start timer thread
            startTimerThread()
        }
    }

    /**
     * Pause the current recording
     */
    fun pauseRecording() {
        if (_recordingState.value == TrajectoryRecordingState.RECORDING) {
            userTrajectoryLineProvider.pauseRecording()
            _recordingState.value = TrajectoryRecordingState.PAUSED

            // Stop timer thread
            stopTimerThread()
        }
    }

    /**
     * Resume a paused recording
     */
    fun resumeRecording() {
        if (_recordingState.value == TrajectoryRecordingState.PAUSED) {
            userTrajectoryLineProvider.resumeRecording()
            _recordingState.value = TrajectoryRecordingState.RECORDING
            lastRecordingTimeMs = System.currentTimeMillis()

            // Restart timer thread
            startTimerThread()
        }
    }

    /**
     * Stop and save the current recording
     * @return The recorded trajectory points
     */
    fun stopRecording(): List<LatLng> {
        val trajectoryPoints = if (_recordingState.value != TrajectoryRecordingState.IDLE) {
            userTrajectoryLineProvider.stopRecording()
        } else {
            emptyList()
        }

        _recordingState.value = TrajectoryRecordingState.IDLE
        stopTimerThread()

        return trajectoryPoints
    }

    /**
     * Clear the current trajectory without saving
     */
    fun cancelRecording() {
        userTrajectoryLineProvider.clearTrajectory()
        _recordingState.value = TrajectoryRecordingState.IDLE
        _recordingDuration.value = 0L
        _recordingDistance.value = 0.0
        stopTimerThread()
    }

    /**
     * Update the recording distance based on new points
     */
    private fun updateDistance(points: List<LatLng>) {
        // Skip if we don't have enough points
        if (points.size < 2) return

        // Get the last two points and calculate distance
        val lastPoint = points[points.size - 1]
        val prevPoint = points[points.size - 2]

        val segmentDistance = calculateDistance(prevPoint, lastPoint)
        _recordingDistance.value += segmentDistance
    }

    /**
     * Calculate distance between two points using the Haversine formula
     */
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val R = 6371000.0 // Earth radius in meters

        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)
        val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLng = Math.toRadians(point2.longitude - point1.longitude)

        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c
    }

    /**
     * Start the timer thread for tracking recording duration
     */
    private fun startTimerThread() {
        stopTimerThread() // Ensure no existing timer is running

        timerThread = Thread {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    if (_recordingState.value == TrajectoryRecordingState.RECORDING) {
                        val now = System.currentTimeMillis()
                        val elapsed = (now - lastRecordingTimeMs) / 1000 // Convert to seconds
                        _recordingDuration.value += elapsed
                        lastRecordingTimeMs = now
                    }
                    Thread.sleep(1000) // Update every second
                }
            } catch (e: InterruptedException) {
                // Thread was interrupted, exit gracefully
            }
        }

        timerThread?.start()
    }

    /**
     * Stop the timer thread
     */
    private fun stopTimerThread() {
        timerThread?.interrupt()
        timerThread = null
    }
}
