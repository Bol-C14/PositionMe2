package com.example.positionme2.ui.map.line

import android.graphics.Color
import com.example.positionme2.ui.map.domain.Trajectory
import com.example.positionme2.ui.map.engine.MapEngine
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides lines for drawing trajectories on the map.
 */
@Singleton
class TrajectoryLineProvider @Inject constructor(
    mapEngine: MapEngine
) : LineProvider {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override val lines: Flow<List<MapLine>> = mapEngine.trajectories.map { trajectories ->
        trajectories.mapIndexed { index, trajectory ->
            mapTrajectoryToLine(trajectory, index)
        }
    }.stateIn(
        coroutineScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    override val type: LineType = LineType.DYNAMIC

    private fun mapTrajectoryToLine(trajectory: Trajectory, index: Int): MapLine {
        val points = trajectory.points.map { LatLng(it.latitude, it.longitude) }

        // Use different colors for different trajectories
        val color = when (index % 5) {
            0 -> Color.BLUE
            1 -> Color.RED
            2 -> Color.GREEN
            3 -> Color.MAGENTA
            else -> Color.CYAN
        }

        return MapLine(
            id = "trajectory_${trajectory.id}",
            points = points,
            width = 8f,
            color = color,
            zIndex = 1f,
            clickable = true
        )
    }
}
