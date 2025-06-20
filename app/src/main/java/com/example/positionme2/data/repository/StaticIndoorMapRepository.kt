package com.example.positionme2.data.repository

import com.example.positionme2.data.model.IndoorMap
import com.example.positionme2.data.model.IndoorPoi
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import javax.inject.Inject

class StaticIndoorMapRepository @Inject constructor() : IndoorMapRepository {
    override suspend fun getIndoorMap(buildingId: String, floor: Int): IndoorMap {
        // In a real app, you would fetch this from a remote or local data source.
        return IndoorMap(
            id = "sample_map",
            buildingId = buildingId,
            floor = floor,
            imageUrl = "asset_map.png",
            imageBounds = LatLngBounds(
                LatLng(40.7128, -74.0060), // Southwest
                LatLng(40.7138, -74.0050)  // Northeast
            ),
            pois = listOf(
                IndoorPoi(LatLng(40.7133, -74.0055), "Entrance", "Main entrance"),
                IndoorPoi(LatLng(40.7130, -74.0058), "Restroom", null)
            )
        )
    }
}

