package com.example.positionme2.data.model

import com.google.android.gms.maps.model.LatLngBounds

data class IndoorMap(
    val id: String,
    val buildingId: String,
    val floor: Int,
    val imageUrl: String?, // For raster overlays
    val imageBounds: LatLngBounds?, // For raster overlays
    val vectorFeatures: List<IndoorFeature> = emptyList(), // For vector overlays
    val pois: List<IndoorPoi> = emptyList()
)

