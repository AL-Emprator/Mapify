package com.app.sensorlogger.data
import com.app.sensorlogger.model.LocationSample
import java.util.UUID
import com.app.sensorlogger.model.ProviderVariant
import com.app.sensorlogger.model.RouteType
import com.app.sensorlogger.model.RunSession
import com.app.sensorlogger.model.WayPointHit

object RunManager {

    private var currentRun: RunSession? = null
    private val allRuns: MutableList<RunSession> = mutableListOf()

    // NEW: echter Status
    var isLogging: Boolean = false
        private set

    fun startNewRun(route: RouteType, providerVariant: ProviderVariant): RunSession {
        val run = RunSession(
            runId = UUID.randomUUID().toString(),
            route = route,
            providerVariant = providerVariant,
            startTime = System.currentTimeMillis()
        )
        currentRun = run
        allRuns.add(run)
        isLogging = true
        return run
    }

    // NEW: pausieren ohne Run zu beenden
    fun pauseLogging() {
        isLogging = false
    }

    // NEW: weiterlaufen lassen (wenn Run existiert)
    fun resumeLogging() {
        if (currentRun != null) isLogging = true
    }

    // Run wirklich beenden
    fun stopRun() {
        currentRun?.endTime = System.currentTimeMillis()
        currentRun = null
        isLogging = false
    }

    fun isRunning(): Boolean = currentRun != null
    fun getCurrentRun(): RunSession? = currentRun

    fun addLocation(sample: LocationSample) {
        if (isLogging) currentRun?.locations?.add(sample)
    }

    fun addWaypoint(hit: WayPointHit) {
        currentRun?.waypoints?.add(hit)
    }

    fun getRuns(): List<RunSession> = allRuns.toList()

    fun clearAll() {
        currentRun = null
        allRuns.clear()
        isLogging = false
    }
}

