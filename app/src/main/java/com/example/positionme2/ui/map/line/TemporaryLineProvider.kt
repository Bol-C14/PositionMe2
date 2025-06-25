package com.example.positionme2.ui.map.line

import android.graphics.Color
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides temporary lines for selection, measurement, or other temporary visualizations.
 * These are frequently updated but have a very short lifespan.
 */
@Singleton
class TemporaryLineProvider @Inject constructor() : LineProvider {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val _temporaryLines = MutableStateFlow<List<MapLine>>(emptyList())

    override val lines: Flow<List<MapLine>> = _temporaryLines
        .stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(1000), // Very short timeout for temporary content
            emptyList()
        )

    override val type: LineType = LineType.TEMPORARY

    /**
     * Add a temporary measurement line between two points
     */
    fun showMeasurementLine(startPoint: LatLng, endPoint: LatLng, measurementText: String) {
        val newLine = MapLine(
            id = "measurement_${System.currentTimeMillis()}",
            points = listOf(startPoint, endPoint),
            width = 2f,
            color = Color.YELLOW,
            zIndex = 3f
        )

        _temporaryLines.value = listOf(newLine)
    }

    /**
     * Add a temporary selection line showing a path between points
     */
    fun showSelectionPath(points: List<LatLng>) {
        if (points.size < 2) return

        val newLine = MapLine(
            id = "selection_${System.currentTimeMillis()}",
            points = points,
            width = 4f,
            color = Color.parseColor("#4CAF50"), // Material green
            zIndex = 3f
        )

        _temporaryLines.value = listOf(newLine)
    }

    /**
     * Clear all temporary lines
     */
    fun clearTemporaryLines() {
        _temporaryLines.value = emptyList()
    }
}
