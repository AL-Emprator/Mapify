package com.app.sensorlogger.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.sensorlogger.model.LocationSample

class SharedLocationViewModel : ViewModel() {
    val currentLocation = MutableLiveData<LocationSample>()
}
