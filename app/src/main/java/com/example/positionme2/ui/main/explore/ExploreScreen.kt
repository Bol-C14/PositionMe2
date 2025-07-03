package com.example.positionme2.ui.main.explore

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.positionme2.domain.recording.RecordingService
import com.example.positionme2.domain.sensor.SensorFusionService
import com.example.positionme2.ui.map.components.MapView
import com.example.positionme2.ui.map.engine.GoogleMapEngine
import com.example.positionme2.ui.map.features.explore.ExploreFeatureManager
import com.example.positionme2.ui.map.marker.OptimizedMarkerRegistry

@Composable
fun ExploreScreen(
    exploreFeatureManager: ExploreFeatureManager,
    mapEngine: GoogleMapEngine,
    recordingService: RecordingService,
    sensorFusionService: SensorFusionService,
    markerRegistry: OptimizedMarkerRegistry
) {
    val currentPosition by exploreFeatureManager.currentPosition.collectAsState()

    LaunchedEffect(Unit) {
        exploreFeatureManager.startExplore()
    }

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

        // Search and navigation controls
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = { /* TODO: Handle search */ },
                    label = { Text("Search destination") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { /* TODO: Get current location */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("My Location")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { /* TODO: Start navigation */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Navigate")
                    }
                }
            }
        }
    }
}
