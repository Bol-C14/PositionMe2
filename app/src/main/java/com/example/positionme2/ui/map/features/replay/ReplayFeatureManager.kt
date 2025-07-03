package com.example.positionme2.ui.map.features.replay

import com.example.positionme2.ui.map.domain.Point
import com.example.positionme2.ui.map.domain.Trajectory
import com.example.positionme2.ui.map.features.TrajectoryManager
import com.example.positionme2.ui.map.features.TrajectoryMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileReader
import javax.inject.Inject
import com.google.gson.Gson
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Manages replay mode - reads trajectory from file and replays position and trajectory
 */
class ReplayFeatureManager @Inject constructor(
    private val trajectoryManager: TrajectoryManager,
    @ApplicationContext private val context: Context
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var replayJob: Job? = null
    private val gson = Gson()

    // Current replay position
    private val _currentPosition = MutableStateFlow<Point?>(null)
    val currentPosition: StateFlow<Point?> = _currentPosition.asStateFlow()

    // Replay state
    private val _isReplaying = MutableStateFlow(false)
    val isReplaying: StateFlow<Boolean> = _isReplaying.asStateFlow()

    // Replay progress (0.0 to 1.0)
    private val _replayProgress = MutableStateFlow(0f)
    val replayProgress: StateFlow<Float> = _replayProgress.asStateFlow()

    // Available trajectory files
    private val _availableTrajectories = MutableStateFlow<List<String>>(emptyList())
    val availableTrajectories: StateFlow<List<String>> = _availableTrajectories.asStateFlow()

    init {
        loadAvailableTrajectories()
    }

    fun startReplay(trajectoryFileName: String, speedMultiplier: Float = 1.0f) {
        if (_isReplaying.value) return

        val trajectory = loadTrajectoryFromFile(trajectoryFileName) ?: return

        _isReplaying.value = true
        trajectoryManager.setMode(TrajectoryMode.REPLAY)
        trajectoryManager.setTrajectoriesForReplay(listOf(trajectory))

        replayJob = coroutineScope.launch {
            replayTrajectory(trajectory, speedMultiplier)
        }
    }

    fun startReplay(trajectory: Trajectory, speedMultiplier: Float = 1.0f) {
        if (_isReplaying.value) return

        _isReplaying.value = true
        trajectoryManager.setMode(TrajectoryMode.REPLAY)
        trajectoryManager.setTrajectoriesForReplay(listOf(trajectory))

        replayJob = coroutineScope.launch {
            replayTrajectory(trajectory, speedMultiplier)
        }
    }

    fun stopReplay() {
        replayJob?.cancel()
        replayJob = null
        _isReplaying.value = false
        _replayProgress.value = 0f
        _currentPosition.value = null
    }

    fun pauseReplay() {
        replayJob?.cancel()
    }

    fun resumeReplay(trajectory: Trajectory, currentProgress: Float, speedMultiplier: Float = 1.0f) {
        if (_isReplaying.value) return

        replayJob = coroutineScope.launch {
            val startIndex = (trajectory.points.size * currentProgress).toInt()
            replayTrajectory(trajectory, speedMultiplier, startIndex)
        }
    }

    private suspend fun replayTrajectory(trajectory: Trajectory, speedMultiplier: Float, startIndex: Int = 0) {
        val points = trajectory.points
        if (points.isEmpty()) return

        for (i in startIndex until points.size) {
            if (!_isReplaying.value) break

            val currentPoint = points[i]
            _currentPosition.value = currentPoint
            _replayProgress.value = i.toFloat() / points.size

            // Calculate delay based on timestamp differences and speed multiplier
            if (i < points.size - 1) {
                val nextPoint = points[i + 1]
                val timeDiff = nextPoint.timestamp - currentPoint.timestamp
                val adjustedDelay = (timeDiff / speedMultiplier).toLong().coerceAtLeast(10)
                delay(adjustedDelay)
            }
        }

        // Replay completed
        _replayProgress.value = 1f
        _isReplaying.value = false
    }

    private fun loadTrajectoryFromFile(fileName: String): Trajectory? {
        return try {
            val trajectoriesDir = File(context.filesDir, "trajectories")
            val file = File(trajectoriesDir, fileName)

            if (!file.exists()) return null

            FileReader(file).use { reader ->
                gson.fromJson(reader, Trajectory::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadAvailableTrajectories() {
        try {
            val trajectoriesDir = File(context.filesDir, "trajectories")
            if (!trajectoriesDir.exists()) {
                trajectoriesDir.mkdirs()
                _availableTrajectories.value = emptyList()
                return
            }

            val trajectoryFiles = trajectoriesDir.listFiles { file ->
                file.isFile && file.name.endsWith(".json")
            }?.map { it.name } ?: emptyList()

            _availableTrajectories.value = trajectoryFiles
        } catch (e: Exception) {
            e.printStackTrace()
            _availableTrajectories.value = emptyList()
        }
    }

    fun refreshAvailableTrajectories() {
        loadAvailableTrajectories()
    }

    fun deleteTrajectory(fileName: String): Boolean {
        return try {
            val trajectoriesDir = File(context.filesDir, "trajectories")
            val file = File(trajectoriesDir, fileName)
            val deleted = file.delete()
            if (deleted) {
                loadAvailableTrajectories()
            }
            deleted
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isReplayActive(): Boolean = _isReplaying.value
}
