package com.example.positionme2.domain.recording

import com.example.positionme2.domain.model.Trajectory

/**
 * Interface for the recording service.
 */
interface RecordingService {
    fun startRecording()
    fun stopRecording()
    fun getTrajectory(): Trajectory?
    val isRecording: Boolean
}

