package com.app.sensorlogger.data



object Prefs {
    const val PREFS_NAME = "sensorlogger_prefs"

    const val KEY_PROVIDER_MODE = "provider_mode"   // !!! einheitlich !!!
    const val KEY_ROUTE_MODE = "route_mode"

    // Werte:
    const val MODE_FUSED_HIGH = "fused_high"
    const val MODE_FUSED_BALANCED = "fused_balanced"
    const val MODE_GPS = "gps"
    const val MODE_NETWORK = "network"


    // Name für SharedPreferences
    // Keys für gespeicherte Werte
    const val KEY_STORAGE_MODE = "storage_mode"
    const val KEY_DEFAULT_RATE = "default_sampling_rate"
    const val KEY_AUTOSTART_LOGGING = "autostart_logging"
    const val KEY_MAX_SESSION = "max_session_duration"


    // mögliche Werte für storage_mode
    const val MODE_CLOUD = "cloud"
    const val MODE_CSV = "csv"
    const val MODE_JSON = "json"

    const val MODE_FUSED = "fused"
    const val MODE_OUTDOOR = "outdoor"
    const val MODE_INDOOR = "indoor"

}
