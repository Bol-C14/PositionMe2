package com.example.positionme2.ui.map.marker

import com.example.positionme2.ui.map.engine.adapter.GnssPositionProvider
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
class DynamicGnssMarkerProvider @Inject constructor(
    gnssPositionProvider: GnssPositionProvider
) : MarkerProvider {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override val markers: Flow<List<MapMarker>> = gnssPositionProvider.position.map { position ->
        listOfNotNull(
            position?.let {
                MapMarker(
                    id = "gnss",
                    position = LatLng(it.latitude, it.longitude),
                    title = "GNSS Position",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
        )
    }.stateIn(coroutineScope, SharingStarted.WhileSubscribed(5000), emptyList())

    override val type: MarkerType = MarkerType.DYNAMIC
}
