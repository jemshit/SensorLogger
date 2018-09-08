package com.jemshit.sensorlogger.background_work

import android.database.Cursor
import androidx.work.State
import androidx.work.Worker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.data.sensor_value.SensorValueEntity
import com.jemshit.sensorlogger.data.sensor_value.SensorValueRepository
import com.jemshit.sensorlogger.data.statistics.StatisticsRepository
import com.jemshit.sensorlogger.model.*

class StatisticsWorker : Worker() {

    private val sensorValueRepository by lazy { SensorValueRepository.getInstance(applicationContext) }
    private val statisticsRepository by lazy { StatisticsRepository.getInstance(applicationContext) }
    private val allStatistics: MutableList<ActivityStatistics> = mutableListOf()
    private val loggedErrors: MutableList<String> = mutableListOf()
    private val gson: Gson = Gson()

    override fun doWork(): Result {
        val statistics = statisticsRepository.getById(id.toString())
        statistics.startTime = System.currentTimeMillis()
        statisticsRepository.update(statistics)

        val cursor = sensorValueRepository.getAllSortedCursor()
        val success = cursor.moveToFirst()
        return if (success) {
            calculateStatisticsWithCursor(cursor)

            val statistics = statisticsRepository.getById(id.toString())
            statistics.status = State.SUCCEEDED.name
            statistics.finishTime = System.currentTimeMillis()
            statistics.activityStatistics = gson.toJson(allStatistics, object : TypeToken<List<ActivityStatistics>>() {}.type)
            statistics.loggedErrors = gson.toJson(loggedErrors, object : TypeToken<List<String>>() {}.type)
            statisticsRepository.update(statistics)

            cursor.close()
            Result.SUCCESS
        } else {
            val statistics = statisticsRepository.getById(id.toString())
            statistics.status = State.SUCCEEDED.name
            statisticsRepository.update(statistics)

            cursor.close()
            Result.SUCCESS
        }
    }

    private fun getCurrentItemFromCursor(cursor: Cursor): SensorValueEntity {
        return SensorValueEntity(
                cursor.getLong(0),
                cursor.getLong(1),
                cursor.getLong(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5),
                cursor.getString(6),
                cursor.getString(7),
                cursor.getString(8),
                cursor.getString(9)
        )
    }

    private fun calculateStatisticsWithCursor(cursor: Cursor) {
        var currentActivity: ActivityStatistics? = null
        var currentPosition: PositionStatistics? = null
        var currentOrientation: OrientationStatistics? = null
        var lastEvent: String
        var startTimestamp: Long? = null
        var endTimestamp: Long? = null


        //region Internal Methods
        fun _clearTemp() {
            currentActivity = null
            currentPosition = null
            currentOrientation = null
            startTimestamp = null
            endTimestamp = null
        }

        fun _saveTemp() {
            if (currentOrientation == null || currentPosition == null || currentActivity == null || startTimestamp == null || endTimestamp == null)
                return
            else {
                currentOrientation!!.duration = endTimestamp!! - startTimestamp!!
                currentPosition!!.orientationStatistics.add(currentOrientation!!)
                currentActivity!!.positionStatistics.add(currentPosition!!)

                val sameActivityFound = allStatistics.find { it.name.equals(currentActivity!!.name, true) }
                sameActivityFound?.let { sameActivity ->
                    // Found same activity saved before

                    val samePositionFound = sameActivity.positionStatistics.find { it.name.equals(currentPosition!!.name, true) }
                    samePositionFound?.let { samePosition ->
                        // Found same position saved before

                        val sameOrientationFound = samePosition.orientationStatistics.find { it.name.equals(currentOrientation!!.name, true) }
                        sameOrientationFound?.let { sameOrientation ->
                            // Found same orientation saved before, UPDATE orientation

                            sameOrientation.count += currentOrientation!!.count
                            sameOrientation.duration += currentOrientation!!.duration

                        } ?: samePosition.orientationStatistics.add(currentOrientation!!)


                    } ?: sameActivity.positionStatistics.add(currentPosition!!)

                } ?: allStatistics.add(currentActivity!!)
            }
        }

        fun _removeDelayAmountOfItems(delayInSeconds: Int) {
            // todo
        }

        fun _createCurrentOrientation(name: String, timestamp: Long, valueAccuracy: String) {
            startTimestamp = timestamp
            currentOrientation = OrientationStatistics(name).also { it.increaseAccuracyCount(valueAccuracy) }
        }

        fun _processCurrentValue(index: Int, currentValue: SensorValueEntity) {
            if (currentValue.sensorType.equals(SensorLogEvent.EVENT.eventName, true)) {
                // Event
                lastEvent = currentValue.sensorName

                when {
                    currentValue.sensorName.equals(SensorLogEvent.START_LOGGING.eventName, true) -> {
                        if (lastEvent.equals(SensorLogEvent.START_LOGGING.eventName, true)) {
                            _saveTemp()
                            _clearTemp()
                        }
                        Unit
                    }
                    currentValue.sensorName.equals(SensorLogEvent.STOP_LOGGING.eventName, true) -> {
                        val delay = getDelay(currentValue.values)
                        if (delay > 0) {
                            _removeDelayAmountOfItems(delay)
                            _saveTemp()
                            _clearTemp()
                        } else {
                            _saveTemp()
                            _clearTemp()
                        }
                    }
                    currentValue.sensorName.equals(SensorLogEvent.SAVE_ERROR.eventName, true) -> {
                        loggedErrors.add("SAVE_ERROR event at index $index")
                        _saveTemp()
                        _clearTemp()
                    }
                    currentValue.sensorName.equals(SensorLogEvent.STOP_AND_IGNORE_LOGGING.eventName, true) -> {
                        _clearTemp()
                    }
                    else -> {
                        loggedErrors.add("Unknown event at index $index")
                        Unit
                    }
                }

            } else {
                // Sensor Value

                endTimestamp = currentValue.timestamp
                val activityName = if (currentValue.activityName.isNotBlank()) currentValue.activityName else EMPTY_ACTIVITY
                val positionName = if (currentValue.devicePosition.isNotBlank()) currentValue.devicePosition else EMPTY_POSITION
                val orientationName = if (currentValue.deviceOrientation.isNotBlank()) currentValue.deviceOrientation else EMPTY_ORIENTATION

                currentOrientation?.let {
                    it.count++
                    it.increaseAccuracyCount(currentValue.valueAccuracy)

                    if (!it.name.equals(orientationName, true)) {
                        loggedErrors.add(
                                applicationContext.getString(R.string.error_statistics_multiple_orientation_in_a_session, index.toString())
                        )
                    }

                }
                        ?: _createCurrentOrientation(orientationName, currentValue.timestamp, currentValue.valueAccuracy)

                if (currentPosition == null) {
                    currentPosition = PositionStatistics(positionName)
                } else {
                    if (!currentPosition!!.name.equals(positionName, true)) {
                        loggedErrors.add(
                                applicationContext.getString(R.string.error_statistics_multiple_position_in_a_session, index.toString())
                        )
                    }
                }

                if (currentActivity == null) {
                    currentActivity = ActivityStatistics(activityName)
                } else {
                    if (!currentActivity!!.name.equals(activityName, true)) {
                        loggedErrors.add(
                                applicationContext.getString(R.string.error_statistics_multiple_activity_in_a_session, index.toString())
                        )
                    }
                }
            }
        }
        //endregion

        var index = 0
        while (!cursor.isAfterLast) {
            _processCurrentValue(index, getCurrentItemFromCursor(cursor))

            cursor.moveToNext()
            index++
        }
    }

    private fun getDelay(values: String): Int {
        return try {
            values.toInt()
        } catch (e: Exception) {
            0
        }
    }
}