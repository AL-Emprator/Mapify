package com.app.sensorlogger.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.app.sensorlogger.R
import com.app.sensorlogger.data.Prefs

class SettingsFragment : Fragment() {

    // UI-Elemente aus deinem Layout
    private lateinit var switchReleaseNotes: Switch
    private lateinit var radioGroupStorage: RadioGroup
    private lateinit var radioCloud: RadioButton
    private lateinit var radioCsv: RadioButton
    private lateinit var radioJson: RadioButton

    private lateinit var radioGroupProvider: RadioGroup
    private lateinit var radiofused: RadioButton
    private lateinit var radiogps: RadioButton
    private lateinit var radionetz: RadioButton

    private lateinit var  radioblanced: RadioButton

    private lateinit var textDefaultRateValue: TextView
    private lateinit var switchAutoStart: Switch
    private lateinit var textMaxSessionValue: TextView

    private lateinit var radioGroupRoute: RadioGroup
    private lateinit var  outdoor_mode: RadioButton
    private lateinit var  indoor_mode: RadioButton

    /*
    // Name für SharedPreferences
    private val PREFS_NAME = "sensorlogger_prefs"

    // Keys für gespeicherte Werte
    //private val KEY_SHOW_RELEASE_NOTES = "show_release_notes"
    private val KEY_STORAGE_MODE = "storage_mode"

    private val KEY_PROVIEDER_MODE = "provieder_mode"

    private val KEY_DEFAULT_RATE = "default_sampling_rate"
    private val KEY_AUTOSTART_LOGGING = "autostart_logging"
    private val KEY_MAX_SESSION = "max_session_duration"


    // mögliche Werte für storage_mode
    private val MODE_CLOUD = "cloud"
    private val MODE_CSV = "csv"
    private val MODE_JSON = "json"

    private val MODE_FUSED = "fused"
    private val MODE_GPS = "GPS"

    //Optional
    private val MODE_NETZ = "Network"

    private val MODE_FUSED_HIGH = "fused_high"
    private val MODE_FUSED_BALANCED = "fused_balanced"

    private val KEY_ROUTE_MODE = "route_mode"
    private val MODE_OUTDOOR = "outdoor"
    private val MODE_INDOOR = "indoor"
*/


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // --- Views finden (IDs sind genau so wie in deinem Layout) ---

        // Allgemein
       // switchReleaseNotes = view.findViewById(R.id.switch_release_notes)

        // Datenspeicherung
        radioGroupStorage = view.findViewById(R.id.radio_storage)
        radioCloud = view.findViewById(R.id.radio_cloud)
        radioCsv = view.findViewById(R.id.radio_csv)
        radioJson = view.findViewById(R.id.radio_json)

        //Netzwerk
        radioGroupProvider = view.findViewById(R.id.radio_provider)
        radiofused = view.findViewById(R.id.radio_fused) //high defualt
        radioblanced = view.findViewById(R.id.radio_balanced)
        radiogps = view.findViewById(R.id.radio_gps)
        radionetz = view.findViewById(R.id.radio_netz)


        radioGroupRoute = view.findViewById(R.id.radio_route)
        outdoor_mode = view.findViewById(R.id.radio_route_outdoor)
        indoor_mode = view.findViewById(R.id.radio_route_indoor)

        // --- Views finden ---
        textDefaultRateValue = view.findViewById(R.id.text_default_rate_value)
        switchAutoStart = view.findViewById(R.id.switch_autostart_logging)
        textMaxSessionValue = view.findViewById(R.id.text_max_session_value)


        // SharedPreferences holen
        val prefs = requireContext().getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)

        // ------- Initialzustand aus Prefs laden -------

        // 1. Release Notes switch
        //val showNotes = prefs.getBoolean(KEY_SHOW_RELEASE_NOTES, true)
        //switchReleaseNotes.isChecked = showNotes

        // 2. Speichermodus (CSV default)
        val storageMode = prefs.getString(Prefs.KEY_STORAGE_MODE, Prefs.MODE_CSV)
        when (storageMode) {
            Prefs.MODE_CLOUD -> radioCloud.isChecked = true
            Prefs.MODE_JSON -> radioJson.isChecked = true
            else -> radioCsv.isChecked = true
        }


        //3. Provieder Posistion (FUSED default)
        val providerMode = prefs.getString(Prefs.KEY_PROVIDER_MODE, Prefs.MODE_FUSED_HIGH)
        when(providerMode) {
            Prefs.MODE_GPS -> radiogps.isChecked = true
            Prefs.MODE_NETWORK -> radionetz.isChecked = true
            Prefs.MODE_FUSED_BALANCED -> radioblanced.isChecked = true //Balnced
            else -> radiofused.isChecked = true //High defualt
        }


        val routemode = prefs.getString(Prefs.KEY_ROUTE_MODE, Prefs.MODE_OUTDOOR)
        when(routemode) {
                Prefs.MODE_INDOOR -> indoor_mode.isChecked = true
                else -> outdoor_mode.isChecked = true

        }

        val defaultRate = prefs.getFloat(Prefs.KEY_DEFAULT_RATE, 1.0f)
        textDefaultRateValue.text = "${defaultRate} Hz"

        val autostart = prefs.getBoolean(Prefs.KEY_AUTOSTART_LOGGING, false)
        switchAutoStart.isChecked = autostart

        val maxSession = prefs.getInt(Prefs.KEY_MAX_SESSION, 0)
        textMaxSessionValue.text = if (maxSession == 0) "Unbegrenzt" else "$maxSession min"


        // ------- Listener zum Speichern -------

        // Release Notes anzeigen
        /*
        switchReleaseNotes.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit()
                .putBoolean(KEY_SHOW_RELEASE_NOTES, isChecked)
                .apply()

            if (isChecked) {
                showReleaseNotesDialog()
            }
        }

         */

        // Speichermodus ändern
        radioGroupStorage.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radio_cloud -> Prefs.MODE_CLOUD
                R.id.radio_json -> Prefs.MODE_JSON
                else -> Prefs.MODE_CSV
            }

            prefs.edit()
                .putString(Prefs.KEY_STORAGE_MODE, mode)
                .apply()
        }

        //Provider Modus ändern
        radioGroupProvider.setOnCheckedChangeListener {  _, checkedId ->

            val mode = when (checkedId) {

                R.id.radio_gps -> Prefs.MODE_GPS
                R.id.radio_netz -> Prefs.MODE_NETWORK
                R.id.radio_balanced -> Prefs.MODE_FUSED_BALANCED
                else -> Prefs.MODE_FUSED_HIGH
            }

            prefs.edit()
                .putString(Prefs.KEY_PROVIDER_MODE, mode)
                .apply()


        }

        radioGroupRoute.setOnCheckedChangeListener { _, checkedId ->

            val mode = when (checkedId) {
                R.id.radio_route_indoor -> Prefs.MODE_INDOOR
                else -> Prefs.MODE_OUTDOOR
            }

            prefs.edit()
                .putString(Prefs.KEY_ROUTE_MODE, mode)
                .apply()
        }

        switchAutoStart.setOnCheckedChangeListener { _, enabled ->
            prefs.edit().putBoolean(Prefs.KEY_AUTOSTART_LOGGING, enabled).apply()
        }

        view.findViewById<View>(R.id.row_default_rate).setOnClickListener {
            showSamplingRateDialog(prefs)
        }

        view.findViewById<View>(R.id.row_max_session).setOnClickListener {
            showMaxSessionDialog(prefs)
        }


        return view
    }


    private fun showMaxSessionDialog(prefs: android.content.SharedPreferences) {
        val options = arrayOf("Unbegrenzt", "1 Minute", "5 Minuten", "10 Minuten", "30 Minuten")
        val values = arrayOf(0, 1, 5, 10, 30)

        val current = prefs.getInt(Prefs.KEY_MAX_SESSION, 0)
        var selectedIndex = values.indexOf(current)
        if (selectedIndex == -1) selectedIndex = 0

        AlertDialog.Builder(requireContext())
            .setTitle("Maximale Aufnahmedauer")
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
                val newValue = values[which]
                prefs.edit().putInt(Prefs.KEY_MAX_SESSION, newValue).apply()
                textMaxSessionValue.text =
                    if (newValue == 0) "Unbegrenzt" else "$newValue min"
                dialog.dismiss()
            }
            .show()
    }

    private fun showSamplingRateDialog(prefs: android.content.SharedPreferences) {
        val rates = arrayOf("0.5 Hz", "1.0 Hz", "2.0 Hz", "5.0 Hz", "10.0 Hz")
        val rateValues = arrayOf(0.5f, 1.0f, 2.0f, 5.0f, 10.0f)

        val current = prefs.getFloat(Prefs.KEY_DEFAULT_RATE, 1.0f)
        var selectedIndex = rateValues.indexOf(current)
        if (selectedIndex == -1) selectedIndex = 1

        AlertDialog.Builder(requireContext())
            .setTitle("Samplingrate auswählen")
            .setSingleChoiceItems(rates, selectedIndex) { dialog, which ->
                val newRate = rateValues[which]
                prefs.edit().putFloat(Prefs.KEY_DEFAULT_RATE, newRate).apply()
                textDefaultRateValue.text = "${newRate} Hz"
                dialog.dismiss()
            }
            .show()
    }


    /*
    private fun showReleaseNotesDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Release Notes")
            .setMessage(
                """
            Version 1.0.0 – 27.10.2025
            
            • Sensorlogger Grundfunktionen  
            • Beschleunigung, Gyroskop, Magnetometer  
            • GPS-Tracking mit Karte  
            • CSV/JSON Export  
            • Einstellungen & Design verbessert
            """.trimIndent()
            )
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

     */


}
