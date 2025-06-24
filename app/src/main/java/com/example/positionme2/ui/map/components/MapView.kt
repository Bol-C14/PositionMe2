package com.example.positionme2.ui.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import com.google.android.gms.maps.CameraUpdateFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.positionme2.domain.recording.RecordingService
import com.example.positionme2.ui.map.engine.MapEngine
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.livedata.observeAsState
import com.example.positionme2.ui.map.marker.OptimizedMarkerRegistry

@Composable
fun MapView(
    mapEngine: MapEngine,
    recordingService: RecordingService,
    sensorFusionService: com.example.positionme2.domain.sensor.SensorFusionService,
    markerRegistry: OptimizedMarkerRegistry
) {
    val currentPosition by mapEngine.currentPosition.collectAsState()
    val trajectories by mapEngine.trajectories.collectAsState()
    val indoorMap by mapEngine.indoorMap.collectAsState()

    // Efficiently collect static and dynamic markers separately
    // Static markers are collected with lower frequency for better performance
    val staticMarkers by markerRegistry.staticMarkers.collectAsState()

    // Dynamic markers (positions) are collected with higher frequency
    val dynamicMarkers by markerRegistry.dynamicMarkers.collectAsState()

    // For debug overlay only
    val pdrPosition by sensorFusionService.pdrPosition.observeAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(40.7128, -74.0060), 10f)
    }
    val coroutineScope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        ) {
            // Render static markers (less frequent updates)
            staticMarkers.forEach { marker ->
                Marker(
                    state = MarkerState(position = marker.position),
                    title = marker.title,
                    snippet = marker.snippet,
                    icon = marker.icon,
                    visible = marker.visible
                )
            }

            // Render dynamic markers (frequent position updates)
            dynamicMarkers.forEach { marker ->
                Marker(
                    state = MarkerState(position = marker.position),
                    title = marker.title,
                    snippet = marker.snippet,
                    icon = marker.icon,
                    visible = marker.visible
                )
            }

            trajectories.forEach { trajectory ->
                Polyline(
                    points = trajectory.points.map { LatLng(it.latitude, it.longitude) },
                )
            }

            indoorMap?.let { indoorMapData ->
                indoorMapData.imageBounds?.let {
                    GroundOverlay(
                        position = GroundOverlayPosition.create(latLngBounds = it),
                        image = BitmapDescriptorFactory.fromAsset(indoorMapData.imageUrl!!),
                        transparency = 0.5f
                    )
                }
                indoorMapData.pois.forEach { poi ->
                    Marker(
                        state = MarkerState(position = poi.position),
                        title = poi.title,
                        snippet = poi.snippet
                    )
                }
            }
        }

        // Show PDR XY as debug text overlay
        pdrPosition?.let { pdr ->
            androidx.compose.material3.Text(
                text = "PDR XY: x=${pdr.x}, y=${pdr.y}",
                color = Color.Blue,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
            )
        }

        FloatingActionButton(
            onClick = {
                currentPosition?.let {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(
                                LatLng(it.latitude, it.longitude),
                                cameraPositionState.position.zoom
                            )
                        )
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = "Recenter",
                tint = Color.White
            )
        }
    }
}
