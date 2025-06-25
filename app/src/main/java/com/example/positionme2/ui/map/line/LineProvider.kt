package com.example.positionme2.ui.map.line

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.flow.Flow

/**
 * A data class representing the state of a line on the map.
 */
data class MapLine(
    val id: String,
    val points: List<LatLng>,
    val width: Float = 5f,
    val color: Int,
    val visible: Boolean = true,
    val zIndex: Float = 0f,
    val geodesic: Boolean = false,
    val clickable: Boolean = false
) {
    /**
     * Convert to Google Maps PolylineOptions for rendering
     */
    fun toPolylineOptions(): PolylineOptions {
        return PolylineOptions()
            .addAll(points)
            .width(width)
            .color(color)
            .visible(visible)
            .zIndex(zIndex)
            .geodesic(geodesic)
            .clickable(clickable)
    }
}

/**
 * Line types for different use cases and performance optimization
 */
enum class LineType {
    STATIC,      // Building boundaries, walls, static paths - rarely updated
    DYNAMIC,     // Current trajectory, active paths - frequently updated
    TEMPORARY    // Temporary visualization elements - very short lifetime
}

/**
 * An interface for providers that supply a list of map lines.
 * This allows for a decoupled, plug-and-play system for different line sources.
 */
interface LineProvider {
    /**
     * A flow that emits the current list of lines.
     */
    val lines: Flow<List<MapLine>>

    /**
     * The type of lines provided, used for optimization.
     */
    val type: LineType
}
