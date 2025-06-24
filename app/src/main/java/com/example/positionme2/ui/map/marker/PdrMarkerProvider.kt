package com.example.positionme2.ui.map.marker

import com.example.positionme2.ui.map.engine.adapter.PdrPositionProvider
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdrMarkerProvider @Inject constructor(
    pdrPositionProvider: PdrPositionProvider
) : MarkerProvider {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override val markers: Flow<List<MapMarker>> = pdrPositionProvider.position.map { position ->
        listOfNotNull(
            position?.let {
                MapMarker(
                    id = "pdr",
                    position = LatLng(it.latitude, it.longitude),
                    title = "PDR Position",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )
            }
        )
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val type: MarkerType = MarkerType.DYNAMIC
}
