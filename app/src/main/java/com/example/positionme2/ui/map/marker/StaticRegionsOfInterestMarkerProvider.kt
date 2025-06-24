package com.example.positionme2.ui.map.marker

import com.example.positionme2.ui.map.engine.MapEngine
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaticRegionsOfInterestMarkerProvider @Inject constructor(
    mapEngine: MapEngine
) : MarkerProvider {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override val markers: Flow<List<MapMarker>> = mapEngine.regionsOfInterest.map { regions ->
        regions.map { roi ->
            MapMarker(
                id = "roi_${roi.id}",
                position = LatLng(roi.point.latitude, roi.point.longitude),
                title = roi.name,
                snippet = roi.description
            )
        }
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(30000), emptyList())

    // These markers rarely change, so they're STATIC
    override val type: MarkerType = MarkerType.STATIC
}
