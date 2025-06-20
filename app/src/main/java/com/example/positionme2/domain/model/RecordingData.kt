package com.example.positionme2.domain.model

import android.os.Build

/**
 * Data classes for recording trajectories.
 */
data class Trajectory(
    val androidVersion: String = Build.VERSION.RELEASE,
    val startTimestamp: Long = System.currentTimeMillis(),
    var sensorInfos: List<SensorInfo> = emptyList(),
    val motionSamples: MutableList<MotionSample> = mutableListOf(),
    val positionSamples: MutableList<PositionSample> = mutableListOf(),
    val pressureSamples: MutableList<PressureSample> = mutableListOf(),
    val gnssSamples: MutableList<GnssSample> = mutableListOf(),
    val wifiSamples: MutableList<WiFiSample> = mutableListOf(),
    val pdrSamples: MutableList<PdrSample> = mutableListOf()
)

data class SensorInfo(
    val name: String,
    val vendor: String,
    val resolution: Float,
    val power: Float,
    val version: Int,
    val type: Int
)

data class MotionSample(
    val relativeTimestamp: Long,
    val accX: Float,
    val accY: Float,
    val accZ: Float,
    val gyrX: Float,
    val gyrY: Float,
    val gyrZ: Float,
    val rotationVectorX: Float,
    val rotationVectorY: Float,
    val rotationVectorZ: Float,
    val rotationVectorW: Float,
    val stepCount: Int
)

data class PositionSample(
    val relativeTimestamp: Long,
    val magX: Float,
    val magY: Float,
    val magZ: Float
)

data class PressureSample(
    val relativeTimestamp: Long,
    val pressure: Float
)

data class GnssSample(
    val relativeTimestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val speed: Float,
    val provider: String
)

data class WiFiSample(
    val relativeTimestamp: Long,
    val macScans: List<MacScan>
)

data class MacScan(
    val relativeTimestamp: Long,
    val mac: String,
    val rssi: Int
)

data class PdrSample(
    val relativeTimestamp: Long,
    val x: Float,
    val y: Float
)

