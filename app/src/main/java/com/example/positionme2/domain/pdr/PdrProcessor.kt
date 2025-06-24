package com.example.positionme2.domain.pdr

import android.content.Context
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.positionme2.domain.model.PdrPosition
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pedestrian-Dead-Reckoning processor.
 */
@Singleton
class PdrProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /* ------------------------------------------------------------------ */
    /* Helper to create a zero-initialised position (edit once if model   */
    /* changes).                                                          */
    /* ------------------------------------------------------------------ */
    private fun zeroPos() = PdrPosition(
        x          = 0f,
        y          = 0f,
        heading    = 0f,
        stepLength = 0f,
        timestamp  = 0L
    )

    /* ---------- tunables ---------- */

    private companion object {
        const val WEINBERG_K          = 0.43f      // stride coefficient (m)
        const val MIN_REQUIRED_SAMPLES = 4
        const val ACCEL_BUF_SIZE      = 100
        const val ELEVATION_BUF_SIZE  = 4
        const val MOVEMENT_THRESHOLD  = 0.30f
        const val HORIZONTAL_EPSILON  = 0.18f
    }

    /* ---------- live data ---------- */

    private val _pdrPosition = MutableLiveData(zeroPos())
    val pdrPosition: LiveData<PdrPosition> get() = _pdrPosition

    private val _elevation  = MutableLiveData(0f)
    val elevation: LiveData<Float> get() = _elevation

    private val _inElevator = MutableLiveData(false)
    val inElevator: LiveData<Boolean> get() = _inElevator

    private val _floorLevel = MutableLiveData(0)
    val floorLevel: LiveData<Int> get() = _floorLevel

    /* ---------- state ---------- */

    private var useManualStep = false
    private var manualStepLen = 0.75f

    private var relElevation  = 0f
    private var refElevation  = 0f
    private var floorHeight   = 4f           // metres
    private var setupIndex    = 0            // first-3-sample median

    private var stepSum   = 0f
    private var stepCount = 0

    private val vertAccBuf = CircularBuffer<Float>(ACCEL_BUF_SIZE)
    private val horiAccBuf = CircularBuffer<Float>(ACCEL_BUF_SIZE)
    private val elevBuf    = CircularBuffer<Float>(ELEVATION_BUF_SIZE)
    private val first3Elev = arrayOfNulls<Float>(3)

    /* ---------- public API ---------- */

    /**
     * Call when *one* hardware step is detected.
     */
    fun updatePdrPosition(
        timestamp: Long,
        accelMagn: List<Float>,
        headingRad: Float
    ): PdrPosition {

        if (accelMagn.size < MIN_REQUIRED_SAMPLES)
            return _pdrPosition.value ?: zeroPos()

        val stepLen = if (useManualStep) manualStepLen
        else calculateStepLength(accelMagn.map(Float::toDouble))

        val dx = stepLen * sin(headingRad)   // East
        val dy = stepLen * cos(headingRad)   // North

        val cur = _pdrPosition.value ?: zeroPos()
        val newPos = cur.copy(
            x          = cur.x + dx,
            y          = cur.y + dy,
            heading    = headingRad,
            stepLength = stepLen,
            timestamp  = timestamp
        )
        _pdrPosition.postValue(newPos)

        stepSum   += stepLen
        stepCount += 1

        return newPos
    }

    /**
     * Feed absolute barometric altitude in metres **ASL**.
     */
    fun updateElevation(absElev: Float): Float {
        if (absElev !in -100f..10_000f) return relElevation

        if (setupIndex < 3) {
            first3Elev[setupIndex++] = absElev
            if (setupIndex == 3)
                refElevation = first3Elev.filterNotNull().sorted()[1]
            return 0f
        }

        relElevation = absElev - refElevation
        _elevation.postValue(relElevation)

        elevBuf.add(absElev)
        if (elevBuf.isFull()) {
            val avgElev = elevBuf.toList().average().toFloat()
            val floors  = ((avgElev - refElevation) / floorHeight).roundToInt()
            _floorLevel.postValue(floors)
        }
        return relElevation
    }

    /**
     * Heuristic elevator detector.
     */
    fun estimateElevator(gravity: FloatArray, linAcc: FloatArray): Boolean {
        val gMag  = SensorManager.STANDARD_GRAVITY
        val gUnit = floatArrayOf(gravity[0] / gMag,
            gravity[1] / gMag,
            gravity[2] / gMag)

        val dot = linAcc[0]*gUnit[0] + linAcc[1]*gUnit[1] + linAcc[2]*gUnit[2]
        val vertAcc = abs(dot)

        val hx = linAcc[0] - dot * gUnit[0]
        val hy = linAcc[1] - dot * gUnit[1]
        val hz = linAcc[2] - dot * gUnit[2]
        val horiAcc = sqrt(hx*hx + hy*hy + hz*hz)

        vertAccBuf.add(vertAcc)
        horiAccBuf.add(horiAcc)

        if (vertAccBuf.isFull() && horiAccBuf.isFull()) {
            val vAvg = vertAccBuf.toList().average().toFloat()
            val hAvg = horiAccBuf.toList().average().toFloat()
            val inLift = hAvg < HORIZONTAL_EPSILON && vAvg > MOVEMENT_THRESHOLD
            _inElevator.postValue(inLift)
        }
        return _inElevator.value ?: false
    }

    /** Average step length since last request. */
    fun getAverageStepLength(): Float =
        if (stepCount == 0) 0f else (stepSum / stepCount).also {
            stepSum = 0f; stepCount = 0
        }

    /** Wipe all state. */
    fun resetPdr() {
        _pdrPosition.postValue(zeroPos())
        _elevation.postValue(0f)
        _floorLevel.postValue(0)
        _inElevator.postValue(false)

        relElevation = 0f
        refElevation = 0f
        setupIndex   = 0
        stepSum      = 0f
        stepCount    = 0

        vertAccBuf.clear()
        horiAccBuf.clear()
        elevBuf.clear()
        first3Elev.fill(null)
    }

    /* ----- configuration helpers ----- */

    fun setInitialPosition(x: Float, y: Float) =
        _pdrPosition.postValue((_pdrPosition.value ?: zeroPos()).copy(x = x, y = y))

    fun setManualStepLength(len: Float) { manualStepLen = len }
    fun useManualStepLength(use: Boolean) { useManualStep = use }
    fun setFloorHeight(heightMeters: Float) { floorHeight = heightMeters }

    /* ---------- internal ---------- */

    private fun calculateStepLength(acc: List<Double>): Float {
        val range = (acc.maxOrNull() ?: 0.0) - (acc.minOrNull() ?: 0.0)
        if (range <= 0.0) return manualStepLen
        val bounce = range.pow(0.25).toFloat()
        return WEINBERG_K * bounce
    }
}

/* ------------------------------------------------------------ */
private class CircularBuffer<T>(private val capacity: Int) {
    private val buf = ArrayList<T>(capacity)

    fun add(item: T) {
        if (buf.size == capacity) buf.removeAt(0)
        buf.add(item)
    }

    fun clear() = buf.clear()
    fun isFull() = buf.size == capacity
    fun toList(): List<T> = buf.toList()
}
