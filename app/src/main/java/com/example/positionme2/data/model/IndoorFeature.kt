package com.example.positionme2.data.model

import com.google.android.gms.maps.model.LatLng

sealed class IndoorFeature {
    data class PolygonFeature(val points: List<LatLng>, val strokeColor: Int, val fillColor: Int) : IndoorFeature()
    data class PolylineFeature(val points: List<LatLng>, val color: Int) : IndoorFeature()
}

