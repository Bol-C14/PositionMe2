package com.example.positionme2.ui.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.math.*

@Composable
fun CompassCalibrationScreen(onCalibrated: () -> Unit = {}) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var calibrationStep by rememberSaveable { mutableStateOf(0) }
    var calibrated by rememberSaveable { mutableStateOf(false) }
    var progress by rememberSaveable { mutableStateOf(0f) }
    var rotations by rememberSaveable { mutableStateOf(0) }
    var rotationAngle by rememberSaveable { mutableStateOf(0f) }
    var lastAngle by rememberSaveable { mutableStateOf(0f) }
    var statusText by rememberSaveable { mutableStateOf("Keep rotating...") }
    var showRetry by rememberSaveable { mutableStateOf(false) }
    var sensorAccuracy by rememberSaveable { mutableStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE) }
    val animatedAngle by animateFloatAsState(targetValue = rotationAngle, label = "compass-needle")

    // Sensor state
    val magneticFieldValues = remember { FloatArray(3) }
    val accelerometerValues = remember { FloatArray(3) }
    val orientation = remember { FloatArray(3) }
    val rotationMatrix = remember { FloatArray(9) }

    // Calibration coroutine
    LaunchedEffect(calibrationStep) {
        if (calibrationStep == 0) {
            statusText = "Keep away from magnetic objects"
            delay(2000)
            calibrationStep = 1
        } else if (calibrationStep == 1) {
            // Start calibration
            progress = 0f
            rotations = 0
            calibrated = false
            showRetry = false
            // Wait for calibration or timeout
            var timeElapsed = 0
            val timeout = 30000
            while (!calibrated && timeElapsed < timeout) {
                delay(100)
                timeElapsed += 100
                statusText = when {
                    progress < 0.4f -> "Keep rotating..."
                    progress < 0.8f -> "Good progress, continue rotating"
                    progress < 1.0f -> "Almost there!"
                    else -> "Complete! Processing results..."
                }
                if (progress >= 1.0f) {
                    calibrated = true
                }
            }
            calibrationStep = 2
        } else if (calibrationStep == 2) {
            delay(1500)
            if (calibrated) {
                onCalibrated()
                return@LaunchedEffect // Do not reset state, let parent handle navigation
            } else {
                showRetry = true
            }
        }
    }

    // Sensor registration
    DisposableEffect(calibrationStep) {
        if (calibrationStep == 1) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    when (event.sensor.type) {
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(event.values, 0, magneticFieldValues, 0, event.values.size)
                        }
                        Sensor.TYPE_ACCELEROMETER -> {
                            System.arraycopy(event.values, 0, accelerometerValues, 0, event.values.size)
                        }
                    }
                    SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magneticFieldValues)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val newAngle = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    rotationAngle = newAngle
                    // Track rotations
                    if (lastAngle > 170f && newAngle < -170f) {
                        progress += 0.125f
                        rotations++
                    } else if (lastAngle < -170f && newAngle > 170f) {
                        progress += 0.125f
                        rotations++
                    }
                    lastAngle = newAngle
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    sensorAccuracy = accuracy
                }
            }
            sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(listener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME)
            onDispose {
                sensorManager.unregisterListener(listener)
            }
        } else {
            onDispose { }
        }
    }

    // UI
    val statusColor = when {
        progress < 0.4f -> MaterialTheme.colorScheme.error
        progress < 0.8f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val accuracyText = when (sensorAccuracy) {
        SensorManager.SENSOR_STATUS_UNRELIABLE -> "Unreliable"
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low"
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium"
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High"
        else -> "Unknown"
    }
    val accuracyColor = when (sensorAccuracy) {
        SensorManager.SENSOR_STATUS_UNRELIABLE -> Color(0xFFF44336)
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> Color(0xFFFF9800)
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Color(0xFFFFEB3B)
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Color(0xFF4CAF50)
        else -> Color.Gray
    }
    val arcBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val arcFgColor = statusColor
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = when (calibrationStep) {
                0 -> "Compass Calibration"
                1 -> "Rotate Your Device"
                else -> if (calibrated) "Calibration Complete!" else "Calibration Failed"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        // Compass + progress + rotation count
        Box(
            modifier = Modifier
                .size(280.dp)
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Circular progress
            Canvas(modifier = Modifier.size(280.dp)) {
                drawArc(
                    color = arcBgColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 16f)
                )
                drawArc(
                    color = arcFgColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 16f)
                )
            }
            // Compass needle
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_compass),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .rotate(animatedAngle),
                tint = MaterialTheme.colorScheme.primary
            )
            // Rotation count in a card
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.size(40.dp).align(Alignment.Center)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = rotations.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
        // Instructions/status card
        Card(
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (calibrationStep) {
                        0 -> "Prepare to calibrate your compass"
                        1 -> "Rotate your device horizontally\n2-3 complete 360° rotations needed"
                        else -> if (calibrated) "Compass is now calibrated" else "Please try again in a different location"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        // Sensor accuracy at the bottom
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .background(Color.Transparent),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Sensor Accuracy: ", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = accuracyText,
                color = accuracyColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (showRetry) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = { calibrationStep = 0 }) {
                Text("Retry Calibration")
            }
        }
    }
}
