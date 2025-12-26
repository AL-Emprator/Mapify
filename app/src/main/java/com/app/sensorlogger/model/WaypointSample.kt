package com.app.sensorlogger.model

data class WaypointSample(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Float?,
    val provider: String
)