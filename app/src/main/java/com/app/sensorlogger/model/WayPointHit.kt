package com.app.sensorlogger.model

data class WayPointHit (

    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val note: String = "", // optional: "P1", "Ampel", etc.
    val provider: String
)
