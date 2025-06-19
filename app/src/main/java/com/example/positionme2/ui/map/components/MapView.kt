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
import com.example.positionme2.ui.map.engine.MapEngine
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun MapView(
    mapEngine: MapEngine
) {
    val currentPosition by mapEngine.currentPosition.collectAsState()
    val trajectories by mapEngine.trajectories.collectAsState()
    val regionsOfInterest by mapEngine.regionsOfInterest.collectAsState()
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
            currentPosition?.let {
                Marker(
                    state = MarkerState(position = LatLng(it.latitude, it.longitude)),
                    title = "Current Position"
                    // Default marker, custom icon can be added here if you provide a bitmap descriptor
                )
            }

            trajectories.forEach { trajectory ->
                Polyline(
                    points = trajectory.points.map { LatLng(it.latitude, it.longitude) },
                )
            }

            regionsOfInterest.forEach { roi ->
                Marker(
                    state = MarkerState(position = LatLng(roi.point.latitude, roi.point.longitude)),
                    title = roi.name,
                    snippet = roi.description
                )
            }
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
