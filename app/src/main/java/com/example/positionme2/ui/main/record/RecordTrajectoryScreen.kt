package com.example.positionme2.ui.main.record

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.positionme2.ui.map.features.record.RecordFeatureManager
import com.example.positionme2.ui.map.engine.GoogleMapEngine
import com.example.positionme2.ui.map.marker.OptimizedMarkerRegistry
import com.example.positionme2.ui.map.components.MapView
import com.example.positionme2.ui.main.RecordingStateManager
import com.example.positionme2.domain.recording.RecordingService
import com.example.positionme2.domain.sensor.SensorFusionService

@Composable
fun RecordTrajectoryScreen(
    recordingStateManager: RecordingStateManager? = null,
    recordFeatureManager: RecordFeatureManager,
    mapEngine: GoogleMapEngine,
    markerRegistry: OptimizedMarkerRegistry,
    recordingService: RecordingService,
    sensorFusionService: SensorFusionService
) {
    val currentPosition by recordFeatureManager.currentPosition.collectAsState()
    val isRecording by recordFeatureManager.isRecordingState.collectAsState()

    // Sync local recording state with global recording state
    LaunchedEffect(isRecording) {
        recordingStateManager?.setRecordingState(isRecording)
    }

    LaunchedEffect(Unit) {
        sensorFusionService.startSensors()
    }

    // Update map engine with current position
    LaunchedEffect(currentPosition) {
        mapEngine.updateCurrentPosition(currentPosition)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapView(
            mapEngine = mapEngine,
            recordingService = recordingService,
            sensorFusionService = sensorFusionService,
            markerRegistry = markerRegistry
        )

        // Recording controls
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isRecording) {
                    Button(
                        onClick = { recordFeatureManager.startRecording() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Start Recording")
                    }
                } else {
                    Button(
                        onClick = { recordFeatureManager.pauseRecording() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pause")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { recordFeatureManager.stopRecording() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Stop Recording")
                    }
                }
            }
        }
    }
}
