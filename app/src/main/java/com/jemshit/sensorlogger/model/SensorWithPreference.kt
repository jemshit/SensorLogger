package com.jemshit.sensorlogger.model

import android.hardware.Sensor
import android.hardware.SensorManager
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceEntity
import com.jemshit.sensorlogger.helper.Diffable

const val DEFAULT_SAMPLING_PERIOD = SensorManager.SENSOR_DELAY_GAME
const val DEFAULT_SAMPLING_PERIOD_CUSTOM = -1

class SensorWithPreference(val sensor: Sensor, var preference: SensorPreferenceEntity?) : Diffable {
    override fun isTheSame(other: Diffable): Boolean {
        val otherItem = other as? SensorWithPreference
        return if (otherItem == null)
            false
        else
            this.sensor.uniqueId.equals(otherItem.sensor.uniqueId, true)
    }

    override fun isContentsTheSame(other: Diffable): Boolean {
        val otherItem = other as SensorWithPreference
        val otherPreference = otherItem.preference
        val currentPreference = this.preference
        return if (otherPreference == null && currentPreference == null)
            true
        else if (otherPreference != null && currentPreference == null)
            false
        else if (otherPreference == null && currentPreference != null)
            false
        else if (otherPreference != null && currentPreference != null) {
            ((otherPreference.isChecked == currentPreference.isChecked)
                    && (otherPreference.samplingPeriod == currentPreference.samplingPeriod)
                    && (otherPreference.samplingPeriodCustom == currentPreference.samplingPeriodCustom))
        } else
            false
    }
}