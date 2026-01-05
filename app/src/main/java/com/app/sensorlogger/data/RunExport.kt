package com.app.sensorlogger.data

import android.content.Context
import android.os.Environment
import com.app.sensorlogger.model.RunSession
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RunExport {

    data class ExportResult(
        val locationsFile: File?,
        val waypointsFile: File?
    )

    fun exportRunToCsv(context: Context, run: RunSession): ExportResult {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: return ExportResult(null, null)

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val routeName = run.route.name.lowercase()
        val providerName = run.providerVariant.name.lowercase()
        val runShort = run.runId.take(8)

        val locationsFile = File(dir, "run_locations_${routeName}_${providerName}_${runShort}_$stamp.csv")
        val waypointsFile = File(dir, "run_waypoints_${routeName}_${providerName}_${runShort}_$stamp.csv")

        val locOk = writeLocations(locationsFile, run)
        val wpOk = writeWaypoints(waypointsFile, run)

        // Pfade merken (für MapFragment Laden)
        val prefs = context.getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        if (locOk) prefs.edit().putString("last_gps_csv_path", locationsFile.absolutePath).apply()
        if (wpOk) prefs.edit().putString("last_waypoints_csv_path", waypointsFile.absolutePath).apply()

        return ExportResult(
            locationsFile = if (locOk) locationsFile else null,
            waypointsFile = if (wpOk) waypointsFile else null
        )
    }

    private fun writeLocations(file: File, run: RunSession): Boolean {
        return try {
            FileWriter(file).use { w ->
                w.append("timestamp,lat,lng,alt,acc,android_provider,run_provider\n")

                for (p in run.locations) {
                    val alt = p.altitude?.toString() ?: ""
                    val acc = p.accuracy?.toString() ?: ""
                    val androidProv  = p.provider.replace(",", " ")
                    val runProv = run.providerVariant.name.lowercase()
                    w.append(
                        "${p.timestamp}," +
                                "${p.latitude}," +
                                "${p.longitude}," +
                                "$alt,$acc," +
                                "$androidProv," +
                                "$runProv\n"
                    )
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun writeWaypoints(file: File, run: RunSession): Boolean {
        return try {
            FileWriter(file).use { w ->
                w.append("timestamp,lat,lng,note\n")
                for (p in run.waypoints) {
                    val note = p.note.replace(",", " ")
                    w.append("${p.timestamp},${p.latitude},${p.longitude},$note\n")
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
