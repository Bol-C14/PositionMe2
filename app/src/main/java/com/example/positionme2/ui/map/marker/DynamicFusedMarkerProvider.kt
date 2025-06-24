package com.example.positionme2.ui.map.marker

import com.example.positionme2.ui.map.engine.adapter.FusedPositionProvider
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
class DynamicFusedMarkerProvider @Inject constructor(
    fusedPositionProvider: FusedPositionProvider
) : MarkerProvider {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

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
