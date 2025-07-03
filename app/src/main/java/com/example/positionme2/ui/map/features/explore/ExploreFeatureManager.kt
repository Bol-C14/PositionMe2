package com.example.positionme2.ui.map.features.explore

import com.example.positionme2.ui.map.domain.Point
import com.example.positionme2.ui.map.domain.PositionType
import com.example.positionme2.ui.map.features.TrajectoryManager
import com.example.positionme2.ui.map.features.TrajectoryMode
import com.example.positionme2.ui.map.engine.adapter.FusedPositionProvider
import com.example.positionme2.ui.map.engine.adapter.GnssPositionProvider
import com.example.positionme2.ui.map.engine.adapter.PdrPositionProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manages explore mode - tracks current position without recording trajectory
 */
class ExploreFeatureManager @Inject constructor(
    private val trajectoryManager: TrajectoryManager,
    private val gnssPositionProvider: GnssPositionProvider,
    private val pdrPositionProvider: PdrPositionProvider,
    private val fusedPositionProvider: FusedPositionProvider
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var isActive = false

    // Current position for explore mode
    private val _currentPosition = MutableStateFlow<Point?>(null)
    val currentPosition: StateFlow<Point?> = _currentPosition.asStateFlow()

    // Position type visibility
    private val positionTypeVisibility = mutableMapOf<PositionType, Boolean>(
        PositionType.GNSS to true,
        PositionType.PDR to true,
        PositionType.WIFI to true,
        PositionType.FUSED to true
    )

    fun startExplore() {
        if (isActive) return
        isActive = true

        // Set trajectory manager to explore mode
        trajectoryManager.setMode(TrajectoryMode.EXPLORE)

        // Start position providers
        gnssPositionProvider.start()
        pdrPositionProvider.start()
        fusedPositionProvider.start()

        // Collect position updates
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
                    _currentPosition.value = point
                }
            }
        }

        coroutineScope.launch {
            fusedPositionProvider.position.collect { point ->
                if (positionTypeVisibility[PositionType.FUSED] == true) {
                    _currentPosition.value = point
                }
            }
        }
    }

    fun stopExplore() {
        if (!isActive) return
        isActive = false

        // Stop position providers
        gnssPositionProvider.stop()
        pdrPositionProvider.stop()
        fusedPositionProvider.stop()

        _currentPosition.value = null
    }

    fun setPositionTypeVisible(type: PositionType, visible: Boolean) {
        positionTypeVisibility[type] = visible
        trajectoryManager.setPositionTypeVisible(type, visible)

        // Clear current position if the active type is hidden
        if (!visible) {
            when (type) {
                PositionType.GNSS, PositionType.PDR, PositionType.FUSED -> {
                    if (positionTypeVisibility.values.none { it }) {
                        _currentPosition.value = null
                    }
                }
                else -> {}
            }
        }
    }

    fun isExploreActive(): Boolean = isActive
}
