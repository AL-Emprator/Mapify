package com.app.sensorlogger.Sensors


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Environment
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
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class LocationFragment : Fragment() {

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

    private var isLogging = false

    // hier sammeln wir ALLE GPS-Punkte dieser Session
    private val loggedLocations = mutableListOf<Location>()

    // wir merken uns die zuletzt bekannte CSV-Datei,
    // damit MapFragment sie später laden kann
    private var lastExportedFilePath: String? = null

    // Location Callback -> bekommt Updates vom FusedLocationProvider
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            updateUiWithLocation(loc)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
            if (isLogging) {
              loggedLocations.add(loc)
            }
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


       // ==== Globale Einstellungen laden ====
       val prefs = requireContext().getSharedPreferences("sensorlogger_prefs", Context.MODE_PRIVATE)




       @SuppressLint("MissingPermission")
       btnRecord.setOnClickListener {
           // Permission zuerst prüfen
           if (ensureLocationPermission()) {
               isLogging = true
               loggedLocations.clear()
               startLocationUpdates()
               textStatus.text = "Status: Tracking läuft…"
               Toast.makeText(requireContext(), "Standort-Logging gestartet", Toast.LENGTH_SHORT).show()
           } else {
               // wir haben gerade Permission angefragt -> User muss erst erlauben
               Toast.makeText(requireContext(), "Berechtigung benötigt…", Toast.LENGTH_SHORT).show()
           }
       }


       btnPause.setOnClickListener {
           isLogging = false
           stopLocationUpdates()
           textStatus.text = "Status: angehalten"
           Toast.makeText(requireContext(), "Standort-Logging pausiert", Toast.LENGTH_SHORT).show()
       }


       btnExport.setOnClickListener {


          val file = exportToCsv()
           if (file != null) {
               lastExportedFilePath = file.absolutePath  // <- jetzt ist file sicher non-null
               Toast.makeText(
                   requireContext(),
                   "Exportiert nach: ${file.name}",
                   Toast.LENGTH_LONG
               ).show()

               //wir merken den Pfad global, damit MapFragment ihn laden kann
               saveLastExportPathForMap(file.absolutePath)
           }else{

               Toast.makeText(requireContext(), "Export fehlgeschlagen", Toast.LENGTH_SHORT).show()
           }
       }

        return view
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onResume() {
        super.onResume()
        // Falls du willst dass bei Zurückkommen weiter aufgenommen wird:
        if (isLogging && ensureLocationPermission()) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    // UI aktualisieren
// Status in der UI aktualisieren
    private fun updateUiWithLocation(loc: android.location.Location) {
        textLat.text = "Breite: %.6f".format(loc.latitude)
        textLng.text = "Länge: %.6f".format(loc.longitude)

        textAlt.text = if (loc.hasAltitude()) {
            "Höhe: %.1f m".format(loc.altitude)
        } else {
            "Höhe: -- m"
        }

        textAcc.text = if (loc.hasAccuracy()) {
            "Genauigkeit: %.1f m".format(loc.accuracy)
        } else {
            "Genauigkeit: -- m"
        }

        // Fused Provider ist quasi Mischung aus GPS / WLAN / Zelle
        textProvider.text = "Provider: fused"

        textStatus.text = "Status: Standort empfangen"

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        textviewb.text = String.format(timeStamp);
        textviewl.text = String.format(timeStamp);
        textviewh.text = String.format(timeStamp);
        textviewg.text = String.format(timeStamp);
    }

    // Startet kontinuierliche Updates mit FusedLocationProvider
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startLocationUpdates() {
        val req = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L // gewünschtes Intervall ~1 Sekunde
        )
            .setMinUpdateIntervalMillis(500L) // früheste Rate
            .setWaitForAccurateLocation(true)
            .build()

        if (!ensureLocationPermission()) return

        locationClient.requestLocationUpdates(
            req,
            callback,
            requireActivity().mainLooper
        )

        textStatus.text = "Status: Tracking läuft…"
    }

    private fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(callback)
        textStatus.text = "Status: angehalten"
    }


    private fun exportData() {
        val prefs = requireContext().getSharedPreferences("sensorlogger_prefs", Context.MODE_PRIVATE)
        val mode = prefs.getString("storage_mode", "csv")

        when (mode) {
            "cloud" -> exportToCloud()
            "json"  -> exportToJson()
            else    -> exportToCsv()
        }
    }

    private fun exportToCloud() {
        // TODO: hier z.B. REST / MQTT / etc.
        Toast.makeText(requireContext(), "Cloud-Upload (TODO)", Toast.LENGTH_SHORT).show()
    }

    private fun exportToJson() {
        // TODO: gleiche Daten wie CSV, aber als JSON-Datei schreiben
        Toast.makeText(requireContext(), "JSON Export (TODO)", Toast.LENGTH_SHORT).show()
    }


    // Exportiert geloggte Positionen als CSV

    private fun exportToCsv(): File? {
        if (loggedLocations.isEmpty()) {
            Toast.makeText(requireContext(), "Keine GPS-Daten aufgezeichnet", Toast.LENGTH_SHORT).show()
            return null
        }

        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (dir == null) {
            Toast.makeText(requireContext(), "Kein Speicher verfügbar", Toast.LENGTH_SHORT).show()
            return null
        }

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(dir, "locations_$stamp.csv")

        try {
            FileWriter(file).use { writer ->
                writer.append("timestamp,lat,lng,alt,acc\n")


                for (loc in loggedLocations) {
                    val t = loc.time
                    val la = loc.latitude
                    val lo = loc.longitude
                    val al = if (loc.hasAltitude()) loc.altitude else Double.NaN
                    val ac = if (loc.hasAccuracy()) loc.accuracy else Float.NaN
                    writer.append("$t,$la,$lo,$al,$ac\n")
                }



            }

            Toast.makeText(
                requireContext(),
                "GPS-Daten exportiert: ${file.name}",
                Toast.LENGTH_LONG
            ).show()


            return file
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
        }

        return null
    }


    // Permission-Check (FINE + COARSE)
    private fun ensureLocationPermission(): Boolean {
        val ctx = requireContext()
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)

        return if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            // Runtime Permission anfragen
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

    private fun saveLastExportPathForMap(path: String) {
        val prefs = requireContext().getSharedPreferences("sensorlogger_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("last_gps_csv_path", path)
            .apply()
    }


}