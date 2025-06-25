package com.example.positionme2.ui.map.line

import android.graphics.Color
import com.example.positionme2.data.repository.IndoorMapRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides lines for drawing interior walls on the map.
 * These are considered static since they rarely change.
 */
@Singleton
class WallLineProvider @Inject constructor(
    private val indoorMapRepository: IndoorMapRepository
) : LineProvider {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    // Using a simple StateFlow instead of directly depending on repository methods
    private val _lines = MutableStateFlow<List<MapLine>>(emptyList())

    override val lines: Flow<List<MapLine>> = _lines
        .stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(30000),
            emptyList()
        )

    override val type: LineType = LineType.STATIC

    /**
     * Update wall lines from repository data
     */
    fun updateWalls() {
        // This would typically be populated from repository data
        // For now, we'll just use an empty list to prevent compilation errors
        _lines.value = emptyList()

        // When you're ready to implement this with real data:
        // coroutineScope.launch {
        //     val floor = indoorMapRepository.getSelectedFloor() // or whatever method you have
        //     if (floor != null) {
        //         val wallLines = // transform floor wall data to MapLine objects
        //         _lines.value = wallLines
        //     }
        // }
    }
}
