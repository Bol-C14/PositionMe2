package com.example.positionme2.service

import com.example.positionme2.domain.model.*
import com.example.positionme2.domain.replay.ReplayService
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReplayServiceImpl @Inject constructor() : ReplayService {
    private var trajectory: Trajectory? = null
    private var callback: ReplayService.Callback? = null
    private var replayJob: Job? = null
    private var replayScope = CoroutineScope(Dispatchers.Default)
    override var isReplaying: Boolean = false
        private set
    override var currentSpeed: Float = 1.0f
        private set
    private var isPaused = false
    private var pauseLock = Any()

    override fun loadTrajectory(trajectory: Trajectory) {
        this.trajectory = trajectory
    }

    override fun setCallback(callback: ReplayService.Callback) {
        this.callback = callback
    }

    override fun startReplay(speed: Float) {
        if (isReplaying) return
        currentSpeed = speed
        isReplaying = true
        isPaused = false
        replayJob = replayScope.launch { replay() }
        callback?.onReplayStarted()
    }

    override fun pauseReplay() {
        isPaused = true
        callback?.onReplayPaused()
    }

    override fun resumeReplay() {
        if (!isPaused) return
        isPaused = false
        callback?.onReplayResumed()
    }

    override fun stopReplay() {
        isReplaying = false
        replayJob?.cancel()
        callback?.onReplayStopped()
    }

    override fun setSpeed(speed: Float) {
        currentSpeed = speed
    }

    private suspend fun replay() {
        val traj = trajectory ?: return
        val allSamples = mutableListOf<Any>()
        allSamples.addAll(traj.motionSamples)
        allSamples.addAll(traj.positionSamples)
        allSamples.addAll(traj.pressureSamples)
        allSamples.addAll(traj.pdrSamples)
        allSamples.addAll(traj.gnssSamples)
        allSamples.sortBy {
            when (it) {
                is MotionSample -> it.relativeTimestamp
                is PositionSample -> it.relativeTimestamp
                is PressureSample -> it.relativeTimestamp
                is PdrSample -> it.relativeTimestamp
                is GnssSample -> it.relativeTimestamp
                else -> Long.MAX_VALUE
            }
        }
        var lastTimestamp = 0L
        for (sample in allSamples) {
            if (!isReplaying) break
            // Pause logic
            while (isPaused) {
                delay(100)
            }
            val ts = when (sample) {
                is MotionSample -> sample.relativeTimestamp
                is PositionSample -> sample.relativeTimestamp
                is PressureSample -> sample.relativeTimestamp
                is PdrSample -> sample.relativeTimestamp
                is GnssSample -> sample.relativeTimestamp
                else -> 0L
            }
            val delayMs = if (lastTimestamp == 0L) 0L else ((ts - lastTimestamp) / currentSpeed).toLong()
            if (delayMs > 0) delay(delayMs)
            lastTimestamp = ts
            when (sample) {
                is MotionSample -> callback?.onMotionSample(sample)
                is PositionSample -> callback?.onPositionSample(sample)
                is PressureSample -> callback?.onPressureSample(sample)
                is PdrSample -> callback?.onPdrSample(sample)
                is GnssSample -> callback?.onGnssSample(sample)
            }
        }
        isReplaying = false
        callback?.onReplayFinished()
    }
}

