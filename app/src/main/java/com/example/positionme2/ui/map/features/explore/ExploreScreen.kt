package com.example.positionme2.ui.map.features.explore

import androidx.compose.runtime.Composable
import com.example.positionme2.ui.map.components.MapView
import com.example.positionme2.ui.map.engine.MapEngine

@Composable
fun ExploreScreen(mapEngine: MapEngine) {
    MapView(mapEngine = mapEngine)
}

