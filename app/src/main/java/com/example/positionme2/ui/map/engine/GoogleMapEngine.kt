package com.example.positionme2.ui.map.engine

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.example.positionme2.ui.map.domain.Point
import com.example.positionme2.ui.map.domain.RegionOfInterest
import com.example.positionme2.ui.map.domain.Trajectory
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationRequest.Builder
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GoogleMapEngine(context: Context) : MapEngine {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val _currentPosition = MutableStateFlow<Point?>(null)
    override val currentPosition: StateFlow<Point?> = _currentPosition
    override val trajectories = MutableStateFlow<List<Trajectory>>(emptyList())
    override val regionsOfInterest = MutableStateFlow<List<RegionOfInterest>>(emptyList())
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var isTracking = false
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location: Location? = result.lastLocation
            location?.let { updatePosition(it) }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startTracking() {
        if (isTracking) return
        isTracking = true
        val request = Builder(1000L)
            .setPriority(PRIORITY_HIGH_ACCURACY)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, null)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let { updatePosition(it) }
        }
    }

    override fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updatePosition(location: Location) {
        coroutineScope.launch {
            _currentPosition.value = Point(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                timestamp = location.time
            )
        }
    }

    override fun startRecording() {}
    override fun stopRecording(): Trajectory? = null
    override fun replayTrajectory(trajectory: Trajectory) {}
    override fun addRegionOfInterest(point: Point, name: String, description: String) {}
    override fun switchMapLayer(layer: MapLayer) {}
}
