package com.example.positionme2.ui.map.engine

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.Observer
import com.example.positionme2.data.repository.IndoorMapRepository
import com.example.positionme2.domain.model.PdrPosition
import com.example.positionme2.domain.sensor.SensorFusionService
import com.example.positionme2.ui.map.domain.Point
import com.example.positionme2.ui.map.domain.PositionType
import com.example.positionme2.ui.map.domain.RegionOfInterest
import com.example.positionme2.ui.map.domain.Trajectory
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationRequest.Builder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.example.positionme2.utils.CoordinateTransform
import com.example.positionme2.data.model.IndoorMap

class GoogleMapEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sensorFusionService: SensorFusionService,
    private val indoorMapRepository: IndoorMapRepository
) : MapEngine {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    // Position state flows
    private val _currentPosition = MutableStateFlow<Point?>(null)
    override val currentPosition: StateFlow<Point?> = _currentPosition

    private val _pdrPosition = MutableStateFlow<Point?>(null)
    val pdrPosition: StateFlow<Point?> = _pdrPosition

    private val _wifiPosition = MutableStateFlow<Point?>(null)
    val wifiPosition: StateFlow<Point?> = _wifiPosition

    private val _fusedPosition = MutableStateFlow<Point?>(null)
    val fusedPosition: StateFlow<Point?> = _fusedPosition

    private val _indoorMap = MutableStateFlow<IndoorMap?>(null)
    override val indoorMap: StateFlow<IndoorMap?> = _indoorMap.asStateFlow()

    override val trajectories = MutableStateFlow<List<Trajectory>>(emptyList())
    override val regionsOfInterest = MutableStateFlow<List<RegionOfInterest>>(emptyList())

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var isTracking = false

    // Visibility flags for different position types
    private val positionTypeVisibility = mutableMapOf<PositionType, Boolean>(
        PositionType.GNSS to true,
        PositionType.PDR to true,
        PositionType.WIFI to true,
        PositionType.FUSED to true
    )

    // Location callback for GNSS updates
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location: Location? = result.lastLocation
            location?.let { updateGnssPosition(it) }
        }
    }

    // Observers for PDR updates
    private val pdrObserver = Observer<PdrPosition> { pdrPosition ->
        updatePdrPosition(pdrPosition)
    }

    // Observer for current LatLng (usually from sensor fusion)
    private val latLngObserver = Observer<LatLng> { latLng ->
        updateFusedPosition(latLng)
    }

    init {
        // Set up observers for the SensorFusionService LiveData
        sensorFusionService.pdrPosition.observeForever(pdrObserver)
        sensorFusionService.currentLatLng.observeForever(latLngObserver)
    }

    @SuppressLint("MissingPermission")
    override fun startTracking() {
        if (isTracking) return
        isTracking = true

        // Start GNSS location updates
        val request = Builder(1000L)
            .setPriority(PRIORITY_HIGH_ACCURACY)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, null)
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let { updateGnssPosition(it) }
        }
    }

    override fun stopTracking() {
        if (!isTracking) return
        isTracking = false

        // Stop GNSS location updates
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateGnssPosition(location: Location) {
        if (positionTypeVisibility[PositionType.GNSS] == false) return

        coroutineScope.launch {
            _currentPosition.value = Point(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                timestamp = location.time
            )
        }
    }

    private fun updatePdrPosition(pdrPosition: PdrPosition) {
        if (positionTypeVisibility[PositionType.PDR] == false) return

        coroutineScope.launch {
            // Convert ENU coordinates (relative) to a LatLng at origin reference
            val latLng = CoordinateTransform.enuToGeodetic(
                pdrPosition.x.toDouble(),
                pdrPosition.y.toDouble(),
                0.0,
                0.0, 0.0, 0.0
            )
            _pdrPosition.value = Point(
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                timestamp = pdrPosition.timestamp
            )
        }
    }

    private fun updateFusedPosition(latLng: LatLng) {
        if (positionTypeVisibility[PositionType.FUSED] == false) return

        coroutineScope.launch {
            _fusedPosition.value = Point(
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    override fun addRegionOfInterest(point: Point, name: String, description: String) {
        val currentRois = regionsOfInterest.value.toMutableList()
        currentRois.add(RegionOfInterest(
            id = UUID.randomUUID().toString(), point = point, name = name, description = description
        ))
        coroutineScope.launch {
            regionsOfInterest.value = currentRois
        }
    }

    override fun switchMapLayer(layer: MapLayer) {
        // Implementation would depend on your specific needs
    }

    override fun setIndoorMap(buildingId: String, floor: Int) {
        coroutineScope.launch {
            _indoorMap.value = indoorMapRepository.getIndoorMap(buildingId, floor)
        }
    }

    fun setPositionTypeVisible(type: PositionType, visible: Boolean) {
        positionTypeVisibility[type] = visible
        // Update positions immediately based on visibility changes
        when (type) {
            PositionType.PDR -> {
                if (!visible) {
                    _pdrPosition.value = null
                }
            }
            PositionType.GNSS -> {
                if (!visible) {
                    _currentPosition.value = null
                }
            }
            PositionType.FUSED -> {
                if (!visible) {
                    _fusedPosition.value = null
                }
            }
            PositionType.WIFI -> {
                if (!visible) {
                    _wifiPosition.value = null
                }
            }
        }
    }

    // Clean up observers when the engine is no longer used
    fun cleanup() {
        sensorFusionService.pdrPosition.removeObserver(pdrObserver)
        sensorFusionService.currentLatLng.removeObserver(latLngObserver)
    }

    override fun startRecording() {
        // TODO: Implement startRecording if needed, or leave empty if not used
    }

    override fun stopRecording(): com.example.positionme2.ui.map.domain.Trajectory? {
        return null
    }

    override fun replayTrajectory(trajectory: com.example.positionme2.ui.map.domain.Trajectory) {
        // TODO: Implement replayTrajectory if needed, or leave empty if not used
    }
}
