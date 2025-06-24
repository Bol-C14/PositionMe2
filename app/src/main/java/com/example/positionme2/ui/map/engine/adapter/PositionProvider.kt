package com.example.positionme2.ui.map.engine.adapter

import com.example.positionme2.ui.map.domain.Point
import kotlinx.coroutines.flow.StateFlow

interface PositionProvider {
    val position: StateFlow<Point?>
    fun start()
    fun stop()
}

