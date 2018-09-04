package com.jemshit.sensorlogger.model

enum class SensorLogEvent(val eventName: String) {
    EVENT("EVENT"),
    SAVE_ERROR("SAVE_ERROR"),
    START_LOGGING("START_LOGGING"),
    STOP_LOGGING("STOP_LOGGING"),
}