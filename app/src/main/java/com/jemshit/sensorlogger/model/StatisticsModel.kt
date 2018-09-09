package com.jemshit.sensorlogger.model

import com.jemshit.sensorlogger.background_work.*

const val EMPTY_ACTIVITY = "EMPTY"
const val EMPTY_POSITION = "EMPTY"
const val EMPTY_ORIENTATION = "EMPTY"

data class SensorStatistics(val type: String,
                            var highAccuracyCount: Long = 0,
                            var mediumAccuracyCount: Long = 0,
                            var lowAccuracyCount: Long = 0,
                            var unreliableAccuracyCount: Long = 0,
                            var unknownAccuracyCount: Long = 0) {
    val count: Long
        get() {
            return highAccuracyCount + mediumAccuracyCount + lowAccuracyCount + unreliableAccuracyCount + unknownAccuracyCount
        }
}

fun SensorStatistics.increaseAccuracyCount(valueAccuracy: String, count: Long = 1) {
    when (valueAccuracy) {
        ACCURACY_HIGH_TEXT -> highAccuracyCount += count
        ACCURACY_MEDIUM_TEXT -> mediumAccuracyCount += count
        ACCURACY_LOW_TEXT -> lowAccuracyCount += count
        ACCURACY_UNRELIABLE_TEXT -> unreliableAccuracyCount += count
        ACCURACY_UNKNOWN_TEXT -> unknownAccuracyCount += count
    }
}

data class OrientationStatistics(val name: String,
                                 val sensorStatistics: MutableList<SensorStatistics>) {
    val count: Long
        get() {
            return sensorStatistics.sumByLong { sensorStatistics ->
                sensorStatistics.count
            }
        }

    val highAccuracyCount: Long
        get() {
            return sensorStatistics.sumByLong { sensorStatistics ->
                sensorStatistics.highAccuracyCount
            }
        }
    val mediumAccuracyCount: Long
        get() {
            return sensorStatistics.sumByLong { sensorStatistics ->
                sensorStatistics.mediumAccuracyCount
            }
        }
    val lowAccuracyCount: Long
        get() {
            return sensorStatistics.sumByLong { sensorStatistics ->
                sensorStatistics.lowAccuracyCount
            }
        }
    val unreliableAccuracyCount: Long
        get() {
            return sensorStatistics.sumByLong { sensorStatistics ->
                sensorStatistics.unreliableAccuracyCount
            }
        }
    val unknownAccuracyCount: Long
        get() {
            return sensorStatistics.sumByLong { sensorStatistics ->
                sensorStatistics.unknownAccuracyCount
            }
        }
}

data class PositionStatistics(val name: String,
                              val orientationStatistics: MutableList<OrientationStatistics> = mutableListOf()) {
    val count: Long
        get() {
            return orientationStatistics.sumByLong { orientationStatistics ->
                orientationStatistics.count
            }
        }

    val highAccuracyCount: Long
        get() {
            return orientationStatistics.sumByLong { orientationStatistics ->
                orientationStatistics.highAccuracyCount
            }
        }
    val mediumAccuracyCount: Long
        get() {
            return orientationStatistics.sumByLong { orientationStatistics ->
                orientationStatistics.mediumAccuracyCount
            }
        }
    val lowAccuracyCount: Long
        get() {
            return orientationStatistics.sumByLong { orientationStatistics ->
                orientationStatistics.lowAccuracyCount
            }
        }
    val unreliableAccuracyCount: Long
        get() {
            return orientationStatistics.sumByLong { orientationStatistics ->
                orientationStatistics.unreliableAccuracyCount
            }
        }
    val unknownAccuracyCount: Long
        get() {
            return orientationStatistics.sumByLong { orientationStatistics ->
                orientationStatistics.unknownAccuracyCount
            }
        }
}

data class ActivityStatistics(val name: String,
                              val positionStatistics: MutableList<PositionStatistics> = mutableListOf(),
                              var sensorAccuracyStatistics: Map<String, MutableList<Pair<String, Long>>> = mapOf(),
                              var durationInMs: Long) {
    val count: Long
        get() {
            return positionStatistics.sumByLong { positionStatistics ->
                positionStatistics.count
            }
        }

    val highAccuracyCount: Long
        get() {
            return positionStatistics.sumByLong { positionStatistics ->
                positionStatistics.highAccuracyCount
            }
        }
    val mediumAccuracyCount: Long
        get() {
            return positionStatistics.sumByLong { positionStatistics ->
                positionStatistics.mediumAccuracyCount
            }
        }
    val lowAccuracyCount: Long
        get() {
            return positionStatistics.sumByLong { positionStatistics ->
                positionStatistics.lowAccuracyCount
            }
        }
    val unreliableAccuracyCount: Long
        get() {
            return positionStatistics.sumByLong { positionStatistics ->
                positionStatistics.unreliableAccuracyCount
            }
        }
    val unknownAccuracyCount: Long
        get() {
            return positionStatistics.sumByLong { positionStatistics ->
                positionStatistics.unknownAccuracyCount
            }
        }
}

fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum: Long = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}