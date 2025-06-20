package com.example.positionme2.ui.map.engine

import com.example.positionme2.data.model.IndoorMap
import com.example.positionme2.ui.map.domain.Point
import com.example.positionme2.ui.map.domain.RegionOfInterest
import com.example.positionme2.ui.map.domain.Trajectory
import kotlinx.coroutines.flow.StateFlow

interface MapEngine {
    val currentPosition: StateFlow<Point?>
    val trajectories: StateFlow<List<Trajectory>>
    val regionsOfInterest: StateFlow<List<RegionOfInterest>>
    val indoorMap: StateFlow<IndoorMap?>

    fun startTracking()
    fun stopTracking()
    fun startRecording()
    fun stopRecording(): Trajectory?
    fun replayTrajectory(trajectory: Trajectory)
    fun addRegionOfInterest(point: Point, name: String, description: String)
    fun switchMapLayer(layer: MapLayer)
    fun setIndoorMap(buildingId: String, floor: Int)
}

enum class MapLayer {
    INDOOR,
    OUTDOOR
}
