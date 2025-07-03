package com.example.positionme2.ui.map.engine

import android.annotation.SuppressLint
import android.content.Context
import com.example.positionme2.data.repository.IndoorMapRepository
import com.example.positionme2.ui.map.domain.Point
import com.example.positionme2.ui.map.domain.RegionOfInterest
import com.example.positionme2.ui.map.domain.Trajectory
import com.example.positionme2.ui.map.features.TrajectoryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.example.positionme2.data.model.IndoorMap

class GoogleMapEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val indoorMapRepository: IndoorMapRepository,
    private val trajectoryManager: TrajectoryManager
) : MapEngine {

    // Position state flows - these will be provided by feature managers
    private val _currentPosition = MutableStateFlow<Point?>(null)
    override val currentPosition: StateFlow<Point?> = _currentPosition

    private val _indoorMap = MutableStateFlow<IndoorMap?>(null)
    override val indoorMap: StateFlow<IndoorMap?> = _indoorMap.asStateFlow()

    // Trajectories come from trajectory manager
    override val trajectories: StateFlow<List<Trajectory>> = trajectoryManager.activeTrajectories
    override val regionsOfInterest = MutableStateFlow<List<RegionOfInterest>>(emptyList())

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    // Method to update current position (called by feature managers)
    fun updateCurrentPosition(position: Point?) {
        _currentPosition.value = position
    }

    @SuppressLint("MissingPermission")
    override fun startTracking() {
        // MapEngine no longer manages tracking directly
        // This is now handled by feature managers
    }

    override fun stopTracking() {
        // MapEngine no longer manages tracking directly
        // This is now handled by feature managers
    }

    override fun addRegionOfInterest(point: Point, name: String, description: String) {
        val currentRois = regionsOfInterest.value.toMutableList()
        currentRois.add(RegionOfInterest(
            id = UUID.randomUUID().toString(),
            point = point,
            name = name,
            description = description
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

    // Simplified recording methods - these delegate to feature managers
    override fun startRecording() {
        // This method is now handled by RecordFeatureManager
        // Kept for interface compatibility
    }

    override fun stopRecording(): Trajectory? {
        // This method is now handled by RecordFeatureManager
        // Kept for interface compatibility
        return null
    }

    override fun replayTrajectory(trajectory: Trajectory) {
        // This method is now handled by ReplayFeatureManager
        // Kept for interface compatibility
    }
}
