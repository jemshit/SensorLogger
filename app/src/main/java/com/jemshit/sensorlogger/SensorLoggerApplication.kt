package com.jemshit.sensorlogger

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.facebook.stetho.Stetho

const val LOG_TAG = "CustomLog"

class SensorLoggerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val workManagerConfiguration = Configuration.Builder().setMinimumLoggingLevel(Log.VERBOSE)
        WorkManager.initialize(this, workManagerConfiguration.build())

        Stetho.initializeWithDefaults(this)
    }
}