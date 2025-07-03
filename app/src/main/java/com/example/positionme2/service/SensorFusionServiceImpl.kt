package com.example.positionme2.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.positionme2.domain.model.*
import com.example.positionme2.domain.pdr.PdrProcessor
import com.example.positionme2.domain.sensor.SensorFusionService
import com.example.positionme2.utils.CoordinateTransform
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorFusionServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdrProcessor: PdrProcessor,
    private val locationService: LocationService
) : SensorFusionService, SensorEventListener {

    companion object {
        private const val TAG = "SensorFusion"
        private const val LARGE_GAP_THRESHOLD_MS = 500L
        private const val GPS_PDR_SYNC_INTERVAL_MS = 30000L // 30 seconds
        private const val GPS_ACCURACY_THRESHOLD = 15.0f // 15 meters
    }

    // Sensor Manager
    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    // Location Manager
    private val locationManager: LocationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    // Sensors
    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null
    private var rotationVectorSensor: Sensor? = null
    private var gravitySensor: Sensor? = null
    private var linearAccelerationSensor: Sensor? = null
    private var pressureSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null

    // Sensor data LiveData
    private val _accelerometerData = MutableLiveData<AccelerometerData>()
    override val accelerometerData: LiveData<AccelerometerData> = _accelerometerData

    private val _linearAccelerationData = MutableLiveData<LinearAccelerationData>()
    override val linearAccelerationData: LiveData<LinearAccelerationData> = _linearAccelerationData

    private val _gyroscopeData = MutableLiveData<GyroscopeData>()
    override val gyroscopeData: LiveData<GyroscopeData> = _gyroscopeData

    private val _magnetometerData = MutableLiveData<MagnetometerData>()
    override val magnetometerData: LiveData<MagnetometerData> = _magnetometerData

    private val _rotationVectorData = MutableLiveData<RotationVectorData>()
    override val rotationVectorData: LiveData<RotationVectorData> = _rotationVectorData

    private val _gravityData = MutableLiveData<GravityData>()
    override val gravityData: LiveData<GravityData> = _gravityData

    private val _pressureData = MutableLiveData<PressureData>()
    override val pressureData: LiveData<PressureData> = _pressureData

    private val _stepDetected = MutableLiveData<StepData>()
    override val stepDetected: LiveData<StepData> = _stepDetected

    // Position data
    override val pdrPosition: LiveData<PdrPosition> = pdrProcessor.pdrPosition

    private val _currentLatLng = MutableLiveData<LatLng>()
    override val currentLatLng: LiveData<LatLng> = _currentLatLng

    private val _currentFloor = MutableLiveData(0)
    override val currentFloor: LiveData<Int> = _currentFloor

    // Elevation data
    override val elevation: LiveData<Float> = pdrProcessor.elevation
    override val isInElevator: LiveData<Boolean> = pdrProcessor.inElevator

    // Sensor values
    private val acceleration = FloatArray(3)
    private val gravity = FloatArray(3)
    private val magneticField = FloatArray(3)
    private val angularVelocity = FloatArray(3)
    private val orientation = FloatArray(3)
    private val rotation = FloatArray(4) { if (it == 3) 1f else 0f }
    private val linearAcceleration = FloatArray(3)
    private var pressure = 0f

    // Step detection variables
    private var bootTime = SystemClock.uptimeMillis()
    private var stepCounter = 0
    private var accelMagnitude = 0f
    private val accelMagnitudes = mutableListOf<Float>()

    // Last timestamp tracking
    private val lastEventTimestamps = HashMap<Int, Long>()
    private val eventCounts = HashMap<Int, Int>()
    private var lastStepTime = 0L

    // Reference position for coordinate conversion
    private var refLat = 0.0
    private var refLon = 0.0
    private var refAlt = 0.0

    // GNSS position variables
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var altitude: Double = 0.0
    private var gnssAccuracy: Float = 0f

    // GNSS LiveData
    private val _gnssData = MutableLiveData<GnssData>()
    override val gnssData: LiveData<GnssData> = _gnssData

    // GNSS listener - ensure WGS84 compliance
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Convert Android Location to WGS84 GpsCoordinate
            val gpsCoordinate = CoordinateTransform.fromAndroidLocation(location)

            // Validate WGS84 coordinates
            if (!CoordinateTransform.isValidWgs84(gpsCoordinate.latitude, gpsCoordinate.longitude)) {
                Log.w(TAG, "Invalid WGS84 coordinates from location listener: ${gpsCoordinate.latitude}, ${gpsCoordinate.longitude}")
                return
            }

            latitude = gpsCoordinate.latitude
            longitude = gpsCoordinate.longitude
            altitude = gpsCoordinate.altitude
            gnssAccuracy = location.accuracy

            // Update LiveData with WGS84 coordinates
            _gnssData.postValue(GnssData(latitude, longitude, altitude, gnssAccuracy, System.currentTimeMillis()))

            // Update reference position if needed
            if (refLat == 0.0 && refLon == 0.0) {
                setReferencePosition(latitude, longitude, altitude)
            }

            Log.d(TAG, "WGS84 GNSS position update: $latitude, $longitude (accuracy: $gnssAccuracy)")
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    // Fallback for software step detection
    private var useSoftwareStepDetection = false
    private var lastAccelPeakTime = 0L
    private var lastAccelValue = 0f
    private var lastStepAccel = 0f
    private val STEP_THRESHOLD = 10.5f // Tune as needed
    private val STEP_MIN_TIME_MS = 300L

    // GPS-PDR synchronization
    private var lastGpsPdrSyncTime = 0L
    private var isPdrInitialized = false

    init {
        // Initialize sensors
        initializeSensors()

        // Observe GPS location for PDR initialization and periodic correction
        locationService.currentLocation.observeForever { gpsCoordinate ->
            handleGpsUpdate(gpsCoordinate)
        }

        locationService.locationAccuracy.observeForever { accuracy ->
            handleLocationAccuracyUpdate(accuracy)
        }

        // Observe PDR initialization status
        pdrProcessor.isInitialized.observeForever { initialized ->
            isPdrInitialized = initialized
        }
    }

    private fun handleGpsUpdate(gpsCoordinate: CoordinateTransform.GpsCoordinate) {
        val currentTime = System.currentTimeMillis()

        if (!isPdrInitialized) {
            // Initialize PDR with first good GPS fix
            Log.d(TAG, "Initializing PDR with WGS84 GPS: lat=${gpsCoordinate.latitude}, lng=${gpsCoordinate.longitude}")
            pdrProcessor.initializeWithGps(gpsCoordinate)
            locationService.setReferencePoint(gpsCoordinate)
            lastGpsPdrSyncTime = currentTime

            // Update current LatLng for map display using WGS84-compliant conversion
            _currentLatLng.postValue(CoordinateTransform.toLatLng(gpsCoordinate))
        } else {
            // Periodic GPS-PDR synchronization for drift correction
            if (currentTime - lastGpsPdrSyncTime > GPS_PDR_SYNC_INTERVAL_MS) {
                val accuracy = locationService.locationAccuracy.value ?: Float.MAX_VALUE
                if (accuracy <= GPS_ACCURACY_THRESHOLD) {
                    Log.d(TAG, "Correcting PDR drift with WGS84 GPS update")
                    pdrProcessor.updateFromGps(gpsCoordinate)
                    lastGpsPdrSyncTime = currentTime
                }
            }

            // Always update the current GPS position for display using WGS84-compliant conversion
            _currentLatLng.postValue(CoordinateTransform.toLatLng(gpsCoordinate))
        }
    }

    private fun handleLocationAccuracyUpdate(accuracy: Float) {
        Log.d(TAG, "GPS accuracy: ${accuracy}m")
    }

    private fun initializeSensors() {
        // Initialize sensors
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        Log.d(TAG, "Step detector sensor: $stepDetectorSensor")
        useSoftwareStepDetection = stepDetectorSensor == null
    }

    override fun startSensors() {
        // Register sensors with appropriate sampling rates
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        gyroscopeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        magnetometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        gravitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        linearAccelerationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }

        pressureSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        stepDetectorSensor?.let {
            val registered = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Step detector listener registered: $registered")
            useSoftwareStepDetection = !registered
        } ?: run {
            Log.w(TAG, "Step detector sensor not available on this device! Using software step detection.")
            useSoftwareStepDetection = true
        }

        // Start location updates
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // 1 second
                1f,    // 1 meter
                locationListener
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
        }
    }

    override fun stopSensors() {
        sensorManager.unregisterListener(this)
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission not granted", e)
        }
    }

    // Recording control
    override fun startRecording() {
        // Not implemented in PdrProcessor, so leave empty or add custom logic if needed
    }

    override fun stopRecording() {
        // Not implemented in PdrProcessor, so leave empty or add custom logic if needed
    }

    override fun resetPdr() {
        pdrProcessor.resetPdr()
    }

    override fun setReferencePosition(latitude: Double, longitude: Double, altitude: Double) {
        // Validate WGS84 coordinates
        if (!CoordinateTransform.isValidWgs84(latitude, longitude)) {
            Log.e(TAG, "Invalid WGS84 coordinates for reference position: $latitude, $longitude")
            return
        }

        this.refLat = latitude
        this.refLon = longitude
        this.refAlt = altitude

        Log.d(TAG, "Set WGS84 reference position: $latitude, $longitude, $altitude")

        // Convert to ENU coordinates using WGS84-compliant transformation
        val referenceGps = CoordinateTransform.GpsCoordinate(refLat, refLon, refAlt)
        val currentGps = CoordinateTransform.GpsCoordinate(latitude, longitude, altitude)
        val enuCoords = CoordinateTransform.gpsToEnu(currentGps, referenceGps)

        pdrProcessor.setInitialPosition(enuCoords.east.toFloat(), enuCoords.north.toFloat())
    }

    override fun setStepLength(stepLength: Float) {
        pdrProcessor.setManualStepLength(stepLength)
    }

    override fun useManualStepLength(useManual: Boolean) {
        pdrProcessor.useManualStepLength(useManual)
    }

    override fun setFloorHeight(heightMeters: Int) {
        pdrProcessor.setFloorHeight(heightMeters.toFloat())
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()
        val sensorType = event.sensor.type

        // Track sensor update frequency
        lastEventTimestamps[sensorType]?.let { lastTimestamp ->
            val gap = currentTime - lastTimestamp
            if (gap > LARGE_GAP_THRESHOLD_MS) {
                Log.w(TAG, "Large time gap for sensor $sensorType: $gap ms")
            }
        }

        lastEventTimestamps[sensorType] = currentTime
        eventCounts[sensorType] = (eventCounts[sensorType] ?: 0) + 1

        when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> {
//                Log.i(TAG, "PDR_SENSOR: ACCELEROMETER event: values=${event.values.joinToString()}, timestamp=${event.timestamp}")
                System.arraycopy(event.values, 0, acceleration, 0, 3)
                accelMagnitude = kotlin.math.sqrt(
                    acceleration[0] * acceleration[0] +
                    acceleration[1] * acceleration[1] +
                    acceleration[2] * acceleration[2]
                )
                _accelerometerData.postValue(
                    AccelerometerData(
                        acceleration[0],
                        acceleration[1],
                        acceleration[2],
                        event.timestamp
                    )
                )

                // Buffer magnitude for PDR
                accelMagnitudes.add(accelMagnitude)

                // Temporarily disable software step detection
//                if (useSoftwareStepDetection) {
//                    val now = System.currentTimeMillis()
//                    // Detect peak
//                    if (accelMagnitude > STEP_THRESHOLD && lastAccelValue <= STEP_THRESHOLD && (now - lastAccelPeakTime) > STEP_MIN_TIME_MS) {
//                        lastAccelPeakTime = now
//                        lastStepAccel = accelMagnitude
//                        Log.d(TAG, "Software step detected at $now, accelMagnitude=$accelMagnitude")
//                        // Use same logic as hardware step detector
//                        val stepTime = SystemClock.uptimeMillis() - bootTime
//                        val headingRad = orientation[0]
//                        val magnitudes = accelMagnitudes.toList()
//                        val newPosition = pdrProcessor.updatePdrPosition(
//                            stepTime,
//                            magnitudes,
//                            headingRad
//                        )
//                        accelMagnitudes.clear()
//                        val latLng = CoordinateTransform.enuToGeodetic(
//                            newPosition.x.toDouble(),
//                            newPosition.y.toDouble(),
//                            0.0,
//                            refLat, refLon, refAlt
//                        )
//                        _currentLatLng.postValue(latLng)
//                        stepCounter++
//                        val stepData = StepData(
//                            stepTime,
//                            newPosition.stepLength,
//                            headingRad,
//                            stepCounter
//                        )
//                        _stepDetected.postValue(stepData)
//                        Log.d(TAG, "Software StepData posted: $stepData")
//                    }
//                    lastAccelValue = accelMagnitude
//                }
            }

            Sensor.TYPE_LINEAR_ACCELERATION -> {
//                Log.i(TAG, "PDR_SENSOR: LINEAR_ACCELERATION event: values=${event.values.joinToString()}, timestamp=${event.timestamp}")
                System.arraycopy(event.values, 0, linearAcceleration, 0, 3)
                _linearAccelerationData.postValue(
                    LinearAccelerationData(
                        linearAcceleration[0],
                        linearAcceleration[1],
                        linearAcceleration[2],
                        event.timestamp
                    )
                )
            }

            Sensor.TYPE_GRAVITY -> {
//                Log.i(TAG, "PDR_SENSOR: GRAVITY event: values=${event.values.joinToString()}, timestamp=${event.timestamp}")
                System.arraycopy(event.values, 0, gravity, 0, 3)
                _gravityData.postValue(
                    GravityData(
                        gravity[0],
                        gravity[1],
                        gravity[2],
                        event.timestamp
                    )
                )

                // Check elevator with new gravity data
                pdrProcessor.estimateElevator(gravity, linearAcceleration)
            }

            Sensor.TYPE_GYROSCOPE -> {
//                Log.i(TAG, "PDR_SENSOR: GYROSCOPE event: values=${event.values.joinToString()}, timestamp=${event.timestamp}")
                System.arraycopy(event.values, 0, angularVelocity, 0, 3)
                _gyroscopeData.postValue(
                    GyroscopeData(
                        angularVelocity[0],
                        angularVelocity[1],
                        angularVelocity[2],
                        event.timestamp
                    )
                )
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
//                Log.i(TAG, "PDR_SENSOR: MAGNETIC_FIELD event: values=${event.values.joinToString()}, timestamp=${event.timestamp}")
                System.arraycopy(event.values, 0, magneticField, 0, 3)
                _magnetometerData.postValue(
                    MagnetometerData(
                        magneticField[0],
                        magneticField[1],
                        magneticField[2],
                        event.timestamp
                    )
                )
            }

            Sensor.TYPE_ROTATION_VECTOR -> {
//                Log.i(TAG, "PDR_SENSOR: ROTATION_VECTOR event: values=${event.values.joinToString()}, timestamp=${event.timestamp}")
                System.arraycopy(event.values, 0, rotation, 0, event.values.size.coerceAtMost(4))
                _rotationVectorData.postValue(
                    RotationVectorData(
                        rotation[0],
                        rotation[1],
                        rotation[2],
                        rotation[3],
                        event.timestamp
                    )
                )

                // Calculate orientation angles
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, rotation)
                SensorManager.getOrientation(rotationMatrix, orientation)
            }

            Sensor.TYPE_PRESSURE -> {
//                Log.i(TAG, "PDR_SENSOR: PRESSURE event: values=${event.values.joinToString()}, timestamp=${event.timestamp}")
                pressure = event.values[0]
                _pressureData.postValue(
                    PressureData(
                        pressure,
                        event.timestamp
                    )
                )

                // Calculate elevation from pressure
                val altitude = SensorManager.getAltitude(
                    SensorManager.PRESSURE_STANDARD_ATMOSPHERE,
                    pressure
                )
                pdrProcessor.updateElevation(altitude)

                // Update floor if needed
                _currentFloor.value = pdrProcessor.floorLevel.value
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                Log.i(TAG, "PDR_SENSOR: STEP_DETECTOR event received: values=${event.values?.joinToString()}, timestamp=${event.timestamp}, currentTime=$currentTime, lastStepTime=$lastStepTime")

                lastStepTime = currentTime

                // Get current orientation (heading)
                val headingRad = orientation[0]

                // Update PDR position using buffered magnitudes
                val magnitudes = accelMagnitudes.toList()
                Log.d(TAG, "Step detected: headingRad=$headingRad, accelMagnitudes=$magnitudes, stepTime=${SystemClock.uptimeMillis() - bootTime}")
                val newPosition = pdrProcessor.updatePdrPosition(
                    SystemClock.uptimeMillis() - bootTime,
                    magnitudes,
                    headingRad
                )
                Log.d(TAG, "PDR updated: newPosition=$newPosition")
                accelMagnitudes.clear()

                // Convert ENU coordinates back to WGS84 latitude/longitude using proper transformation
                val referenceGps = CoordinateTransform.GpsCoordinate(refLat, refLon, refAlt)
                val enuCoord = CoordinateTransform.EnuCoordinate(
                    east = newPosition.x.toDouble(),
                    north = newPosition.y.toDouble(),
                    up = 0.0
                )
                val resultGps = CoordinateTransform.enuToGps(enuCoord, referenceGps)
                val latLng = CoordinateTransform.toLatLng(resultGps)

                Log.d(TAG, "WGS84 ENU to GPS: enu=(${newPosition.x},${newPosition.y}), gps=(${resultGps.latitude},${resultGps.longitude}), ref=($refLat,$refLon,$refAlt)")

                // Update current LatLng
                _currentLatLng.postValue(latLng)

                // Create step event
                stepCounter++
                val stepData = StepData(
                    SystemClock.uptimeMillis() - bootTime,
                    newPosition.stepLength,
                    headingRad,
                    stepCounter
                )
                _stepDetected.postValue(stepData)
                Log.d(TAG, "StepData posted: $stepData")
                Log.d(TAG, "Step detected: $stepCounter, heading: ${Math.toDegrees(headingRad.toDouble())}°")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not used but required by SensorEventListener
    }
}
