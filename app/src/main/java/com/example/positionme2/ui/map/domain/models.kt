package com.example.positionme2.ui.map.domain

data class Trajectory(
    val id: String,
    val points: List<Point>,
    val name: String
)

data class Point(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val timestamp: Long
)

data class RegionOfInterest(
    val id: String,
    val point: Point,
    val name: String,
    val description: String
)

