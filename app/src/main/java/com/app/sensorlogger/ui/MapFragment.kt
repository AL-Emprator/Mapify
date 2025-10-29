package com.app.sensorlogger.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.app.sensorlogger.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

class MapFragment : Fragment() {

    private lateinit var mapView: MapView



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // osmdroid braucht einen User Agent, sonst beschwert es sich
        Configuration.getInstance().userAgentValue = requireContext().packageName

        val view = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = view.findViewById(R.id.osm_map)

        // Basiskarte vorbereiten
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(16.0)


        // 1. CSV laden -> Liste von GeoPoint
        val pathPoints = loadPathFromLastExport()

        // Wenn wir aufgezeichnete Punkte haben -> Route zeichnen
        if (pathPoints.isNotEmpty()) {
            // Kamera auf letzten Punkt
            val lastPoint = pathPoints.last()
            mapView.controller.setCenter(lastPoint)

            // Linie/Pfad
            val polyline = Polyline()
            polyline.setPoints(pathPoints)
            polyline.title = "Deine Route"
            mapView.overlays.add(polyline)

            // Marker am Start
            val startMarker = Marker(mapView)
            startMarker.position = pathPoints.first()
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            startMarker.title = "Start"
            mapView.overlays.add(startMarker)

            // Marker am Ende
            val endMarker = Marker(mapView)
            endMarker.position = pathPoints.last()
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            endMarker.title = "Ende"
            mapView.overlays.add(endMarker)
        } else {
            // Fallback: wenn keine Daten vorhanden
            val munich = GeoPoint(48.1351, 11.5820)
            mapView.controller.setCenter(munich)

            val marker = Marker(mapView)
            marker.position = munich
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Keine CSV gefunden"
            mapView.overlays.add(marker)
        }

        // (Optional) eigene aktuelle Position anzeigen, wenn Berechtigung OK ist
        enableMyLocationOverlayIfAllowed()

        return view
    }

    private fun enableMyLocationOverlayIfAllowed() {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                42
            )
            return
        }

        // Du kannst hier ein MyLocationOverlay von osmdroid hinzufügen.
        // (Kleines "blauer Punkt"-Overlay).
        // Dafür brauchst du zusätzlich osmdroid-wms / osmdroid-bonuspack oder MyLocationNewOverlay.
        // Minimalvariante (ohne extra libs): wir skippen den Blue-Dot und bleiben bei aufgezeichneten Markern.
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume() // wichtig für osmdroid (GPS, Compass etc.)
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause() // wichtig für osmdroid
    }


    // liest die NEUSTE locations_*.csv und baut daraus GeoPoints
    private fun loadPathFromLastExport(): List<GeoPoint> {
        val prefs = requireContext().getSharedPreferences("sensorlogger_prefs", Context.MODE_PRIVATE)
        val path = prefs.getString("last_gps_csv_path", null) ?: return emptyList()

        val file = File(path)
        if (!file.exists()) return emptyList()

        val points = mutableListOf<GeoPoint>()

        file.forEachLine { line ->
            // Header überspringen
            if (line.startsWith("timestamp")) return@forEachLine

            val parts = line.split(",")
            if (parts.size >= 3) {
                val lat = parts[1].toDoubleOrNull()
                val lng = parts[2].toDoubleOrNull()
                if (lat != null && lng != null) {
                    points.add(GeoPoint(lat, lng))
                }
            }
        }

        return points
    }

}
