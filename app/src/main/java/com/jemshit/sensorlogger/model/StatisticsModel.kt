package com.jemshit.sensorlogger.model

import com.jemshit.sensorlogger.background_work.*

const val EMPTY_ACTIVITY = "EMPTY"
const val EMPTY_POSITION = "EMPTY"
const val EMPTY_ORIENTATION = "EMPTY"

data class OrientationStatistics(val name: String,
                                 var count: Long = 1,
                                 var duration: Long = 0,
                                 var highAccuracyCount: Long = 0,
                                 var mediumAccuracyCount: Long = 0,
                                 var lowAccuracyCount: Long = 0,
                                 var unreliableAccuracyCount: Long = 0,
                                 var unknownAccuracyCount: Long = 0)

fun OrientationStatistics.increaseAccuracyCount(valueAccuracy: String) {
    when (valueAccuracy) {
        ACCURACY_HIGH_TEXT -> highAccuracyCount++
        ACCURACY_MEDIUM_TEXT -> mediumAccuracyCount++
        ACCURACY_LOW_TEXT -> lowAccuracyCount++
        ACCURACY_UNRELIABLE_TEXT -> unreliableAccuracyCount++
        ACCURACY_UNKNOWN_TEXT -> unknownAccuracyCount++
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

    val duration: Long
        get() {
            return orientationStatistics.sumByLong { orientationStatistics ->
                orientationStatistics.duration
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
                              val positionStatistics: MutableList<PositionStatistics> = mutableListOf()) {
    val count: Long
        get() {
            return positionStatistics.sumByLong { positionStatistics ->
                positionStatistics.count
            }
        }

    val duration: Long
        get() {
            return positionStatistics.sumByLong { positionStatistics ->
                positionStatistics.duration
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