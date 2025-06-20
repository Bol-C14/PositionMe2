package com.example.positionme2.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.Observer
import com.example.positionme2.domain.model.*
import com.example.positionme2.domain.recording.RecordingService
import com.example.positionme2.domain.sensor.SensorFusionService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sensorFusionService: SensorFusionService
) : RecordingService {

    private var trajectory: Trajectory? = null
    override var isRecording = false
        private set

    private var startTimestampNanos: Long = 0

    private val motionObserver = Observer<Any> { if (isRecording) recordMotionSample() }
    private val magObserver = Observer<MagnetometerData> { if (isRecording) recordPositionSample(it) }
    private val pressureObserver = Observer<PressureData> { if (isRecording) recordPressureSample(it) }
    private val pdrObserver = Observer<PdrPosition> { if (isRecording) recordPdrSample(it) }
    private val gnssObserver = Observer<GnssData> { if (isRecording) recordGnssSample(it) }

    override fun startRecording() {
        if (isRecording) return

        startTimestampNanos = 0L
        trajectory = Trajectory(startTimestamp = System.currentTimeMillis())
        getSensorInfo()

        sensorFusionService.accelerometerData.observeForever(motionObserver)
        sensorFusionService.gyroscopeData.observeForever(motionObserver)
        sensorFusionService.rotationVectorData.observeForever(motionObserver)
        sensorFusionService.stepDetected.observeForever(motionObserver)
        sensorFusionService.magnetometerData.observeForever(magObserver)
        sensorFusionService.pressureData.observeForever(pressureObserver)
        sensorFusionService.pdrPosition.observeForever(pdrObserver)
        sensorFusionService.gnssData.observeForever(gnssObserver)

        isRecording = true
    }

    override fun stopRecording() {
        if (!isRecording) return
        isRecording = false

        sensorFusionService.accelerometerData.removeObserver(motionObserver)
        sensorFusionService.gyroscopeData.removeObserver(motionObserver)
        sensorFusionService.rotationVectorData.removeObserver(motionObserver)
        sensorFusionService.stepDetected.removeObserver(motionObserver)
        sensorFusionService.magnetometerData.removeObserver(magObserver)
        sensorFusionService.pressureData.removeObserver(pressureObserver)
        sensorFusionService.pdrPosition.removeObserver(pdrObserver)
        sensorFusionService.gnssData.removeObserver(gnssObserver)
    }

    override fun getTrajectory(): Trajectory? {
        return trajectory
    }

    private fun getSensorInfo() {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)
        trajectory?.sensorInfos = sensorList.map {
            SensorInfo(
                name = it.name,
                vendor = it.vendor,
                resolution = it.resolution,
                power = it.power,
                version = it.version,
                type = it.type
            )
        }
    }

    private fun getRelativeTimestamp(eventTimestampNanos: Long): Long {
        if (startTimestampNanos == 0L) {
            startTimestampNanos = eventTimestampNanos
        }
        return (eventTimestampNanos - startTimestampNanos) / 1_000_000
    }

    private fun recordMotionSample() {
        val accData = sensorFusionService.accelerometerData.value ?: return
        val gyrData = sensorFusionService.gyroscopeData.value ?: return
        val rotVecData = sensorFusionService.rotationVectorData.value ?: return
        val stepData = sensorFusionService.stepDetected.value

        val relativeTimestamp = getRelativeTimestamp(accData.timestamp)

        val sample = MotionSample(
            relativeTimestamp = relativeTimestamp,
            accX = accData.x,
            accY = accData.y,
            accZ = accData.z,
            gyrX = gyrData.x,
            gyrY = gyrData.y,
            gyrZ = gyrData.z,
            rotationVectorX = rotVecData.x,
            rotationVectorY = rotVecData.y,
            rotationVectorZ = rotVecData.z,
            rotationVectorW = rotVecData.w,
            stepCount = stepData?.stepCount ?: 0
        )
        trajectory?.motionSamples?.add(sample)
    }

    private fun recordPositionSample(data: MagnetometerData) {
        val relativeTimestamp = getRelativeTimestamp(data.timestamp)
        val sample = PositionSample(
            relativeTimestamp = relativeTimestamp,
            magX = data.x,
            magY = data.y,
            magZ = data.z
        )
        trajectory?.positionSamples?.add(sample)
    }

    private fun recordPressureSample(data: PressureData) {
        val relativeTimestamp = getRelativeTimestamp(data.timestamp)
        val sample = PressureSample(
            relativeTimestamp = relativeTimestamp,
            pressure = data.pressure
        )
        trajectory?.pressureSamples?.add(sample)
    }

    private fun recordPdrSample(data: PdrPosition) {
        val relativeTimestamp = getRelativeTimestamp(data.timestamp)
        val sample = PdrSample(
            relativeTimestamp = relativeTimestamp,
            x = data.x,
            y = data.y
        )
        trajectory?.pdrSamples?.add(sample)
    }

    private fun recordGnssSample(data: GnssData) {
        val relativeTimestamp = getRelativeTimestamp(data.timestamp)
        val sample = GnssSample(
            relativeTimestamp = relativeTimestamp,
            latitude = data.latitude,
            longitude = data.longitude,
            altitude = data.altitude,
            accuracy = data.accuracy,
            speed = 0f, // TODO: Get from GnssData
            provider = "" // TODO: Get from GnssData
        )
        trajectory?.gnssSamples?.add(sample)
    }
}
