package com.app.sensorlogger.Sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
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
import com.app.sensorlogger.Sensors.AccelerometerFragment.SensorSample
import com.app.sensorlogger.data.Prefs
import com.google.android.material.slider.Slider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.round

class GyroscopeFragment : Fragment(), SensorEventListener {

    data class SensorSample(
        val timestamp: String,
        val x: Float,
        val y: Float,
        val z: Float
    )

    private var sensorManager: SensorManager? = null
    private var gyroscope: Sensor? = null

    private lateinit var textX: TextView
    private lateinit var textY: TextView
    private lateinit var textZ: TextView
    private lateinit var textviewx: TextView
    private lateinit var textviewy: TextView
    private lateinit var textviewz: TextView

    private lateinit var freqLabel: TextView
    private lateinit var slider: Slider
    private lateinit var minusBtn: ImageButton
    private lateinit var plusBtn: ImageButton
    private lateinit var btnRecord: ImageButton
    private lateinit var btnPause: ImageButton
    private lateinit var btnExport: ImageButton


    private var currentFreqHz = 1.0f
    private var lastUpdateTimeMs = 0L
    private var isLogging = false
    private val loggedData = mutableListOf<SensorSample>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_gyroscope, container, false)

        textX = view.findViewById(R.id.text_x_value)
        textY = view.findViewById(R.id.text_y_value)
        textZ = view.findViewById(R.id.text_z_value)

        textviewx = view.findViewById(R.id.textViewx)
        textviewy = view.findViewById(R.id.textViewy)
        textviewz = view.findViewById(R.id.textViewz)

        freqLabel = view.findViewById(R.id.text_frequency_label)
        slider = view.findViewById(R.id.slider_freq)
        minusBtn = view.findViewById(R.id.btn_freq_minus)
        plusBtn = view.findViewById(R.id.btn_freq_plus)
        btnRecord = view.findViewById(R.id.btn_record)
        btnPause = view.findViewById(R.id.btn_pause)
        btnExport = view.findViewById(R.id.btn_export)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)


        // ==== Globale Einstellungen laden ====
        val prefs = requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)


        // Samplingrate aus Settings laden (Standard 1.0 Hz)
        val defaultRateHz = prefs.getFloat(Prefs.KEY_DEFAULT_RATE, 1.0f)
        currentFreqHz = defaultRateHz
        slider.value = currentFreqHz
        updateFreqLabel()

        // 2Autostart laden
        val autoStartEnabled = prefs.getBoolean(Prefs.KEY_AUTOSTART_LOGGING, false)


        // Maximale Aufnahmedauer laden
        val maxSessionMinutes = prefs.getInt(Prefs.KEY_MAX_SESSION, 0)

        slider.addOnChangeListener { _, value, _ ->
            currentFreqHz = value
            updateFreqLabel()
            prefs.edit().putFloat(Prefs.KEY_DEFAULT_RATE, value).apply()
        }

        minusBtn.setOnClickListener {
            currentFreqHz = (currentFreqHz - 0.1f).coerceAtLeast(0.1f)
            currentFreqHz = (round(currentFreqHz * 10f) / 10f)
            slider.value = currentFreqHz
            updateFreqLabel()
        }

        plusBtn.setOnClickListener {
            currentFreqHz = (currentFreqHz + 0.1f).coerceAtMost(5.0f)
            currentFreqHz = (round(currentFreqHz * 10f) / 10f)
            slider.value = currentFreqHz
            updateFreqLabel()
        }

        btnRecord.setOnClickListener {
            loggedData.clear()
            isLogging = true
            startSensor()
            Toast.makeText(requireContext(), "Gyroskop-Logging gestartet", Toast.LENGTH_SHORT).show()
        }

        btnPause.setOnClickListener {
            isLogging = false
            stopSensor()
            Toast.makeText(requireContext(), "Gyroskop-Logging pausiert", Toast.LENGTH_SHORT).show()
        }

        btnExport.setOnClickListener { exportData() }


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


    private fun updateFreqLabel() {
        freqLabel.text = "Frequenz: ${String.format("%.1f", currentFreqHz)} Hz"
    }

    private fun startSensor() {
        gyroscope?.also {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    private fun stopSensor() {
        sensorManager?.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (isLogging) startSensor()
    }

    override fun onPause() {
        super.onPause()
        stopSensor()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_GYROSCOPE) return
        if (!isLogging) return

        val now = System.currentTimeMillis()
        val periodMs = (1000f / currentFreqHz).toLong()
        if (now - lastUpdateTimeMs < periodMs) return
        lastUpdateTimeMs = now

        val gx = event.values[0]
        val gy = event.values[1]
        val gz = event.values[2]

        textX.text = String.format("X-Achse: %.3f °/s", gx)
        textY.text = String.format("Y-Achse: %.3f °/s", gy)
        textZ.text = String.format("Z-Achse: %.3f °/s", gz)

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())

        textviewx.text = String.format(timeStamp)
        textviewy.text = String.format(timeStamp)
        textviewz.text = String.format(timeStamp)


        // Daten speichern, wenn Logging aktiv ist Mit timestamp
        if (isLogging) {
            loggedData.add(
                SensorSample(
                    timestamp = timeStamp,
                    x = gx,
                    y = gy,
                    z = gz
                )
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}


    private fun exportData() {
        val prefs = requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getString(Prefs.KEY_STORAGE_MODE, Prefs.MODE_CSV)

        when (mode) {
            Prefs.MODE_CLOUD -> exportToCloud()
            Prefs.MODE_JSON  -> exportToJson()
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


    private fun exportToCsv() {
        if (loggedData.isEmpty()) {
            Toast.makeText(requireContext(), "Keine Daten zum Exportieren", Toast.LENGTH_SHORT).show()
            return
        }

        val dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(dir, "gyroscope_$timeStamp.csv")

        try {

            FileWriter(file).use { writer ->
                writer.append("timestamp,x,y,z\n")
                for (sample in loggedData) {
                    writer.append(
                        "${sample.timestamp},${sample.x},${sample.y},${sample.z}\n"
                    )
                }

            }

            Toast.makeText(requireContext(), "Exportiert: ${file.name}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }



}