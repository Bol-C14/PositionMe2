package com.example.positionme2.data.model

import com.google.android.gms.maps.model.LatLng

data class IndoorPoi(
    val position: LatLng,
    val title: String,
    val snippet: String?
)

