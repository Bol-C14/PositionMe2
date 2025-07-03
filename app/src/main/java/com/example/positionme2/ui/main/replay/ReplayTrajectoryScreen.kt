package com.example.positionme2.ui.main.replay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.positionme2.ui.map.features.replay.ReplayFeatureManager
import com.example.positionme2.ui.map.engine.GoogleMapEngine
import com.example.positionme2.ui.map.marker.OptimizedMarkerRegistry
import com.example.positionme2.ui.map.components.MapView
import com.example.positionme2.domain.recording.RecordingService
import com.example.positionme2.domain.sensor.SensorFusionService

@Composable
fun ReplayTrajectoryScreen(
    replayFeatureManager: ReplayFeatureManager,
    mapEngine: GoogleMapEngine,
    markerRegistry: OptimizedMarkerRegistry,
    recordingService: RecordingService,
    sensorFusionService: SensorFusionService
) {
    val availableTrajectories by replayFeatureManager.availableTrajectories.collectAsState()
    val isReplaying by replayFeatureManager.isReplaying.collectAsState()
    var selectedTrajectory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedTrajectory) {
        if (selectedTrajectory != null) {
            replayFeatureManager.startReplay(selectedTrajectory!!)
        } else {
            replayFeatureManager.stopReplay()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedTrajectory != null) {
            // Map view during replay
            MapView(
                mapEngine = mapEngine,
                recordingService = recordingService,
                sensorFusionService = sensorFusionService,
                markerRegistry = markerRegistry
            )

            // Replay controls
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
                    IconButton(
                        onClick = { selectedTrajectory = null }
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Back to list")
                    }

                    IconButton(
                        onClick = {
                            if (isReplaying) {
                                replayFeatureManager.pauseReplay()
                            } else {
                                replayFeatureManager.startReplay(selectedTrajectory!!)
                            }
                        }
                    ) {
                        Icon(
                            if (isReplaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isReplaying) "Pause" else "Play"
                        )
                    }
                }
            }
        } else {
            // Recording list view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Recorded Trajectories",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn {
                    items(availableTrajectories) { filename ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = filename,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )

                                Row {
                                    IconButton(
                                        onClick = { selectedTrajectory = filename }
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                                    }

                                    // Optionally add a delete button if you implement deletion
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
