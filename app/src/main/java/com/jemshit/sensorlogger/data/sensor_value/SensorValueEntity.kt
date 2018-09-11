package com.jemshit.sensorlogger.data.sensor_value

class SensorValueEntity(val id: Long = 0,
                        val phoneUptime: Long,
                        val timestamp: Long,
                        val sensorName: String,
                        val sensorType: String,
                        val activityName: String,
                        val devicePosition: String,
                        val deviceOrientation: String,
                        val valueAccuracy: String,
                        val values: String)