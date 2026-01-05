// LocationFragment.kt
package com.app.sensorlogger.Sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.app.sensorlogger.R
import com.app.sensorlogger.data.Prefs
import com.app.sensorlogger.data.RunExport
import com.app.sensorlogger.data.RunManager
import com.app.sensorlogger.model.LocationSample
import com.app.sensorlogger.model.ProviderVariant
import com.app.sensorlogger.model.RouteType
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationFragment : Fragment() {

    private val sharedVM by lazy {
        androidx.lifecycle.ViewModelProvider(requireActivity())
            .get(com.app.sensorlogger.viewmodel.SharedLocationViewModel::class.java)
    }

    private lateinit var locationManager: LocationManager

    private lateinit var textLat: TextView
    private lateinit var textLng: TextView
    private lateinit var textAlt: TextView
    private lateinit var textAcc: TextView

    private lateinit var textviewb: TextView
    private lateinit var textviewl: TextView
    private lateinit var textviewh: TextView
    private lateinit var textviewg: TextView

    private lateinit var textProvider: TextView
    private lateinit var textStatus: TextView

    private lateinit var btnRecord: ImageButton
    private lateinit var btnPause: ImageButton
    private lateinit var btnExport: ImageButton

    private val locationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private var lastExportedFilePath: String? = null

    private fun handleLocation(loc: Location) {
        val sample = LocationSample(
            timestamp = System.currentTimeMillis(),
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracy = if (loc.hasAccuracy()) loc.accuracy else null,
            altitude = if (loc.hasAltitude()) loc.altitude else null,
            provider = loc.provider ?: ""
        )

        // 1) Immer speichern, wenn Run läuft
        if (RunManager.isLogging) {
            RunManager.addLocation(sample)
        }

        // 2) Live an alle UIs senden
        sharedVM.currentLocation.postValue(sample)
    }

    private val fusedCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            handleLocation(loc)
        }
    }

    private val legacyLocationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            handleLocation(loc)
        }

        override fun onProviderDisabled(provider: String) {
            Toast.makeText(requireContext(), "$provider deaktiviert", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_location, container, false)

        textLat = view.findViewById(R.id.textlat)
        textLng = view.findViewById(R.id.textlng)
        textAlt = view.findViewById(R.id.textalt)
        textAcc = view.findViewById(R.id.textacc)
        textProvider = view.findViewById(R.id.textprovider)
        textStatus = view.findViewById(R.id.textstatus)

        textviewb = view.findViewById(R.id.textViewb)
        textviewl = view.findViewById(R.id.textViewl)
        textviewh = view.findViewById(R.id.textViewh)
        textviewg = view.findViewById(R.id.textViewg)

        btnRecord = view.findViewById(R.id.btn_record)
        btnPause = view.findViewById(R.id.btn_pause)
        btnExport = view.findViewById(R.id.btn_export)

        locationManager = requireContext()
            .getSystemService(Context.LOCATION_SERVICE) as LocationManager

        sharedVM.currentLocation.observe(viewLifecycleOwner) { s ->

            textLat.text = "Breite: %.6f".format(s.latitude)
            textLng.text = "Länge: %.6f".format(s.longitude)

            textAlt.text = if (s.altitude != null) {
                "Höhe: %.1f m".format(s.altitude)
            } else {
                "Höhe: -- m"
            }

            textAcc.text = if (s.accuracy != null) {
                "Genauigkeit: %.1f m".format(s.accuracy)
            } else {
                "Genauigkeit: -- m"
            }

            // ✅ Anzeige: gewählter Provider (Run) + Android Provider (loc.provider)
            val chosen = RunManager.getCurrentRun()?.providerVariant?.name ?: "?"
            textProvider.text = "Provider: $chosen (android: ${s.provider})"

            textStatus.text = if (RunManager.isLogging) {
                "Status: Logging läuft"
            } else {
                "Status: Standort empfangen"
            }
        }

        @SuppressLint("MissingPermission")
        btnRecord.setOnClickListener {
            if (!ensureLocationPermission()) {
                Toast.makeText(requireContext(), "Berechtigung benötigt…", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)

            // ✅ robust: trim + lowercase
            val mode = prefs.getString(Prefs.KEY_PROVIDER_MODE, Prefs.MODE_FUSED_HIGH)
                ?.trim()
                ?.lowercase()

            val providerVariant = when (mode) {
                Prefs.MODE_FUSED_BALANCED -> ProviderVariant.FUSED_BALANCED
                Prefs.MODE_GPS -> ProviderVariant.GPS
                Prefs.MODE_NETWORK -> ProviderVariant.NETWORK
                else -> ProviderVariant.FUSED_HIGH
            }

            val routeMode = prefs.getString(Prefs.KEY_ROUTE_MODE, Prefs.MODE_OUTDOOR)
                ?.trim()
                ?.lowercase()

            val route = when (routeMode) {
                Prefs.MODE_INDOOR -> RouteType.INDOOR
                else -> RouteType.OUTDOOR
            }

            if (!RunManager.isRunning()) {
                val run = RunManager.startNewRun(route, providerVariant)

                // START updates
                startSelectedProvider()

                textStatus.text = "Status: Run läuft (${run.runId.take(8)})"
                Toast.makeText(
                    requireContext(),
                    "Run gestartet: ${route.name} / ${providerVariant.name}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // ✅ RESUME: Run-Konfiguration an aktuelle Einstellungen anpassen
                RunManager.updateCurrentRunConfig(route, providerVariant)

                RunManager.resumeLogging()

                // Updates wieder starten (mit neuem Provider aus Prefs)
                startSelectedProvider()

                val run = RunManager.getCurrentRun()
                textStatus.text = "Status: Run läuft (${run?.runId?.take(8) ?: "?"})"
                Toast.makeText(
                    requireContext(),
                    "Run fortgesetzt: ${route.name} / ${providerVariant.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnPause.setOnClickListener {
            RunManager.pauseLogging()
            stopLocationUpdates()
            textStatus.text = "Status: angehalten"
            Toast.makeText(requireContext(), "Standort-Logging pausiert", Toast.LENGTH_SHORT).show()
        }

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

        return view
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onResume() {
        super.onResume()
        if (RunManager.isLogging && ensureLocationPermission()) {
            startSelectedProvider()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!RunManager.isLogging) {
            stopLocationUpdates()
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startFusedrovider(priority: Int) {
        val req = LocationRequest.Builder(
            priority,
            1000L
        )
            .setMinUpdateIntervalMillis(500L)
            .setWaitForAccurateLocation(false)
            .build()

        if (!ensureLocationPermission()) return

        locationClient.requestLocationUpdates(
            req,
            fusedCallback,
            requireActivity().mainLooper
        )

        textStatus.text = "Status: Fused aktiv"
    }

    private fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(fusedCallback)
        locationManager.removeUpdates(legacyLocationListener)
        textStatus.text = "Status: angehalten"
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startSelectedProvider() {
        val prefs = requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)

        // ✅ robust: trim + lowercase
        val mode = prefs.getString(Prefs.KEY_PROVIDER_MODE, Prefs.MODE_FUSED_HIGH)
            ?.trim()
            ?.lowercase()

        stopLocationUpdates()

        when (mode) {
            Prefs.MODE_GPS -> startGpsProvider()
            Prefs.MODE_NETWORK -> startNetworkProvider()
            Prefs.MODE_FUSED_BALANCED -> startFusedrovider(priority = Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            else -> startFusedrovider(priority = Priority.PRIORITY_HIGH_ACCURACY)
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startGpsProvider() {
        stopLocationUpdates()

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(requireContext(), "GPS ist deaktiviert", Toast.LENGTH_SHORT).show()
            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            1000L,
            0f,
            legacyLocationListener
        )

        textStatus.text = "Status: GPS aktiv"
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startNetworkProvider() {
        stopLocationUpdates()

        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(requireContext(), "Netzwerk-Provider deaktiviert", Toast.LENGTH_SHORT).show()
            return
        }

        locationManager.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            1000L,
            0f,
            legacyLocationListener
        )

        textStatus.text = "Status: Netzwerk aktiv"
    }

    private fun ensureLocationPermission(): Boolean {
        val ctx = requireContext()
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)

        return if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                99
            )
            false
        }
    }
}
