package com.example.positionme2.domain.model

import android.hardware.SensorManager

/**
 * Data classes for sensor readings
 */

data class AccelerometerData(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
) {
    val magnitude: Double
        get() = Math.sqrt((x * x + y * y + z * z).toDouble())
}

data class GyroscopeData(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
)

data class MagnetometerData(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
)

data class RotationVectorData(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
    val timestamp: Long
) {
    // Convert rotation vector to orientation angles (azimuth, pitch, roll)
    fun toOrientationAngles(): FloatArray {
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        // Convert rotation vector to rotation matrix
        SensorManager.getRotationMatrixFromVector(rotationMatrix, floatArrayOf(x, y, z, w))

        // Get orientation angles from rotation matrix
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        return orientationAngles
    }
}

data class GravityData(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
)

data class LinearAccelerationData(
    val x: Float,
    val y: Float,
    val z: Float,
    val timestamp: Long
) {
    val magnitude: Double
        get() = Math.sqrt((x * x + y * y + z * z).toDouble())
}

data class PressureData(
    val pressure: Float,
    val timestamp: Long
)

data class GnssData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val timestamp: Long
)

data class StepData(
    val timestamp: Long,
    val stepLength: Float,
    val heading: Float,
    val stepCount: Int = 0
)

data class PdrPosition(
    val x: Float,  // East coordinate in meters (ENU)
    val y: Float,  // North coordinate in meters (ENU)
    val heading: Float,  // Heading in radians
    val floor: Int = 0,
    val stepLength: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class WifiFingerprint(
    val bssid: String,
    val ssid: String,
    val level: Int,
    val frequency: Int
)
