package com.jemshit.sensorlogger

import android.app.Application
import com.facebook.stetho.Stetho

class SensorLoggerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
    }
}