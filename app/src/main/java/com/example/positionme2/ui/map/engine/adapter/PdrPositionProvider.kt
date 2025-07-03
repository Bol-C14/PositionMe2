package com.example.positionme2.ui.map.engine.adapter

import androidx.lifecycle.Observer
import com.example.positionme2.domain.model.PdrPosition
import com.example.positionme2.domain.sensor.SensorFusionService
import com.example.positionme2.domain.pdr.PdrProcessor
import com.example.positionme2.ui.map.domain.Point
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdrPositionProvider @Inject constructor(
    private val sensorFusionService: SensorFusionService,
    private val pdrProcessor: PdrProcessor
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
            // Get the current GPS position from PDR processor (which handles ENU to WGS84 conversion)
            val gpsPosition = pdrProcessor.getCurrentGpsPosition()

            if (gpsPosition != null) {
                _position.value = Point(
                    latitude = gpsPosition.latitude,
                    longitude = gpsPosition.longitude,
                    timestamp = pdrPosition.timestamp
                )
            } else {
                // Fallback: if no GPS reference available, use current LatLng from sensor fusion service
                val currentLatLng = sensorFusionService.currentLatLng.value
                if (currentLatLng != null) {
                    _position.value = Point(
                        latitude = currentLatLng.latitude,
                        longitude = currentLatLng.longitude,
                        timestamp = pdrPosition.timestamp
                    )
                }
            }
        }
    }
}
