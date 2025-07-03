package com.example.positionme2.ui.map.features

import com.example.positionme2.ui.map.domain.Point
import com.example.positionme2.ui.map.domain.PositionType
import com.example.positionme2.ui.map.domain.Trajectory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central manager for trajectory operations across different modes (explore, record, replay)
 */
@Singleton
class TrajectoryManager @Inject constructor() {

    // Current active trajectories for rendering
    private val _activeTrajectories = MutableStateFlow<List<Trajectory>>(emptyList())
    val activeTrajectories: StateFlow<List<Trajectory>> = _activeTrajectories.asStateFlow()

    // Trajectory storage for different position types
    private val trajectoryPoints = mutableMapOf<PositionType, MutableList<Point>>(
        PositionType.GNSS to mutableListOf(),
        PositionType.PDR to mutableListOf(),
        PositionType.WIFI to mutableListOf(),
        PositionType.FUSED to mutableListOf()
    )

    // Current trajectory mode
    private var currentMode: TrajectoryMode = TrajectoryMode.EXPLORE

    // Visibility flags for different position types
    private val positionTypeVisibility = mutableMapOf<PositionType, Boolean>(
        PositionType.GNSS to true,
        PositionType.PDR to true,
        PositionType.WIFI to true,
        PositionType.FUSED to true
    )

    /**
     * Add a point to trajectory for a specific position type
     */
    fun addPoint(positionType: PositionType, point: Point) {
        if (currentMode != TrajectoryMode.RECORD) return
        if (positionTypeVisibility[positionType] != true) return

        trajectoryPoints[positionType]?.add(point)
        updateActiveTrajectories()
    }

    /**
     * Set trajectory mode (explore, record, replay)
     */
    fun setMode(mode: TrajectoryMode) {
        currentMode = mode
        when (mode) {
            TrajectoryMode.EXPLORE -> {
                // In explore mode, no trajectories are shown
                _activeTrajectories.value = emptyList()
            }
            TrajectoryMode.RECORD -> {
                // Clear existing points and start fresh
                clearTrajectoryPoints()
                updateActiveTrajectories()
            }
            TrajectoryMode.REPLAY -> {
                // Replay mode will set trajectories explicitly
            }
        }
    }

    /**
     * Set visibility for a position type
     */
    fun setPositionTypeVisible(type: PositionType, visible: Boolean) {
        positionTypeVisibility[type] = visible
        updateActiveTrajectories()
    }

    /**
     * Clear all trajectory points
     */
    fun clearTrajectoryPoints() {
        trajectoryPoints.values.forEach { it.clear() }
        updateActiveTrajectories()
    }

    /**
     * Get trajectory for specific position type
     */
    fun getTrajectoryForPositionType(positionType: PositionType): List<Point> {
        return trajectoryPoints[positionType]?.toList() ?: emptyList()
    }

    /**
     * Set trajectories for replay mode
     */
    fun setTrajectoriesForReplay(trajectories: List<Trajectory>) {
        if (currentMode == TrajectoryMode.REPLAY) {
            _activeTrajectories.value = trajectories
        }
    }

    /**
     * Get combined trajectory from all position types
     */
    fun getCombinedTrajectory(): Trajectory? {
        val allPoints = mutableListOf<Point>()
        trajectoryPoints.values.forEach { points ->
            allPoints.addAll(points)
        }

        return if (allPoints.isNotEmpty()) {
            val sortedPoints = allPoints.sortedBy { it.timestamp }
            Trajectory(
                id = "combined_trajectory_${System.currentTimeMillis()}",
                points = sortedPoints,
                name = "Combined Trajectory"
            )
        } else {
            null
        }
    }

    /**
     * Update active trajectories based on current state
     */
    private fun updateActiveTrajectories() {
        if (currentMode != TrajectoryMode.RECORD) return

        val currentTrajectories = mutableListOf<Trajectory>()

        trajectoryPoints.forEach { (positionType, points) ->
            if (points.isNotEmpty() && positionTypeVisibility[positionType] == true) {
                currentTrajectories.add(
                    Trajectory(
                        id = "${positionType.name.lowercase()}_trajectory",
                        points = points.toList(),
                        name = "${positionType.name} Trajectory"
                    )
                )
            }
        }

        _activeTrajectories.value = currentTrajectories
    }
}

enum class TrajectoryMode {
    EXPLORE,  // No trajectory recording/showing
    RECORD,   // Recording and showing trajectory in real-time
    REPLAY    // Replaying saved trajectory
}
