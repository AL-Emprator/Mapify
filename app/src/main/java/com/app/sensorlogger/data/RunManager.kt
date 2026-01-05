package com.app.sensorlogger.data
import com.app.sensorlogger.model.LocationSample
import java.util.UUID
import com.app.sensorlogger.model.ProviderVariant
import com.app.sensorlogger.model.RouteType
import com.app.sensorlogger.model.RunSession
import com.app.sensorlogger.model.WayPointHit

// RunManager.kt

object RunManager {

    private var currentRun: RunSession? = null
    private val allRuns: MutableList<RunSession> = mutableListOf()

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

    fun pauseLogging() {
        isLogging = false
    }

    fun resumeLogging() {
        if (currentRun != null) isLogging = true
    }

    // ✅ NEU: Beim Resume aktuelle Settings in den bestehenden Run übernehmen
    fun updateCurrentRunConfig(route: RouteType, providerVariant: ProviderVariant) {
        currentRun?.route = route
        currentRun?.providerVariant = providerVariant
    }

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


