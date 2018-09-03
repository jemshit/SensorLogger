package com.jemshit.sensorlogger.ui.sensor_detail.widgets

import android.content.Context
import android.hardware.Sensor
import android.os.Build
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.model.SensorValueInfo
import com.jemshit.sensorlogger.model.getCategory
import com.jemshit.sensorlogger.model.maxFrequency
import com.jemshit.sensorlogger.model.minFrequency
import kotlinx.android.synthetic.main.sensor_info_widget.view.*

class SensorInfoWidget : MaterialCardView {
    constructor(context: Context) : super(context) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setup()
    }

    private fun setup() {
        inflate(context, R.layout.sensor_info_widget, this)
    }

    fun updateInfo(sensor: Sensor, sensorValueInfo: SensorValueInfo) {
        text_category.text = sensor.getCategory().name
        text_name.text = sensor.name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
            text_type.text = sensor.stringType
        else
            text_type.text = context.getString(R.string.unknown)

        text_vendor.text = sensor.vendor
        text_version.text = sensor.version.toString()
        if (sensorValueInfo.valueUnit.isNotBlank()) {
            text_max_range.text = "${sensor.maximumRange} (${sensorValueInfo.valueUnit})"
            text_resolution.text = "${sensor.resolution} (${sensorValueInfo.valueUnit})"
        } else {
            text_max_range.text = "${sensor.maximumRange}"
            text_resolution.text = "${sensor.resolution}"
        }
        text_power.text = "${sensor.power} (mA)"

        val minDelay = sensor.minDelay
        if (minDelay <= 0)
            text_min_delay.text = "${sensor.minDelay} (Non Streaming)"
        else {
            text_min_delay.text = "${sensor.minDelay} microseconds (Streaming).\nMax Freq. ${sensor.maxFrequency}Hz"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val maxDelay = sensor.maxDelay
            if (maxDelay <= 0)
                text_max_delay.text = "${sensor.maxDelay} (Non Streaming)"
            else
                text_max_delay.text = "${sensor.maxDelay} microseconds.\nMin Freq. ${sensor.minFrequency}Hz"
        } else
            text_max_delay.text = context.getString(R.string.unknown)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            text_is_wakeup.text = if (sensor.isWakeUpSensor) context.getString(R.string.yes) else context.getString(R.string.no)
        else
            text_is_wakeup.text = context.getString(R.string.unknown)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            text_is_dynamic.text = if (sensor.isDynamicSensor) context.getString(R.string.yes) else context.getString(R.string.no)
        else
            text_is_dynamic.text = context.getString(R.string.unknown)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            text_additional_info.text = if (sensor.isAdditionalInfoSupported) context.getString(R.string.yes) else context.getString(R.string.no)
        else
            text_additional_info.text = context.getString(R.string.unknown)
    }
}