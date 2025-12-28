package com.app.sensorlogger.model

import com.app.sensorlogger.Sensors.LocationFragment

enum class RouteType { OUTDOOR, INDOOR }
enum class ProviderVariant { FUSED_HIGH, FUSED_BALANCED, GPS, NETWORK }

data class RunSession (

    val runId: String,
    val route: RouteType,
    val providerVariant: ProviderVariant,
    val startTime: Long,
    var endTime: Long? = null,
    val locations: MutableList<LocationSample> = mutableListOf(),
    val waypoints: MutableList<WayPointHit> = mutableListOf()

)