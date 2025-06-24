package com.example.positionme2.ui.map.engine

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
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
import com.example.positionme2.ui.map.engine.adapter.FusedPositionProvider
import com.example.positionme2.ui.map.engine.adapter.GnssPositionProvider
import com.example.positionme2.ui.map.engine.adapter.PdrPositionProvider

class GoogleMapEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val indoorMapRepository: IndoorMapRepository,
    private val gnssPositionProvider: GnssPositionProvider,
    private val pdrPositionProvider: PdrPositionProvider,
    private val fusedPositionProvider: FusedPositionProvider
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

    init {
        coroutineScope.launch {
            gnssPositionProvider.position.collect { point ->
                if (positionTypeVisibility[PositionType.GNSS] == true) {
                    _currentPosition.value = point
                }
            }
        }
        coroutineScope.launch {
            pdrPositionProvider.position.collect { point ->
                if (positionTypeVisibility[PositionType.PDR] == true) {
                    _pdrPosition.value = point
                    _currentPosition.value = point
                }
            }
        }
        coroutineScope.launch {
            fusedPositionProvider.position.collect { point ->
                if (positionTypeVisibility[PositionType.FUSED] == true) {
                    _fusedPosition.value = point
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun startTracking() {
        if (isTracking) return
        isTracking = true

        // Start location updates
        gnssPositionProvider.start()
        pdrPositionProvider.start()
        fusedPositionProvider.start()
    }

    override fun stopTracking() {
        if (!isTracking) return
        isTracking = false

        // Stop location updates
        gnssPositionProvider.stop()
        pdrPositionProvider.stop()
        fusedPositionProvider.stop()
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
