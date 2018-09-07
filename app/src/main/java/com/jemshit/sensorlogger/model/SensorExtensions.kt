package com.jemshit.sensorlogger.model

import android.hardware.Sensor
import android.os.Build


val Sensor.uniqueId: String
    get() = name + "-" + type.toString() + "-" + vendor + "-" + version.toString()

val Sensor.minFrequency: Float
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (maxDelay <= 0)
                0f
            else
                1000f / (maxDelay / 1000f)
        } else {
            0f
        }
    }

val Sensor.maxFrequency: Float
    get() {
        return if (minDelay <= 0)
            0f
        else
            1000f / (minDelay / 1000f)
    }


fun Sensor.getValueCount(): Int {
    return when (type) {
        Sensor.TYPE_ACCELEROMETER -> 3
        Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> 6
        Sensor.TYPE_GRAVITY -> 3
        Sensor.TYPE_GYROSCOPE -> 3
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> 6
        Sensor.TYPE_LINEAR_ACCELERATION -> 3
        Sensor.TYPE_ROTATION_VECTOR -> 4    // 4th is optional?
        Sensor.TYPE_SIGNIFICANT_MOTION -> 0
        Sensor.TYPE_STEP_COUNTER -> 1
        Sensor.TYPE_STEP_DETECTOR -> 0

        Sensor.TYPE_GAME_ROTATION_VECTOR -> 3
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> 3
        Sensor.TYPE_MAGNETIC_FIELD -> 3
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> 6
        Sensor.TYPE_ORIENTATION -> 3
        Sensor.TYPE_PROXIMITY -> 1

        Sensor.TYPE_AMBIENT_TEMPERATURE -> 1
        Sensor.TYPE_TEMPERATURE -> 1
        Sensor.TYPE_LIGHT -> 1
        Sensor.TYPE_PRESSURE -> 1
        Sensor.TYPE_RELATIVE_HUMIDITY -> 1

        Sensor.TYPE_STATIONARY_DETECT -> 1
        Sensor.TYPE_MOTION_DETECT -> 1
        Sensor.TYPE_HEART_BEAT -> 1

        else -> -1
    }
}

fun Sensor.getValueUnit(): String {
    return when (type) {
        Sensor.TYPE_ACCELEROMETER -> "m/s2"
        Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> "m/s2"
        Sensor.TYPE_GRAVITY -> "m/s2"
        Sensor.TYPE_GYROSCOPE -> "rad/s"
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "rad/s"
        Sensor.TYPE_LINEAR_ACCELERATION -> "m/s2"
        Sensor.TYPE_ROTATION_VECTOR -> ""
        Sensor.TYPE_SIGNIFICANT_MOTION -> ""
        Sensor.TYPE_STEP_COUNTER -> "Steps"
        Sensor.TYPE_STEP_DETECTOR -> ""

        Sensor.TYPE_GAME_ROTATION_VECTOR -> ""
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> ""
        Sensor.TYPE_MAGNETIC_FIELD -> "μT"
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "μT"
        Sensor.TYPE_ORIENTATION -> "Degrees"
        Sensor.TYPE_PROXIMITY -> "cm (hard. dep.)"

        Sensor.TYPE_AMBIENT_TEMPERATURE -> "°C"
        Sensor.TYPE_TEMPERATURE -> "°C (hard. dep.)"
        Sensor.TYPE_LIGHT -> "lx"
        Sensor.TYPE_PRESSURE -> "hPa or mbar"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "%"

        Sensor.TYPE_STATIONARY_DETECT -> ""
        Sensor.TYPE_MOTION_DETECT -> ""
        Sensor.TYPE_HEART_BEAT -> "confidence"

        else -> ""
    }
}

fun Sensor.getCategory(): SensorCategory {
    return when (this.type) {
        Sensor.TYPE_ACCELEROMETER -> SensorCategory.MOTION
        Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> SensorCategory.MOTION
        Sensor.TYPE_GRAVITY -> SensorCategory.MOTION
        Sensor.TYPE_GYROSCOPE -> SensorCategory.MOTION
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> SensorCategory.MOTION
        Sensor.TYPE_LINEAR_ACCELERATION -> SensorCategory.MOTION
        Sensor.TYPE_ROTATION_VECTOR -> SensorCategory.MOTION
        Sensor.TYPE_SIGNIFICANT_MOTION -> SensorCategory.MOTION
        Sensor.TYPE_STEP_COUNTER -> SensorCategory.MOTION
        Sensor.TYPE_STEP_DETECTOR -> SensorCategory.MOTION

        Sensor.TYPE_GAME_ROTATION_VECTOR -> SensorCategory.POSITION
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> SensorCategory.POSITION
        Sensor.TYPE_MAGNETIC_FIELD -> SensorCategory.POSITION
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> SensorCategory.POSITION
        Sensor.TYPE_ORIENTATION -> SensorCategory.POSITION
        Sensor.TYPE_PROXIMITY -> SensorCategory.POSITION

        Sensor.TYPE_AMBIENT_TEMPERATURE -> SensorCategory.ENVIRONMENT
        Sensor.TYPE_TEMPERATURE -> SensorCategory.ENVIRONMENT
        Sensor.TYPE_LIGHT -> SensorCategory.ENVIRONMENT
        Sensor.TYPE_PRESSURE -> SensorCategory.ENVIRONMENT
        Sensor.TYPE_RELATIVE_HUMIDITY -> SensorCategory.ENVIRONMENT

        Sensor.TYPE_STATIONARY_DETECT -> SensorCategory.MOTION
        Sensor.TYPE_MOTION_DETECT -> SensorCategory.MOTION
        Sensor.TYPE_HEART_BEAT -> SensorCategory.UNKNOWN

        else -> SensorCategory.UNKNOWN
    }
}

fun Sensor.getTypePre20(): String {
    return when (type) {

        Sensor.TYPE_ACCELEROMETER -> "android.sensor.accelerometer"
        Sensor.TYPE_AMBIENT_TEMPERATURE -> "android.sensor.ambient_temperature"
        Sensor.TYPE_GAME_ROTATION_VECTOR -> "android.sensor.game_rotation_vector"
        Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "android.sensor.geomagnetic_rotation_vector"
        Sensor.TYPE_GRAVITY -> "android.sensor.gravity"
        Sensor.TYPE_GYROSCOPE -> "android.sensor.gyroscope"
        Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "android.sensor.gyroscope_uncalibrated"
        Sensor.TYPE_HEART_RATE -> "android.sensor.heart_rate"
        Sensor.TYPE_LIGHT -> "android.sensor.light"
        Sensor.TYPE_LINEAR_ACCELERATION -> "android.sensor.linear_acceleration"
        Sensor.TYPE_MAGNETIC_FIELD -> "android.sensor.magnetic_field"
        Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "android.sensor.magnetic_field_uncalibrated"
        Sensor.TYPE_PRESSURE -> "android.sensor.pressure"
        Sensor.TYPE_PROXIMITY -> "android.sensor.proximity"
        Sensor.TYPE_RELATIVE_HUMIDITY -> "android.sensor.relative_humidity"
        Sensor.TYPE_ROTATION_VECTOR -> "android.sensor.rotation_vector"
        Sensor.TYPE_SIGNIFICANT_MOTION -> "android.sensor.significant_motion"
        Sensor.TYPE_STEP_COUNTER -> "android.sensor.step_counter"
        Sensor.TYPE_STEP_DETECTOR -> "android.sensor.step_detector"
        Sensor.TYPE_ORIENTATION -> "android.sensor.orientation"
        Sensor.TYPE_TEMPERATURE -> "android.sensor.temperature"
        Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT -> "android.sensor.low_latency_offbody_detect"
        Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> "android.sensor.accelerometer_uncalibrated"
        else -> "Unknown"

    }
}

fun getSensorSimpleNameFromType(type: String): String {
    return when (type) {

        "android.sensor.accelerometer" -> "Accelerometer"
        "android.sensor.ambient_temperature" -> "Ambient Temperature"
        "android.sensor.game_rotation_vector" -> "Game Rotation Vector"
        "android.sensor.geomagnetic_rotation_vector" -> "Geomagnetic Rotation Vector"
        "android.sensor.gravity" -> "Gravity"
        "android.sensor.gyroscope" -> "Gyroscope"
        "android.sensor.gyroscope_uncalibrated" -> "Gyroscope Uncalibrated"
        "android.sensor.heart_rate" -> "Heart Rate"
        "android.sensor.light" -> "Light"
        "android.sensor.linear_acceleration" -> "Linear Acceleration"
        "android.sensor.magnetic_field" -> "Magnetometer"
        "android.sensor.magnetic_field_uncalibrated" -> "Magnetometer Uncalibrated"
        "android.sensor.pressure" -> "Pressure"
        "android.sensor.proximity" -> "Proximity"
        "android.sensor.relative_humidity" -> "Relative Humidity"
        "android.sensor.rotation_vector" -> "Rotation Vector"
        "android.sensor.significant_motion" -> "Significant Motion"
        "android.sensor.step_counter" -> "Step Counter"
        "android.sensor.step_detector" -> "Step Detector"
        "android.sensor.orientation" -> "Orientation"
        "android.sensor.temperature" -> "Temperature"
        "android.sensor.low_latency_offbody_detect" -> "Low Latency Offbody Detect"
        "android.sensor.accelerometer_uncalibrated" -> "Accelerometer Uncalibrated"
        else -> "Unknown"

    }
}