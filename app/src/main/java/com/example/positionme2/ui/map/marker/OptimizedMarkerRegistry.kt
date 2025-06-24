package com.example.positionme2.ui.map.marker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A registry that efficiently manages static and dynamic markers separately.
 * Static markers are updated less frequently to improve performance.
 */
@Singleton
class OptimizedMarkerRegistry @Inject constructor(
    providers: Set<@JvmSuppressWildcards MarkerProvider>
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    // Group providers by type
    private val staticProviders = providers.filter { it.type == MarkerType.STATIC }
    private val dynamicProviders = providers.filter { it.type == MarkerType.DYNAMIC }

    /**
     * Static markers that update infrequently (POIs, floor markers, etc.)
     * Using a longer timeout for better performance with static content.
     */
    val staticMarkers: StateFlow<List<MapMarker>> = if (staticProviders.isNotEmpty()) {
        combine(staticProviders.map { it.markers }.toList()) { markerLists ->
            markerLists.flatMap { it }
        }.stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(30000), // 30 second timeout for static content
            emptyList()
        )
    } else {
        MutableStateFlow(emptyList())
    }

    /**
     * Dynamic markers that update frequently (position markers)
     * Using a shorter timeout for real-time responsiveness.
     */
    val dynamicMarkers: StateFlow<List<MapMarker>> = if (dynamicProviders.isNotEmpty()) {
        combine(dynamicProviders.map { it.markers }.toList()) { markerLists ->
            markerLists.flatMap { it }
        }.stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(5000), // 5 second timeout for dynamic content
            emptyList()
        )
    } else {
        MutableStateFlow(emptyList())
    }
}
