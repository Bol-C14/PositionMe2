package com.example.positionme2.ui.map.marker

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * A data class representing the state of a marker on the map.
 */
data class MapMarker(
    val id: String,
    val position: LatLng,
    val title: String,
    val snippet: String? = null,
    val icon: BitmapDescriptor? = null,
    val visible: Boolean = true
)

enum class MarkerType {
    STATIC,
    DYNAMIC
}

/**
 * An interface for providers that supply a list of map markers.
 * This allows for a decoupled, plug-and-play system for different marker sources.
 */
interface MarkerProvider {
    /**
     * A flow that emits the current list of markers.
     */
    val markers: Flow<List<MapMarker>>

    /**
     * The type of markers provided, used for optimization.
     */
    val type: MarkerType
}
