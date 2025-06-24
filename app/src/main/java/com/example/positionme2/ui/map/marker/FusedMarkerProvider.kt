package com.example.positionme2.ui.map.marker

import com.example.positionme2.ui.map.engine.adapter.FusedPositionProvider
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FusedMarkerProvider @Inject constructor(
    fusedPositionProvider: FusedPositionProvider
) : MarkerProvider {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    // Convert the single marker to a list for compatibility with the interface
    override val markers: Flow<List<MapMarker>> = fusedPositionProvider.position.map { position ->
        listOfNotNull(
            position?.let {
                MapMarker(
                    id = "fused",
                    position = LatLng(it.latitude, it.longitude),
                    title = "Fused Position",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                )
            }
        )
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val type: MarkerType = MarkerType.DYNAMIC
}
