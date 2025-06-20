package com.example.positionme2.data.repository

import com.example.positionme2.data.model.IndoorMap

interface IndoorMapRepository {
    suspend fun getIndoorMap(buildingId: String, floor: Int): IndoorMap?
}

