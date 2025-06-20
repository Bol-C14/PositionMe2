package com.example.positionme2.ui.map.engine

import com.example.positionme2.data.model.IndoorMap
import com.example.positionme2.ui.map.domain.Point
import com.example.positionme2.ui.map.domain.RegionOfInterest
import com.example.positionme2.ui.map.domain.Trajectory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeMapEngine : MapEngine {
    override val currentPosition: StateFlow<Point?> = MutableStateFlow(null)
    override val trajectories: StateFlow<List<Trajectory>> = MutableStateFlow(emptyList())
    override val regionsOfInterest: StateFlow<List<RegionOfInterest>> = MutableStateFlow(emptyList())
    override val indoorMap: StateFlow<IndoorMap?> = MutableStateFlow(null)

    override fun startTracking() {}
    override fun stopTracking() {}
    override fun startRecording() {}
    override fun stopRecording(): Trajectory? = null
    override fun replayTrajectory(trajectory: Trajectory) {}
    override fun addRegionOfInterest(point: Point, name: String, description: String) {}
    override fun switchMapLayer(layer: MapLayer) {}
    override fun setIndoorMap(buildingId: String, floor: Int) {}
}
