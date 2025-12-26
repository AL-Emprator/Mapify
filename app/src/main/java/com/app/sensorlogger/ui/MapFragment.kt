package com.app.sensorlogger.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.app.sensorlogger.R
import com.app.sensorlogger.data.RunExport
import com.app.sensorlogger.data.RunManager
import com.app.sensorlogger.model.ProviderVariant
import com.app.sensorlogger.model.RouteType
import com.app.sensorlogger.model.WayPointHit
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var btnWaypoint: ImageButton
    private lateinit var btnExport: ImageButton

    // ====== Einstellungen ======
    private var followMe: Boolean = true   // <- wenn false: Kamera bleibt wie sie ist

    // ====== Location (Fused) ======
    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private var lastLocation: Location? = null
    private var liveMarker: Marker? = null

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            lastLocation = loc
            updateLiveMarker(loc)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Configuration.getInstance().userAgentValue = requireContext().packageName

        val view = inflater.inflate(R.layout.fragment_map, container, false)

        mapView = view.findViewById(R.id.osm_map)
        btnWaypoint = view.findViewById(R.id.btn_waypoint)
        btnExport = view.findViewById(R.id.btn_export)

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(18.0)

        // Route aus letzter exportierter CSV zeichnen (optional)
        val measuredPoints = loadPathFromLastExport()
        if (measuredPoints.isNotEmpty()) {
            val polyline = Polyline().apply {
                setPoints(measuredPoints)
                title = "Messroute"
            }
            mapView.overlays.add(polyline)

            val startMarker = Marker(mapView).apply {
                position = measuredPoints.first()
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Start"
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_start_green)
            }
            mapView.overlays.add(startMarker)

            val endMarker = Marker(mapView).apply {
                position = measuredPoints.last()
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Ende"
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_end_red)
            }
            mapView.overlays.add(endMarker)

            mapView.controller.setCenter(measuredPoints.last())
        } else {
            // Fallback Center (wird später durch GPS ersetzt)
            mapView.controller.setCenter(GeoPoint(48.1351, 11.5820))
        }

        btnWaypoint.setOnClickListener { onSaveWaypointClicked() }

        btnExport.setOnClickListener {
            val run = RunManager.getCurrentRun() ?: RunManager.getRuns().lastOrNull()
            if (run == null) {
                Toast.makeText(requireContext(), "Kein Run vorhanden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = RunExport.exportRunToCsv(requireContext(), run)

            val msg = buildString {
                append("Export:\n")
                append("Locations: ${result.locationsFile?.name ?: "FEHLT"}\n")
                append("Waypoints: ${result.waypointsFile?.name ?: "FEHLT"}")
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
        }

        // Permission check (fragt ggf. an)
        ensureLocationPermission()

        return view
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        startMapLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopMapLocationUpdates()
        mapView.onPause()
    }

    private fun onSaveWaypointClicked() {
        val loc = lastLocation
        if (loc == null) {
            Toast.makeText(requireContext(), "Warte auf GPS-Fix…", Toast.LENGTH_SHORT).show()
            return
        }

        // Wenn noch kein Run aktiv ist -> starte Run (einmalig)
        if (!RunManager.isRunning()) {
            val prefs = requireContext().getSharedPreferences("sensorlogger_prefs", Context.MODE_PRIVATE)

            val mode = prefs.getString("provieder_mode", "fused_high")
            val providerVariant = when (mode?.lowercase()) {
                "fused_balanced" -> ProviderVariant.FUSED_BALANCED
                "gps" -> ProviderVariant.GPS
                else -> ProviderVariant.FUSED_HIGH
            }

            val route = when (prefs.getString("route_mode", "outdoor")?.lowercase()) {
                "indoor" -> RouteType.INDOOR
                else -> RouteType.OUTDOOR
            }

            RunManager.startNewRun(route, providerVariant)
            Toast.makeText(requireContext(), "Kein Run aktiv → neuer Run gestartet", Toast.LENGTH_SHORT).show()
        }

        val wpIndex = (RunManager.getCurrentRun()?.waypoints?.size ?: 0) + 1

        val hit = WayPointHit(
            timestamp = System.currentTimeMillis(),
            latitude = loc.latitude,
            longitude = loc.longitude,
            note = "WP #$wpIndex",
            provider = loc.provider ?: "unknown"
        )

        RunManager.addWaypoint(hit)

        // Marker setzen
        val point = GeoPoint(hit.latitude, hit.longitude)
        val marker = Marker(mapView).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Waypoint #$wpIndex"
            subDescription = "t=${hit.timestamp}"
        }

        mapView.overlays.add(marker)

        if (followMe) mapView.controller.animateTo(point)

        mapView.invalidate()

        Toast.makeText(requireContext(), "Wegpunkt gespeichert (#$wpIndex)", Toast.LENGTH_SHORT).show()
    }

    private fun updateLiveMarker(loc: Location) {
        val p = GeoPoint(loc.latitude, loc.longitude)

        if (liveMarker == null) {
            liveMarker = Marker(mapView).apply {
                position = p
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Aktuell"
            }
            mapView.overlays.add(liveMarker)

            // Beim ersten Fix ggf. zentrieren
            if (followMe) {
                mapView.controller.setZoom(18.0)
                mapView.controller.setCenter(p)
            }
        } else {
            liveMarker!!.position = p
            if (followMe) mapView.controller.animateTo(p)
        }

        mapView.invalidate()
    }

    private fun startMapLocationUpdates() {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            ensureLocationPermission()
            return
        }

        val req = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L
        ).setMinUpdateIntervalMillis(500L).build()

        fusedClient.requestLocationUpdates(req, fusedCallback, requireActivity().mainLooper)
    }

    private fun stopMapLocationUpdates() {
        fusedClient.removeLocationUpdates(fusedCallback)
    }

    private fun ensureLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)

        return if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                42
            )
            false
        }
    }

    // liest die NEUSTE locations_*.csv (Pfad in last_gps_csv_path) und baut GeoPoints
    private fun loadPathFromLastExport(): List<GeoPoint> {
        val prefs = requireContext().getSharedPreferences("sensorlogger_prefs", Context.MODE_PRIVATE)
        val path = prefs.getString("last_gps_csv_path", null) ?: return emptyList()

        val file = File(path)
        if (!file.exists()) return emptyList()

        val points = mutableListOf<GeoPoint>()
        file.forEachLine { line ->
            if (line.startsWith("timestamp")) return@forEachLine
            val parts = line.split(",")
            if (parts.size >= 3) {
                val lat = parts[1].toDoubleOrNull()
                val lng = parts[2].toDoubleOrNull()
                if (lat != null && lng != null) points.add(GeoPoint(lat, lng))
            }
        }
        return points
    }
}
