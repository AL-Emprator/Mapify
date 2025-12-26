package com.app.sensorlogger.model



data class LocationSample(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val altitude: Double?,
    val provider: String = ""
)
