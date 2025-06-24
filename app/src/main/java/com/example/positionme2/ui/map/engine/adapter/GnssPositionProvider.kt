package com.example.positionme2.ui.map.engine.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.example.positionme2.ui.map.domain.Point
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GnssPositionProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : PositionProvider {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val _position = MutableStateFlow<Point?>(null)
    override val position: StateFlow<Point?> = _position

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { updateGnssPosition(it) }
        }
    }

    @SuppressLint("MissingPermission")
    override fun start() {
        val request = LocationRequest.Builder(1000L)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let { updateGnssPosition(it) }
        }
    }

    override fun stop() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateGnssPosition(location: Location) {
        Log.d("GnssPositionProvider", "Received GNSS location: lat=${location.latitude}, lon=${location.longitude}, alt=${location.altitude}, time=${location.time}")
        coroutineScope.launch {
            _position.value = Point(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                timestamp = location.time
            )
        }
    }
}
