package com.example.positionme2.ui.map.line

import android.graphics.Color
import com.example.positionme2.ui.map.engine.MapEngine
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A provider that draws the user's current trajectory as they move.
 * This provider collects position updates and maintains a history
 * of points to draw as a continuous line.
 */
@Singleton
class UserTrajectoryLineProvider @Inject constructor(
    private val mapEngine: MapEngine
) : LineProvider {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    // Store trajectory points with timestamps
    private val trajectoryPoints = MutableStateFlow<List<LatLng>>(emptyList())

    // Unique ID for this trajectory
    private val trajectoryId = UUID.randomUUID().toString()

    // Max number of points to keep in the trajectory
    private val maxTrajectoryPoints = 1000

    // Whether recording is active
    private val isRecording = MutableStateFlow(false)

    init {
        // Collect position updates when recording is active
        coroutineScope.launch {
            combine(
                mapEngine.currentPosition,
                isRecording
            ) { position, recording ->
                Pair(position, recording)
            }.collect { (position, recording) ->
                if (recording && position != null) {
                    val point = LatLng(position.latitude, position.longitude)
                    addTrajectoryPoint(point)
                }
            }
        }
    }

    override val lines: Flow<List<MapLine>> = trajectoryPoints
        .map { points ->
            if (points.size < 2) {
                emptyList()
            } else {
                listOf(
                    MapLine(
                        id = "user_trajectory_$trajectoryId",
                        points = points,
                        width = 5f,
                        color = Color.parseColor("#2196F3"), // Material Blue
                        zIndex = 1.5f
                    )
                )
            }
        }
        .stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    override val type: LineType = LineType.DYNAMIC

    /**
     * Start recording the user's trajectory
     */
    fun startRecording() {
        if (!isRecording.value) {
            trajectoryPoints.value = emptyList()
            isRecording.value = true
        }
    }

    /**
     * Pause recording the user's trajectory
     */
    fun pauseRecording() {
        isRecording.value = false
    }

    /**
     * Resume recording the user's trajectory
     */
    fun resumeRecording() {
        isRecording.value = true
    }

    /**
     * Stop recording and return the recorded trajectory points
     */
    fun stopRecording(): List<LatLng> {
        isRecording.value = false
        return trajectoryPoints.value
    }

    /**
     * Clear the current trajectory
     */
    fun clearTrajectory() {
        trajectoryPoints.value = emptyList()
    }

    private fun addTrajectoryPoint(point: LatLng) {
        trajectoryPoints.update { currentPoints ->
            val newPoints = currentPoints.toMutableList()

            // Only add the point if it's significantly different from the last one
            if (newPoints.isEmpty() || isSignificantMovement(newPoints.last(), point)) {
                newPoints.add(point)

                // Trim the list if it exceeds the maximum size
                if (newPoints.size > maxTrajectoryPoints) {
                    newPoints.subList(0, newPoints.size - maxTrajectoryPoints).clear()
                }
            }

            newPoints
        }
    }

    private fun isSignificantMovement(lastPoint: LatLng, newPoint: LatLng): Boolean {
        // Calculate distance between points (simplified approximation)
        val latDiff = Math.abs(lastPoint.latitude - newPoint.latitude)
        val lngDiff = Math.abs(lastPoint.longitude - newPoint.longitude)

        // Movement is significant if it exceeds a small threshold
        // Adjust these thresholds based on your needs
        return latDiff > 0.00001 || lngDiff > 0.00001
    }
}
