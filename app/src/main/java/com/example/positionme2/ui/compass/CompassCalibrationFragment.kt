package com.example.positionme2.ui.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.positionme2.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fragment implementation for compass calibration using the XML layout.
 * This is the traditional View-based approach as an alternative to the Compose implementation.
 */
class CompassCalibrationFragment : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null
    private var accelerometer: Sensor? = null

    // UI components
    private lateinit var tvCalibrationTitle: TextView
    private lateinit var ivCompassNeedle: ImageView
    private lateinit var circularProgress: CircularProgressView
    private lateinit var tvRotationCount: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var linearProgress: ProgressBar
    private lateinit var tvCalibrationStatus: TextView
    private lateinit var tvAccuracy: TextView
    private lateinit var layoutAccuracy: View

    // Calibration state
    private var calibrated = false
    private var accuracy = SensorManager.SENSOR_STATUS_UNRELIABLE
    private var progress = 0f
    private var rotations = 0
    private var rotationAngle = 0f
    private var lastAngle = 0f
    private var calibrationStep = 0

    // Sensor readings
    private val magneticFieldValues = FloatArray(3)
    private val accelerometerValues = FloatArray(3)
    private val orientation = FloatArray(3)
    private val rotationMatrix = FloatArray(9)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.screen_compass_calibration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI components
        tvCalibrationTitle = view.findViewById(R.id.tvCalibrationTitle)
        ivCompassNeedle = view.findViewById(R.id.ivCompassNeedle)
        circularProgress = view.findViewById(R.id.circularProgress)
        tvRotationCount = view.findViewById(R.id.tvRotationCount)
        tvInstructions = view.findViewById(R.id.tvInstructions)
        linearProgress = view.findViewById(R.id.linearProgress)
        tvCalibrationStatus = view.findViewById(R.id.tvCalibrationStatus)
        tvAccuracy = view.findViewById(R.id.tvAccuracy)
        layoutAccuracy = view.findViewById(R.id.layoutAccuracy)

        // Initialize sensors
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Start calibration process
        startCalibrationProcess()
    }

    private fun startCalibrationProcess() {
        // Reset calibration values
        progress = 0f
        rotations = 0
        calibrated = false

        viewLifecycleOwner.lifecycleScope.launch {
            // Step 0: Initial instructions
            calibrationStep = 0
            updateUI()
            delay(3000)

            // Step 1: Start calibration
            calibrationStep = 1
            updateUI()

            // Register sensor listeners
            sensorManager.registerListener(
                this@CompassCalibrationFragment,
                magnetometer,
                SensorManager.SENSOR_DELAY_GAME
            )
            sensorManager.registerListener(
                this@CompassCalibrationFragment,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )

            // Animate compass during calibration
            animateCompassInitially()

            // Wait until calibrated or timeout
            var timeElapsed = 0
            val timeout = 30000 // 30 seconds timeout
            while (!calibrated && timeElapsed < timeout) {
                delay(100)
                timeElapsed += 100
                updateProgressUI()
            }

            // Unregister listeners
            sensorManager.unregisterListener(this@CompassCalibrationFragment)

            // Step 2: Show results
            calibrationStep = 2
            updateUI()
            delay(1500)

            // Navigate based on results
            if (calibrated) {
                Toast.makeText(context, "Compass calibrated successfully", Toast.LENGTH_SHORT).show()
                val prefs = requireContext().getSharedPreferences("positionme_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("calibrated", true).apply()

                // Navigate to main screen
                findNavController().navigate(R.id.action_calibration_to_main)
            } else {
                Toast.makeText(context, "Compass accuracy too low. Try again.", Toast.LENGTH_LONG).show()
                // Reset for retry
                startCalibrationProcess()
            }
        }
    }

    private fun animateCompassInitially() {
        // Initial rotation animation for when user hasn't started moving the device yet
        val rotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 5000
            repeatCount = Animation.INFINITE
        }

        if (calibrationStep == 1) {
            ivCompassNeedle.startAnimation(rotateAnimation)
        }
    }

    private fun updateProgressUI() {
        // Update the progress bar
        circularProgress.progress = progress
        linearProgress.progress = (progress * 100).toInt()
        tvRotationCount.text = rotations.toString()

        // Update color of status text based on progress
        val statusColor = when {
            progress < 0.4f -> ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
            progress < 0.8f -> ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
            else -> ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
        }

        tvCalibrationStatus.setTextColor(statusColor)
        tvCalibrationStatus.text = when {
            progress < 0.4f -> "Keep rotating..."
            progress < 0.8f -> "Good progress, continue rotating"
            progress < 1.0f -> "Almost there!"
            else -> "Complete! Processing results..."
        }
    }

    private fun updateUI() {
        when (calibrationStep) {
            0 -> { // Initial state
                tvCalibrationTitle.text = "Compass Calibration"
                tvInstructions.text = "Prepare to calibrate your compass"
                tvCalibrationStatus.text = "Keep away from magnetic objects"
                layoutAccuracy.visibility = View.INVISIBLE
            }
            1 -> { // Calibration in progress
                tvCalibrationTitle.text = "Rotate Your Device"
                tvInstructions.text = "Rotate your device horizontally\n2-3 complete 360° rotations needed"
                updateProgressUI()
                layoutAccuracy.visibility = View.VISIBLE
            }
            2 -> { // Completed
                tvCalibrationTitle.text = if (calibrated) "Calibration Complete!" else "Calibration Failed"
                tvInstructions.text = if (calibrated) "Compass is now calibrated" else "Please try again in a different location"
                tvCalibrationStatus.text = if (calibrated) "✓ Successfully calibrated" else "× Calibration failed"
                tvCalibrationStatus.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (calibrated) android.R.color.holo_green_dark else android.R.color.holo_red_light
                    )
                )
                layoutAccuracy.visibility = View.INVISIBLE

                // Stop any animations
                ivCompassNeedle.clearAnimation()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magneticFieldValues, 0, event.values.size)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerValues, 0, event.values.size)
            }
        }

        SensorManager.getRotationMatrix(
            rotationMatrix, null,
            accelerometerValues, magneticFieldValues
        )
        SensorManager.getOrientation(rotationMatrix, orientation)

        // Convert radians to degrees
        val newAngle = Math.toDegrees(orientation[0].toDouble()).toFloat()
        rotationAngle = newAngle

        // Once we have real sensor data, stop the initial animation
        ivCompassNeedle.clearAnimation()

        // Rotate the compass needle
        ivCompassNeedle.rotation = rotationAngle

        // Track rotations
        if (calibrationStep >= 1) {
            if (lastAngle > 170f && newAngle < -170f) {
                // Crossed from positive to negative (clockwise rotation)
                progress += 0.125f // 1/8 of full rotation
            } else if (lastAngle < -170f && newAngle > 170f) {
                // Crossed from negative to positive (counter-clockwise rotation)
                progress += 0.125f
            }

            // Calculate full rotations (each full rotation is 0.5 progress)
            val fullRotations = (progress / 0.5f).toInt()
            if (fullRotations > rotations) {
                rotations = fullRotations
            }

            // Cap progress at 1.0 (2 full rotations)
            progress = minOf(progress, 1.0f)
        }

        lastAngle = newAngle
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracyValue: Int) {
        if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            accuracy = accuracyValue

            // Update accuracy text
            val accuracyText = when (accuracyValue) {
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
                else -> "Unreliable"
            }

            val accuracyColor = when (accuracyValue) {
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH ->
                    ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM ->
                    ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
                else ->
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
            }

            tvAccuracy.text = accuracyText
            tvAccuracy.setTextColor(accuracyColor)

            // Check if calibrated
            if (accuracyValue >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM) {
                if (progress >= 0.95f) {
                    calibrated = true
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume sensors if we're in calibration step
        if (calibrationStep == 1) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        // Always unregister sensors when not in foreground
        sensorManager.unregisterListener(this)
    }
}
