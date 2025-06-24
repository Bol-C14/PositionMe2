package com.example.positionme2.ui.map.marker

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A registry that collects all available marker providers and combines their markers into a single flow.
 * This allows for a decoupled, plug-and-play system for adding new markers to the map.
 */
@Singleton
class MarkerProviderRegistry @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards MarkerProvider>
) {
    /**
     * A flow that emits a list of all currently visible markers from all registered providers.
     */
    val markers: Flow<List<MapMarker>> = combine(
        providers.map { it.markers }.toList()
    ) { markerLists ->
        markerLists.flatMap { it }
    }
}
