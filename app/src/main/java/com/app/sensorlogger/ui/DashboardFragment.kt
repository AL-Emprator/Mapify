package com.app.sensorlogger.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.app.sensorlogger.R

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        // verbinde Fragment mit seinem Layout


        // row_accel muss die ID sein von der "Beschleunigung"-Zeile in fragment_dashboard.xml
        val accelRow = view.findViewById<View>(R.id.row_accel)
        accelRow.setOnClickListener {
            findNavController().navigate(R.id.nav_accelerometer)
        }

        val rowmag = view.findViewById<View>(R.id.row_mag)
        rowmag.setOnClickListener {
            findNavController().navigate(R.id.nav_magnetometer)
        }

        val rowGyro = view.findViewById<View>(R.id.row_gyro)

        rowGyro.setOnClickListener {
            findNavController().navigate(R.id.nav_gyroscope)
        }

        val rowLocation = view.findViewById<View>(R.id.row_location)
        rowLocation.setOnClickListener {
            findNavController().navigate(R.id.nav_location)
        }

        val rowsettings = view.findViewById<View>(R.id.row_settings)
        rowsettings.setOnClickListener { findNavController().navigate(R.id.nav_settings) }




        return view

    }

}