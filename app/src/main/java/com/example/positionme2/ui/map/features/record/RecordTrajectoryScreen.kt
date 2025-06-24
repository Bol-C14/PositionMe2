package com.example.positionme2.ui.map.features.record

import androidx.compose.runtime.Composable
import com.example.positionme2.ui.map.engine.MapEngine
import com.example.positionme2.ui.map.marker.OptimizedMarkerRegistry
import com.example.positionme2.ui.map.components.MapView
import com.example.positionme2.domain.recording.RecordingService
import com.example.positionme2.domain.sensor.SensorFusionService
import javax.inject.Inject

@Composable
fun RecordTrajectoryScreen(
    mapEngine: MapEngine,
    markerRegistry: OptimizedMarkerRegistry,
    recordingService: RecordingService,
    sensorFusionService: SensorFusionService
) {
    MapView(
        mapEngine = mapEngine,
        recordingService = recordingService,
        sensorFusionService = sensorFusionService,
        markerRegistry = markerRegistry
    )
}
