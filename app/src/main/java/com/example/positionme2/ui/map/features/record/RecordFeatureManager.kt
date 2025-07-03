package com.example.positionme2.ui.map.features.record

import com.example.positionme2.ui.map.domain.Point
import com.example.positionme2.ui.map.domain.PositionType
import com.example.positionme2.ui.map.domain.Trajectory
import com.example.positionme2.ui.map.features.TrajectoryManager
import com.example.positionme2.ui.map.features.TrajectoryMode
import com.example.positionme2.ui.map.engine.adapter.FusedPositionProvider
import com.example.positionme2.ui.map.engine.adapter.GnssPositionProvider
import com.example.positionme2.ui.map.engine.adapter.PdrPositionProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import javax.inject.Inject
import com.google.gson.Gson
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

/**
 * Manages record mode - tracks position and records trajectory to file
 */
class RecordFeatureManager @Inject constructor(
    private val trajectoryManager: TrajectoryManager,
    private val gnssPositionProvider: GnssPositionProvider,
    private val pdrPositionProvider: PdrPositionProvider,
    private val fusedPositionProvider: FusedPositionProvider,
    @ApplicationContext private val context: Context
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var isRecording = false
    private val gson = Gson()

    // Current position for record mode
    private val _currentPosition = MutableStateFlow<Point?>(null)
    val currentPosition: StateFlow<Point?> = _currentPosition.asStateFlow()

    // Recording state
    private val _isRecordingState = MutableStateFlow(false)
    val isRecordingState: StateFlow<Boolean> = _isRecordingState.asStateFlow()

    fun startRecording() {
        if (isRecording) return
        isRecording = true
        _isRecordingState.value = true

        // Set trajectory manager to record mode
        trajectoryManager.setMode(TrajectoryMode.RECORD)

        // Start position providers
        gnssPositionProvider.start()
        pdrPositionProvider.start()
        fusedPositionProvider.start()

        // Collect position updates and add to trajectory
        coroutineScope.launch {
            gnssPositionProvider.position.collect { point ->
                point?.let {
                    _currentPosition.value = it
                    trajectoryManager.addPoint(PositionType.GNSS, it)
                }
            }
        }

        coroutineScope.launch {
            pdrPositionProvider.position.collect { point ->
                point?.let {
                    _currentPosition.value = it
                    trajectoryManager.addPoint(PositionType.PDR, it)
                }
            }
        }

        coroutineScope.launch {
            fusedPositionProvider.position.collect { point ->
                point?.let {
                    trajectoryManager.addPoint(PositionType.FUSED, it)
                }
            }
        }
    }

    fun stopRecording(): Trajectory? {
        if (!isRecording) return null
        isRecording = false
        _isRecordingState.value = false

        // Stop position providers
        gnssPositionProvider.stop()
        pdrPositionProvider.stop()
        fusedPositionProvider.stop()

        // Get the recorded trajectory
        val trajectory = trajectoryManager.getCombinedTrajectory()

        // Save trajectory to file if it exists
        trajectory?.let { saveTrajectoryToFile(it) }

        _currentPosition.value = null
        return trajectory
    }

    fun pauseRecording() {
        if (!isRecording) return

        // Stop position providers but keep recording state
        gnssPositionProvider.stop()
        pdrPositionProvider.stop()
        fusedPositionProvider.stop()
    }

    fun resumeRecording() {
        if (!isRecording) return

        // Resume position providers
        gnssPositionProvider.start()
        pdrPositionProvider.start()
        fusedPositionProvider.start()
    }

    fun setPositionTypeVisible(type: PositionType, visible: Boolean) {
        trajectoryManager.setPositionTypeVisible(type, visible)
    }

    private fun saveTrajectoryToFile(trajectory: Trajectory) {
        try {
            val trajectoriesDir = File(context.filesDir, "trajectories")
            if (!trajectoriesDir.exists()) {
                trajectoriesDir.mkdirs()
            }

            val fileName = "trajectory_${System.currentTimeMillis()}.json"
            val file = File(trajectoriesDir, fileName)

            val trajectoryJson = gson.toJson(trajectory)
            FileWriter(file).use { writer ->
                writer.write(trajectoryJson)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isRecordingActive(): Boolean = isRecording
}
