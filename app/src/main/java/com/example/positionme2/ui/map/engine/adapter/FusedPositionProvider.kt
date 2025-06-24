package com.example.positionme2.ui.map.engine.adapter

import androidx.lifecycle.Observer
import com.example.positionme2.domain.sensor.SensorFusionService
import com.example.positionme2.ui.map.domain.Point
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FusedPositionProvider @Inject constructor(
    private val sensorFusionService: SensorFusionService
) : PositionProvider {

    private val _position = MutableStateFlow<Point?>(null)
    override val position: StateFlow<Point?> = _position

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val latLngObserver = Observer<LatLng> { latLng ->
        updateFusedPosition(latLng)
    }

    override fun start() {
        sensorFusionService.currentLatLng.observeForever(latLngObserver)
    }

    override fun stop() {
        sensorFusionService.currentLatLng.removeObserver(latLngObserver)
    }

    private fun updateFusedPosition(latLng: LatLng) {
        coroutineScope.launch {
            _position.value = Point(
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
