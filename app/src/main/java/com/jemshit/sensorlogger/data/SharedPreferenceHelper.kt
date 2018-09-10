package com.jemshit.sensorlogger.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

const val PREF_KEY_LAST_WORKER_ID = "LastWorkerId"

fun getDefaultSharedPreference(context: Context): SharedPreferences {
    return context.getSharedPreferences("SensorLoggerPreferences", MODE_PRIVATE)
}