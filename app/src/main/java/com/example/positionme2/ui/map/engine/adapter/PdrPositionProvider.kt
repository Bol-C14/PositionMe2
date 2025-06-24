package com.example.positionme2.ui.map.engine.adapter

import androidx.lifecycle.Observer
import com.example.positionme2.domain.model.PdrPosition
import com.example.positionme2.domain.sensor.SensorFusionService
import com.example.positionme2.ui.map.domain.Point
import com.example.positionme2.utils.CoordinateTransform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdrPositionProvider @Inject constructor(
    private val sensorFusionService: SensorFusionService
) : PositionProvider {

    private val _position = MutableStateFlow<Point?>(null)
    override val position: StateFlow<Point?> = _position

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val pdrObserver = Observer<PdrPosition> { pdrPosition ->
        updatePdrPosition(pdrPosition)
    }

    override fun start() {
        sensorFusionService.pdrPosition.observeForever(pdrObserver)
    }

    override fun stop() {
        sensorFusionService.pdrPosition.removeObserver(pdrObserver)
    }

    private fun updatePdrPosition(pdrPosition: PdrPosition) {
        coroutineScope.launch {
            // Convert ENU coordinates (relative) to a LatLng at origin reference
            val latLng = CoordinateTransform.enuToGeodetic(
                pdrPosition.x.toDouble(),
                pdrPosition.y.toDouble(),
                0.0,
                0.0, 0.0, 0.0
            )
            _position.value = Point(
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                timestamp = pdrPosition.timestamp
            )
        }
    }
}
