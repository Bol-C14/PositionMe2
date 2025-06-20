package com.example.positionme2.domain.sensor

import androidx.lifecycle.LiveData
import com.example.positionme2.domain.model.*
import com.google.android.gms.maps.model.LatLng

/**
 * Interface for the SensorFusion service
 *
 * This service is responsible for collecting sensor data,
 * processing it through PDR algorithms, and providing position updates.
 */
interface SensorFusionService {

    // Sensor data streams
    val accelerometerData: LiveData<AccelerometerData>
    val linearAccelerationData: LiveData<LinearAccelerationData>
    val gyroscopeData: LiveData<GyroscopeData>
    val magnetometerData: LiveData<MagnetometerData>
    val rotationVectorData: LiveData<RotationVectorData>
    val gravityData: LiveData<GravityData>
    val pressureData: LiveData<PressureData>
    val stepDetected: LiveData<StepData>
    val gnssData: LiveData<GnssData>

    // Position data streams
    val pdrPosition: LiveData<PdrPosition>  // PDR position in ENU coordinates
    val currentLatLng: LiveData<LatLng>     // Current position as LatLng
    val currentFloor: LiveData<Int>         // Current floor number

    // Elevation data
    val elevation: LiveData<Float>          // Current elevation in meters
    val isInElevator: LiveData<Boolean>     // Whether user is in elevator

    // Control functions
    fun startSensors()
    fun stopSensors()
    fun startRecording()
    fun stopRecording()
    fun resetPdr()

    // Initialize with reference position
    fun setReferencePosition(latitude: Double, longitude: Double, altitude: Double = 0.0)

    // Configuration
    fun setStepLength(stepLength: Float)
    fun useManualStepLength(useManual: Boolean)
    fun setFloorHeight(heightMeters: Int)
}
