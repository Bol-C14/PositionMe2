package com.example.positionme2.ui.map.features.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.positionme2.domain.recording.RecordingService
import com.example.positionme2.domain.sensor.SensorFusionService
import com.example.positionme2.ui.map.components.MapView
import com.example.positionme2.ui.map.engine.MapEngine
import com.example.positionme2.ui.map.marker.OptimizedMarkerRegistry

@Composable
fun ExploreScreen(
    mapEngine: MapEngine,
    recordingService: RecordingService,
    sensorFusionService: SensorFusionService,
    markerRegistry: OptimizedMarkerRegistry
) {
    LaunchedEffect(Unit) {
        sensorFusionService.startSensors()
    }
    MapView(
        mapEngine = mapEngine,
        recordingService = recordingService,
        sensorFusionService = sensorFusionService,
        markerRegistry = markerRegistry
    )
}
