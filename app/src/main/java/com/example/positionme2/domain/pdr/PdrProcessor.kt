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
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * PDR Processing class
 *
 * This class implements the core PDR algorithms including:
 * - Step detection (if not provided by hardware sensor)
 * - Step length estimation using Weinberg algorithm
 * - Position calculation based on step detection and heading
 * - Elevation estimation
 */
@Singleton
class PdrProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Weinberg algorithm coefficient for stride calculations
        private const val WEINBERG_K = 0.364f

        // Number of samples to keep for elevation calculation
        private const val ELEVATION_WINDOW = 4

        // Number of samples for acceleration
        private const val ACCEL_SAMPLES = 100

        // Movement thresholds
        private const val MOVEMENT_THRESHOLD = 0.3f  // m/s^2
        private const val EPSILON = 0.18f

        // Min samples required for step length calculation
        private const val MIN_REQUIRED_SAMPLES = 2
    }

    // Current position
    private val _pdrPosition = MutableLiveData(PdrPosition(0f, 0f, 0f))
    val pdrPosition: LiveData<PdrPosition> = _pdrPosition

    // Current step length
    private var stepLength = 0.75f

    // Current elevation
    private var elevation = 0f
    private val _elevationData = MutableLiveData(0f)
    val elevationData: LiveData<Float> = _elevationData

    // Elevator detection
    private val _isInElevator = MutableLiveData(false)
    val isInElevator: LiveData<Boolean> = _isInElevator

    // Floor detection
    private var startElevation = 0f
    private var currentFloor = 0
    private val _floorLevel = MutableLiveData(0)
    val floorLevel: LiveData<Int> = _floorLevel

    // Configuration
    private var floorHeight = 4
    private var useManualStepLength = false

    // Step aggregation for analytics
    private var sumStepLength = 0f
    private var stepCount = 0

    // Circular buffers for calculations
    private val elevationBuffer = CircularBuffer<Float>(ELEVATION_WINDOW)
    private val verticalAccelBuffer = CircularBuffer<Float>(ACCEL_SAMPLES)
    private val horizontalAccelBuffer = CircularBuffer<Float>(ACCEL_SAMPLES)
    private val initialElevationBuffer = Array<Float?>(3) { null }
    private var setupIndex = 0

    /**
     * Update PDR position based on step detection
     *
     * @param timestamp Time when step occurred
     * @param accelMagnitudes List of acceleration magnitudes since last step
     * @param headingRad Heading in radians
     * @return Current position after update
     */
    fun updatePdrPosition(
        timestamp: Long,
        accelMagnitudes: List<Float>,
        headingRad: Float
    ): PdrPosition {
        // Safety check for sufficient data
        if (accelMagnitudes.size < MIN_REQUIRED_SAMPLES) {
            return _pdrPosition.value ?: PdrPosition(0f, 0f, 0f)
        }

        // Convert heading (0 rad = North) to standard angle (0 rad = East)
        val adaptedHeading = (Math.PI/2 - headingRad).toFloat()

        // Calculate step length if not using manual value
        if (!useManualStepLength) {
            stepLength = calculateStepLength(accelMagnitudes.map { it.toDouble() })
        }

        // Update step aggregation
        sumStepLength += stepLength
        stepCount++

        // Calculate position change
        val deltaX = stepLength * cos(adaptedHeading)
        val deltaY = stepLength * sin(adaptedHeading)

        // Get current position
        val currentPosition = _pdrPosition.value ?: PdrPosition(0f, 0f, 0f)

        // Update position
        val newPosition = currentPosition.copy(
            x = currentPosition.x + deltaX,
            y = currentPosition.y + deltaY,
            heading = headingRad,
            stepLength = stepLength,
            timestamp = timestamp
        )

        // Post update
        _pdrPosition.postValue(newPosition)

        return newPosition
    }

    /**
     * Calculate step length using Weinberg algorithm
     */
    private fun calculateStepLength(accelMagnitudes: List<Double>): Float {
        // Find min and max acceleration
        val maxAccel = accelMagnitudes.maxOrNull() ?: return 0f
        val minAccel = accelMagnitudes.minOrNull() ?: return 0f

        // Calculate bounce (fourth root of acceleration range)
        val bounce = (maxAccel - minAccel).pow(0.25).toFloat()

        // Apply Weinberg formula
        return bounce * WEINBERG_K * 2
    }

    /**
     * Update elevation based on barometric pressure
     *
     * @param absoluteElevation Absolute elevation in meters from sea level
     * @return Relative elevation from start position
     */
    fun updateElevation(absoluteElevation: Float): Float {
        // Filter invalid values
        if (absoluteElevation < -100 || absoluteElevation > 10000) {
            return elevation
        }

        // Initialize elevation reference
        if (setupIndex < 3) {
            initialElevationBuffer[setupIndex] = absoluteElevation

            if (setupIndex == 2) {
                // Get median of first 3 values as reference
                val validValues = initialElevationBuffer.filterNotNull()
                if (validValues.size == 3) {
                    startElevation = validValues.sorted()[1]
                } else {
                    startElevation = 0f
                }
            }

            setupIndex++
            return 0f
        }

        // Calculate relative elevation
        elevation = absoluteElevation - startElevation
        _elevationData.postValue(elevation)

        // Add to buffer for floor detection
        elevationBuffer.add(absoluteElevation)

        // Detect floor changes
        if (elevationBuffer.isFull()) {
            val avgElevation = elevationBuffer.toList().average().toFloat()

            // Check if elevation change is significant enough for floor change
            if (abs(avgElevation - startElevation) > floorHeight) {
                currentFloor = ((avgElevation - startElevation) / floorHeight).toInt()
                _floorLevel.postValue(currentFloor)
            }
        }

        return elevation
    }

    /**
     * Estimate if user is in elevator based on gravity and acceleration
     */
    fun estimateElevator(
        gravity: FloatArray,
        linearAcceleration: FloatArray
    ): Boolean {
        // Standard gravity
        val g = SensorManager.STANDARD_GRAVITY

        // Calculate vertical acceleration component (along gravity)
        val verticalAcc = sqrt(
            (linearAcceleration[0] * gravity[0]/g).pow(2) +
            (linearAcceleration[1] * gravity[1]/g).pow(2) +
            (linearAcceleration[2] * gravity[2]/g).pow(2)
        )

        // Calculate horizontal acceleration component (perpendicular to gravity)
        val horizontalAcc = sqrt(
            (linearAcceleration[0] * (1 - gravity[0]/g)).pow(2) +
            (linearAcceleration[1] * (1 - gravity[1]/g)).pow(2) +
            (linearAcceleration[2] * (1 - gravity[2]/g)).pow(2)
        )

        // Store in buffers
        verticalAccelBuffer.add(verticalAcc)
        horizontalAccelBuffer.add(horizontalAcc)

        // Evaluate once buffers are full
        if (verticalAccelBuffer.isFull() && horizontalAccelBuffer.isFull()) {
            // Calculate average vertical acceleration magnitude
            val verticalAvg = verticalAccelBuffer.toList().map { abs(it) }.average().toFloat()

            // Calculate average horizontal acceleration magnitude
            val horizontalAvg = horizontalAccelBuffer.toList().map { abs(it) }.average().toFloat()

            // Elevator characteristic: minimal horizontal movement, significant vertical movement
            val inElevator = horizontalAvg < EPSILON && verticalAvg > MOVEMENT_THRESHOLD
            _isInElevator.postValue(inElevator)

            return inElevator
        }

        return _isInElevator.value ?: false
    }

    /**
     * Get average step length from aggregate data
     */
    fun getAverageStepLength(): Float {
        if (stepCount == 0) return 0f

        val average = sumStepLength / stepCount

        // Reset aggregates
        sumStepLength = 0f
        stepCount = 0

        return average
    }

    /**
     * Reset PDR to initial state
     */
    fun resetPdr() {
        _pdrPosition.postValue(PdrPosition(0f, 0f, 0f))
        elevation = 0f
        _elevationData.postValue(0f)
        currentFloor = 0
        _floorLevel.postValue(0)
        setupIndex = 0
        sumStepLength = 0f
        stepCount = 0

        // Clear buffers
        elevationBuffer.clear()
        verticalAccelBuffer.clear()
        horizontalAccelBuffer.clear()
        initialElevationBuffer.fill(null)
    }

    /**
     * Set reference position for PDR
     */
    fun setInitialPosition(x: Float, y: Float) {
        val currentPos = _pdrPosition.value ?: PdrPosition(0f, 0f, 0f)
        _pdrPosition.postValue(currentPos.copy(x = x, y = y))
    }

    /**
     * Configuration methods
     */
    fun setStepLength(length: Float) {
        stepLength = length
    }

    fun useManualStepLength(useManual: Boolean) {
        useManualStepLength = useManual
    }

    fun setFloorHeight(height: Int) {
        floorHeight = height
    }
}

/**
 * A simple circular buffer implementation for sensor data processing
 */
class CircularBuffer<T>(private val capacity: Int) {
    private val buffer = ArrayList<T>(capacity)

    fun add(item: T) {
        if (buffer.size >= capacity) {
            buffer.removeAt(0)
        }
        buffer.add(item)
    }

    fun clear() {
        buffer.clear()
    }

    fun isFull(): Boolean = buffer.size >= capacity

    fun toList(): List<T> = buffer.toList()
}
