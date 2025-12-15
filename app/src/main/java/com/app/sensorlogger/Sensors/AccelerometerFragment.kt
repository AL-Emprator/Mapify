package com.app.sensorlogger.Sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.app.sensorlogger.R
import com.google.android.material.slider.Slider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Locale

class AccelerometerFragment : Fragment(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private lateinit var textX: TextView
    private lateinit var textY: TextView
    private lateinit var textZ: TextView
    private lateinit var freqLabel: TextView
    private lateinit var slider: Slider
    private lateinit var minusBtn: ImageButton
    private lateinit var plusBtn: ImageButton

    private lateinit var btnRecord: ImageButton
    private lateinit var btnPause: ImageButton
    private lateinit var btnExport: ImageButton

    // --- Zustände ---
    private var currentFreqHz = 1.0f
    private var lastUpdateTimeMs: Long = 0L

    // Logging aktiv?
    private var isLogging = true


    // --- Datenliste ---
    private val loggedData = mutableListOf<Triple<Float, Float, Float>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_accelerometer, container, false)

        // Views referenzieren
        textX = view.findViewById(R.id.text_x_value)
        textY = view.findViewById(R.id.text_y_value)
        textZ = view.findViewById(R.id.text_z_value)
        freqLabel = view.findViewById(R.id.text_frequency_label)
        slider = view.findViewById(R.id.slider_freq)
        minusBtn = view.findViewById(R.id.btn_freq_minus)
        plusBtn = view.findViewById(R.id.btn_freq_plus)


        btnRecord = view.findViewById(R.id.btn_record)
        btnPause  = view.findViewById(R.id.btn_pause)
        btnExport = view.findViewById(R.id.btn_export)

        // Sensor holen
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // ==== Globale Einstellungen laden ====
        val prefs = requireContext().getSharedPreferences("sensorlogger_prefs", Context.MODE_PRIVATE)

        // Samplingrate aus Settings laden (Standard 1.0 Hz)
        val defaultRateHz = prefs.getFloat("default_sampling_rate", 1.0f)
        currentFreqHz = defaultRateHz
        slider.value = currentFreqHz
        updateFreqLabel()

        // 2Autostart laden
        val autoStartEnabled = prefs.getBoolean("autostart_logging", false)


        // Maximale Aufnahmedauer laden
        val maxSessionMinutes = prefs.getInt("max_session_duration", 0)

        // Slider Listener
        slider.addOnChangeListener { _, value, _ ->
            currentFreqHz = value
            updateFreqLabel() //für + und -
            prefs.edit().putFloat("default_sampling_rate", value).apply()
        }

        // Minus Button
        minusBtn.setOnClickListener {
            currentFreqHz = (currentFreqHz - 0.1f).coerceAtLeast(0.1f)
            currentFreqHz = (Math.round(currentFreqHz * 10f) / 10f)
            slider.value = currentFreqHz
            updateFreqLabel()
        }

        // Plus Button
        plusBtn.setOnClickListener {
            currentFreqHz = (currentFreqHz + 0.1f).coerceAtMost(5.0f)
            currentFreqHz = (Math.round(currentFreqHz * 10f) / 10f)
            slider.value = currentFreqHz
            updateFreqLabel()
        }

        // === Record/Pause Buttons ===
        btnRecord.setOnClickListener {
            loggedData.clear()
            isLogging = true
            startSensor()
            Toast.makeText(requireContext(), "Aufzeichnung gestartet", Toast.LENGTH_SHORT).show()
        }

        btnPause.setOnClickListener {
            isLogging = false
            stopSensor()
            Toast.makeText(requireContext(), "Aufzeichnung pausiert", Toast.LENGTH_SHORT).show()
        }

        btnExport.setOnClickListener {
            // hier später: CSV / JSON / Cloud Upload basierend auf deiner Einstellung
            // aktuell nur Platzhalter
            exportData()
        }


        if (autoStartEnabled) {
            loggedData.clear()
            isLogging = true
            startSensor()
            Toast.makeText(
                requireContext(),
                "Automatische Aufzeichnung gestartet",
                Toast.LENGTH_SHORT
            ).show()

            // Optional: automatisches Stoppen nach maxSessionMinutes
            if (maxSessionMinutes > 0) {
                view.postDelayed({
                    if (isLogging) {
                        isLogging = false
                        stopSensor()
                        Toast.makeText(
                            requireContext(),
                            "Aufzeichnung automatisch gestoppt nach $maxSessionMinutes Minuten",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }, (maxSessionMinutes * 60_000).toLong())
            }

        }


        return view
    }


    //für slider
    private fun updateFreqLabel() {
        freqLabel.text = "Frequenz: ${String.format("%.1f", currentFreqHz)} Hz"
    }


    // === Sensor Lifecycle ===
    override fun onResume() {
        super.onResume()
        super.onResume()
        if (isLogging) startSensor()
    }

    override fun onPause() {
        super.onPause()
        stopSensor()
    }

    // === SensorEventListener ===
    override fun onSensorChanged(event: android.hardware.SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {

            val now = System.currentTimeMillis()

            // gewünschtes Intervall in Millisekunden
            val periodMs = (1000f / currentFreqHz).toLong()

            // Prüfen, ob seit letztem Update genug Zeit vergangen ist
            if (now - lastUpdateTimeMs < periodMs) {
                // zu früh -> einfach zurückkehren, keine UI-Updates
                return
            }

            // Zeitstempel merken
            lastUpdateTimeMs = now

            val ax = event.values[0] // m/s^2
            val ay = event.values[1]
            val az = event.values[2]

            textX.text = String.format("X-Achse: %.5f m/s²", ax)
            textY.text = String.format("Y-Achse: %.5f m/s²", ay)
            textZ.text = String.format("Z-Achse: %.5f m/s²", az)


            // Daten speichern, wenn Logging aktiv ist
            if (isLogging) {
                loggedData.add(Triple(ax, ay, az))
            }

        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // kein Problem, ignorieren
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


    // === Export als CSV-Datei ===
    private fun exportToCsv() {
        if (loggedData.isEmpty()) {
            Toast.makeText(requireContext(), "Keine Daten zum Exportieren", Toast.LENGTH_SHORT).show()
            return
        }

        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (dir == null) {
            Toast.makeText(requireContext(), "Kein Speicher gefunden", Toast.LENGTH_SHORT).show()
            return
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        val file = File(dir, "accelerometer_$timeStamp.csv")

        try {
            FileWriter(file).use { writer ->
                writer.append("X,Y,Z\n")
                for ((x, y, z) in loggedData) {
                    writer.append("$x,$y,$z\n")
                }
            }

            Toast.makeText(requireContext(), "Exportiert nach: ${file.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Fehler beim Export: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startSensor() {
        accelerometer?.also { sensor ->
            sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    private fun stopSensor() {
        sensorManager?.unregisterListener(this)
    }


}
