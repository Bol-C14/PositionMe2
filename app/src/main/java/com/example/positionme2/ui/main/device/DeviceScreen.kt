package com.example.positionme2.ui.main.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensors = remember { sensorManager.getSensorList(Sensor.TYPE_ALL) }
    val sensorReadings = remember { mutableStateMapOf<Int, FloatArray>() }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                sensorReadings[event.sensor.type] = event.values.clone()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensors.forEach { sensor ->
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Device Sensors") })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(sensors) { sensor ->
                val values = sensorReadings[sensor.type]?.joinToString() ?: "N/A"
                val isWakeUp = sensor.isWakeUpSensor
                val wakeupColor = if (isWakeUp) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                val wakeupTextColor = if (isWakeUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = wakeupColor),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Name: ${sensor.name}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(text = "Type: ${sensor.type}")
                        Text(text = "Vendor: ${sensor.vendor}")
                        Text(text = "Version: ${sensor.version}")
                        Text(text = "Max Range: ${sensor.maximumRange}")
                        Text(text = "Resolution: ${sensor.resolution}")
                        Text(text = "Wakeup: ${if (isWakeUp) "Yes" else "No"}", color = wakeupTextColor)
                        Text(text = "Current Values: $values")
                    }
                }
            }
        }
    }
}
