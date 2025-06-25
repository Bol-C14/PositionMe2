package com.example.positionme2.ui.map.trajectory

import android.graphics.Color
import com.example.positionme2.ui.map.domain.Point
import com.example.positionme2.ui.map.domain.Trajectory
import com.example.positionme2.ui.map.line.LineProvider
import com.example.positionme2.ui.map.line.LineType
import com.example.positionme2.ui.map.line.MapLine
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Playback states for trajectory replay
 */
enum class PlaybackState {
    STOPPED,
    PLAYING,
    PAUSED
}

/**
 * Playback direction for trajectory replay
 */
enum class PlaybackDirection {
    FORWARD,
    REVERSE
}

/**
 * A provider specifically designed for both live trajectory drawing and
 * trajectory replay with advanced playback controls.
 */
@Singleton
class TrajectoryPlaybackProvider @Inject constructor() : LineProvider {

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // The source trajectory points with timestamps
    private val _sourceTrajectory = MutableStateFlow<List<TrajectoryPoint>>(emptyList())

    // The currently visible portion of the trajectory
    private val _visibleTrajectory = MutableStateFlow<List<TrajectoryPoint>>(emptyList())

    // Whether we're in live mode or playback mode
    private val _mode = MutableStateFlow(TrajectoryMode.LIVE)
    val mode: StateFlow<TrajectoryMode> = _mode.asStateFlow()

    // Playback state (playing, paused, stopped)
    private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // Playback direction (forward or reverse)
    private val _playbackDirection = MutableStateFlow(PlaybackDirection.FORWARD)
    val playbackDirection: StateFlow<PlaybackDirection> = _playbackDirection.asStateFlow()

    // Playback speed multiplier (1.0 = normal speed)
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Current playback position (0.0 to 1.0)
    private val _playbackPosition = MutableStateFlow(0.0f)
    val playbackPosition: StateFlow<Float> = _playbackPosition.asStateFlow()

    // IDs for all trajectories being displayed
    private val trajectoryIds = mutableMapOf<String, TrajectoryStyle>()

    // Current playback job
    private var playbackJob: Job? = null

    // Unique ID for this provider instance
    private val providerId = UUID.randomUUID().toString()

    override val lines: Flow<List<MapLine>> = _visibleTrajectory
        .map { trajectoryPoints ->
            if (trajectoryPoints.isEmpty()) {
                emptyList()
            } else {
                // Group points by trajectory ID
                trajectoryPoints.groupBy { it.trajectoryId }
                    .map { (trajectoryId, points) ->
                        val style = trajectoryIds[trajectoryId] ?: TrajectoryStyle()

                        MapLine(
                            id = "trajectory_$trajectoryId",
                            points = points.map { LatLng(it.latitude, it.longitude) },
                            width = style.width,
                            color = style.color,
                            zIndex = style.zIndex
                        )
                    }
            }
        }
        .stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    override val type: LineType = LineType.DYNAMIC

    /**
     * Set the trajectory for playback
     */
    fun setTrajectory(trajectory: Trajectory, style: TrajectoryStyle = TrajectoryStyle()) {
        // Convert domain trajectory to trajectory points with timestamps
        val points = trajectory.points.mapIndexed { index, point ->
            TrajectoryPoint(
                trajectoryId = trajectory.id,
                latitude = point.latitude,
                longitude = point.longitude,
                timestamp = point.timestamp ?: System.currentTimeMillis() + index * 1000L, // Fallback if no timestamp
                index = index
            )
        }.sortedBy { it.timestamp }

        // Store the trajectory style
        trajectoryIds[trajectory.id] = style

        coroutineScope.launch {
            // Reset playback state
            _playbackState.value = PlaybackState.STOPPED
            _playbackPosition.value = 0.0f

            // Switch to playback mode
            _mode.value = TrajectoryMode.PLAYBACK

            // Set the source trajectory
            _sourceTrajectory.value = points

            // Initialize with empty visible trajectory
            _visibleTrajectory.value = emptyList()
        }
    }

    /**
     * Set multiple trajectories for synchronized playback
     */
    fun setTrajectories(trajectories: List<Pair<Trajectory, TrajectoryStyle>>) {
        val allPoints = mutableListOf<TrajectoryPoint>()

        // Process each trajectory
        trajectories.forEach { (trajectory, style) ->
            // Store the trajectory style
            trajectoryIds[trajectory.id] = style

            // Convert domain trajectory to trajectory points
            val points = trajectory.points.mapIndexed { index, point ->
                TrajectoryPoint(
                    trajectoryId = trajectory.id,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    timestamp = point.timestamp ?: System.currentTimeMillis() + index * 1000L,
                    index = index
                )
            }

            allPoints.addAll(points)
        }

        coroutineScope.launch {
            // Reset playback state
            _playbackState.value = PlaybackState.STOPPED
            _playbackPosition.value = 0.0f

            // Switch to playback mode
            _mode.value = TrajectoryMode.PLAYBACK

            // Set the source trajectory (sorted by timestamp)
            _sourceTrajectory.value = allPoints.sortedBy { it.timestamp }

            // Initialize with empty visible trajectory
            _visibleTrajectory.value = emptyList()
        }
    }

    /**
     * Add a point to the live trajectory
     */
    fun addLivePoint(point: Point, trajectoryId: String = providerId) {
        // Only add points in live mode
        if (_mode.value != TrajectoryMode.LIVE) return

        // Create a trajectory point
        val trajectoryPoint = TrajectoryPoint(
            trajectoryId = trajectoryId,
            latitude = point.latitude,
            longitude = point.longitude,
            timestamp = point.timestamp ?: System.currentTimeMillis(),
            index = _sourceTrajectory.value.size
        )

        // Ensure we have a style for this trajectory
        if (!trajectoryIds.containsKey(trajectoryId)) {
            trajectoryIds[trajectoryId] = TrajectoryStyle()
        }

        coroutineScope.launch {
            // Add to both source and visible trajectories
            val currentSource = _sourceTrajectory.value.toMutableList()
            currentSource.add(trajectoryPoint)
            _sourceTrajectory.value = currentSource

            val currentVisible = _visibleTrajectory.value.toMutableList()
            currentVisible.add(trajectoryPoint)
            _visibleTrajectory.value = currentVisible
        }
    }

    /**
     * Clear all trajectories
     */
    fun clearTrajectories() {
        coroutineScope.launch {
            stopPlayback()
            _sourceTrajectory.value = emptyList()
            _visibleTrajectory.value = emptyList()
            trajectoryIds.clear()
        }
    }

    /**
     * Start or resume playback
     */
    fun startPlayback() {
        if (_mode.value != TrajectoryMode.PLAYBACK ||
            _playbackState.value == PlaybackState.PLAYING) return

        // Cancel any existing playback
        playbackJob?.cancel()

        // Set state to playing
        _playbackState.value = PlaybackState.PLAYING

        // Start a new playback job
        playbackJob = coroutineScope.launch {
            try {
                val sourcePoints = _sourceTrajectory.value
                if (sourcePoints.isEmpty()) return@launch

                // Get time range
                val startTime = sourcePoints.first().timestamp
                val endTime = sourcePoints.last().timestamp
                val duration = endTime - startTime
                if (duration <= 0) return@launch

                // Get current position
                var currentPosition = _playbackPosition.value
                val currentTime = startTime + (duration * currentPosition).toLong()

                // Initialize visible points based on current position
                if (_playbackDirection.value == PlaybackDirection.FORWARD) {
                    _visibleTrajectory.value = sourcePoints.filter { it.timestamp <= currentTime }
                } else {
                    _visibleTrajectory.value = sourcePoints.filter { it.timestamp >= currentTime }
                }

                // Determine the frame duration based on speed
                val frameTimeMs = (1000 / (30 * _playbackSpeed.value)).toLong().coerceAtLeast(16)

                // Playback loop
                while (isActive && _playbackState.value == PlaybackState.PLAYING) {
                    // Calculate how much to advance
                    val positionDelta = (frameTimeMs.toFloat() / duration) * _playbackSpeed.value

                    // Update position based on direction
                    if (_playbackDirection.value == PlaybackDirection.FORWARD) {
                        currentPosition += positionDelta
                        if (currentPosition > 1.0f) {
                            currentPosition = 1.0f
                            _playbackState.value = PlaybackState.PAUSED
                        }
                    } else {
                        currentPosition -= positionDelta
                        if (currentPosition < 0.0f) {
                            currentPosition = 0.0f
                            _playbackState.value = PlaybackState.PAUSED
                        }
                    }

                    _playbackPosition.value = currentPosition

                    // Calculate current playback time
                    val playbackTime = startTime + (duration * currentPosition).toLong()

                    // Update visible trajectory based on current time and direction
                    if (_playbackDirection.value == PlaybackDirection.FORWARD) {
                        _visibleTrajectory.value = sourcePoints.filter { it.timestamp <= playbackTime }
                    } else {
                        _visibleTrajectory.value = sourcePoints.filter { it.timestamp >= playbackTime }
                    }

                    // Wait for next frame
                    delay(frameTimeMs)
                }
            } catch (e: CancellationException) {
                // Playback was cancelled, which is fine
            } catch (e: Exception) {
                // Handle other exceptions
                e.printStackTrace()
            }
        }
    }

    /**
     * Pause playback
     */
    fun pausePlayback() {
        if (_playbackState.value == PlaybackState.PLAYING) {
            _playbackState.value = PlaybackState.PAUSED
            playbackJob?.cancel()
        }
    }

    /**
     * Stop playback and reset to the beginning
     */
    fun stopPlayback() {
        playbackJob?.cancel()
        _playbackState.value = PlaybackState.STOPPED
        _playbackPosition.value = 0.0f

        // Reset visible trajectory based on direction
        val sourcePoints = _sourceTrajectory.value
        if (sourcePoints.isNotEmpty()) {
            if (_playbackDirection.value == PlaybackDirection.FORWARD) {
                _visibleTrajectory.value = emptyList()
            } else {
                _visibleTrajectory.value = sourcePoints
            }
        }
    }

    /**
     * Set playback speed
     * @param speed Speed multiplier (1.0 = normal speed)
     */
    fun setPlaybackSpeed(speed: Float) {
        if (speed > 0) {
            _playbackSpeed.value = speed

            // Restart playback if we're currently playing
            if (_playbackState.value == PlaybackState.PLAYING) {
                val wasPlaying = true
                pausePlayback()
                if (wasPlaying) {
                    startPlayback()
                }
            }
        }
    }

    /**
     * Toggle playback direction between forward and reverse
     */
    fun toggleDirection() {
        _playbackDirection.value = if (_playbackDirection.value == PlaybackDirection.FORWARD) {
            PlaybackDirection.REVERSE
        } else {
            PlaybackDirection.FORWARD
        }

        // Restart playback if we're currently playing
        if (_playbackState.value == PlaybackState.PLAYING) {
            val wasPlaying = true
            pausePlayback()
            if (wasPlaying) {
                startPlayback()
            }
        }
    }

    /**
     * Seek to a specific position in the trajectory (0.0 to 1.0)
     */
    fun seekToPosition(position: Float) {
        val clampedPosition = position.coerceIn(0f, 1f)
        _playbackPosition.value = clampedPosition

        val sourcePoints = _sourceTrajectory.value
        if (sourcePoints.isNotEmpty()) {
            val startTime = sourcePoints.first().timestamp
            val endTime = sourcePoints.last().timestamp
            val duration = endTime - startTime
            val seekTime = startTime + (duration * clampedPosition).toLong()

            // Update visible trajectory based on seek time and direction
            if (_playbackDirection.value == PlaybackDirection.FORWARD) {
                _visibleTrajectory.value = sourcePoints.filter { it.timestamp <= seekTime }
            } else {
                _visibleTrajectory.value = sourcePoints.filter { it.timestamp >= seekTime }
            }
        }
    }

    /**
     * Switch to live mode (for real-time tracking)
     */
    fun switchToLiveMode() {
        playbackJob?.cancel()
        _mode.value = TrajectoryMode.LIVE
        _playbackState.value = PlaybackState.STOPPED

        // In live mode, all points are visible
        _visibleTrajectory.value = _sourceTrajectory.value
    }

    /**
     * Switch to playback mode (for trajectory replay)
     */
    fun switchToPlaybackMode() {
        playbackJob?.cancel()
        _mode.value = TrajectoryMode.PLAYBACK
        _playbackState.value = PlaybackState.STOPPED
        _playbackPosition.value = 0.0f

        // Reset visible trajectory based on direction
        if (_playbackDirection.value == PlaybackDirection.FORWARD) {
            _visibleTrajectory.value = emptyList()
        } else {
            _visibleTrajectory.value = _sourceTrajectory.value
        }
    }

    /**
     * Clean up resources when this provider is no longer needed
     */
    fun cleanup() {
        playbackJob?.cancel()
        coroutineScope.cancel()
    }
}

/**
 * Data class for a point in a trajectory with timestamp and index
 */
data class TrajectoryPoint(
    val trajectoryId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val index: Int
)

/**
 * Styling options for a trajectory
 */
data class TrajectoryStyle(
    val color: Int = Color.BLUE,
    val width: Float = 5f,
    val zIndex: Float = 1f
)

/**
 * Trajectory mode (live or playback)
 */
enum class TrajectoryMode {
    LIVE,      // Real-time tracking
    PLAYBACK   // Replay of recorded trajectory
}
