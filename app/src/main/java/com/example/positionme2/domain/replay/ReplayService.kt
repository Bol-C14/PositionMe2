package com.example.positionme2.domain.replay

import com.example.positionme2.domain.model.Trajectory
import com.example.positionme2.domain.model.MotionSample
import com.example.positionme2.domain.model.PositionSample
import com.example.positionme2.domain.model.PressureSample
import com.example.positionme2.domain.model.PdrSample
import com.example.positionme2.domain.model.GnssSample

interface ReplayService {
    fun loadTrajectory(trajectory: Trajectory)
    fun startReplay(speed: Float = 1.0f)
    fun pauseReplay()
    fun resumeReplay()
    fun stopReplay()
    fun setSpeed(speed: Float)
    val isReplaying: Boolean
    val currentSpeed: Float
    fun setCallback(callback: Callback)

    interface Callback {
        fun onMotionSample(sample: MotionSample)
        fun onPositionSample(sample: PositionSample)
        fun onPressureSample(sample: PressureSample)
        fun onPdrSample(sample: PdrSample)
        fun onGnssSample(sample: GnssSample)
        fun onReplayStarted()
        fun onReplayPaused()
        fun onReplayResumed()
        fun onReplayStopped()
        fun onReplayFinished()
    }
}

