package com.jemshit.sensorlogger.background_work

import android.database.Cursor
import androidx.core.content.edit
import androidx.work.Worker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jemshit.sensorlogger.data.getDefaultSharedPreference
import com.jemshit.sensorlogger.data.sensor_value.SensorValueEntity
import com.jemshit.sensorlogger.data.sensor_value.SensorValueRepository
import com.jemshit.sensorlogger.helper.createActivityFolder
import com.jemshit.sensorlogger.helper.createLogFile
import com.jemshit.sensorlogger.helper.random
import com.jemshit.sensorlogger.model.*
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

const val ARG_EXCLUDED_ACCURACIES = "excluded_accuracies"

const val ARG_AGE = "age"
const val ARG_WEIGHT = "weight"
const val ARG_HEIGHT = "height"
const val ARG_GENDER = "gender"

const val GENDER_MALE = "male"
const val GENDER_FEMALE = "female"

class ExportWorker : Worker() {

    private val sensorValueRepository by lazy { SensorValueRepository.getInstance(applicationContext) }
    private val loggedErrors: MutableList<String> = mutableListOf()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
    private val gson = Gson()
    private val floatListType: Type = object : TypeToken<List<Float>>() {}.type
    private val stringListType: Type = object : TypeToken<List<String>>() {}.type
    private val deviceInfo: DeviceInfoModel = DeviceInfoModel()

    private lateinit var excludedAccuracies: Array<String>
    private lateinit var age: String
    private lateinit var weight: String
    private lateinit var height: String
    private lateinit var gender: String
    private lateinit var userInfo: UserInfoModel

    override fun doWork(): Result {
        excludedAccuracies = inputData.getStringArray(ARG_EXCLUDED_ACCURACIES) ?: arrayOf()
        age = inputData.getString(ARG_AGE) ?: ""
        weight = inputData.getString(ARG_WEIGHT) ?: ""
        height = inputData.getString(ARG_HEIGHT) ?: ""
        gender = inputData.getString(ARG_GENDER) ?: ""
        userInfo = UserInfoModel(age, weight, height, gender)

        val cursor = sensorValueRepository.getAllSortedCursor()
        val success = cursor.moveToFirst()
        return if (success) {
            export(cursor)
        } else {
            // Nothing to export
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

    private fun getLine(item: SensorValueEntity): String {
        if (item.values.isBlank())
            return ""

        val sensorName = getSensorTinyNameFromType(item.sensorType)
        val valuesDecoded = gson.fromJson<List<Float>>(item.values, floatListType)
        var valuesAsString = ""
        valuesDecoded.forEach {
            valuesAsString += it.toString()
            valuesAsString += " "
        }
        valuesAsString.removeSuffix(" ")

        return "${item.timestamp} ${item.phoneUptime} $sensorName: $valuesAsString ${item.valueAccuracy}"
    }

    private fun createFilename(): String {
        return dateFormatter.format(Date(System.currentTimeMillis())) + "-" + (0..1000).random().toString() + ".log"
    }

    private fun export(cursor: Cursor): Result {
        var tempValues = mutableListOf<SensorValueEntity>()
        var lastEvent = ""

        //region Internal Methods
        fun _clearTemp() {
            tempValues.clear()
            lastEvent = ""
        }

        fun _saveTemp(): Boolean {
            if (tempValues.isNotEmpty()) {
                val activityName = if (tempValues[0].activityName.isNotBlank()) tempValues[0].activityName else EMPTY_ACTIVITY
                val devicePosition = if (tempValues[0].devicePosition.isNotBlank()) tempValues[0].devicePosition else EMPTY_POSITION
                val deviceOrientation = if (tempValues[0].deviceOrientation.isNotBlank()) tempValues[0].deviceOrientation else EMPTY_ORIENTATION

                val folderErrorPair = createActivityFolder(applicationContext, activityName, devicePosition, deviceOrientation)
                if (folderErrorPair.first == null) {
                    loggedErrors.add(folderErrorPair.second)
                    return false

                } else {
                    val fileName = createFilename()
                    val fileErrorPair = createLogFile(applicationContext, folderErrorPair.first!!, fileName)
                    if (fileErrorPair.first == null) {
                        loggedErrors.add(fileErrorPair.second)
                        return false
                    } else {
                        val file = fileErrorPair.first!!
                        try {
                            file.bufferedWriter().use { bufferedWriter ->
                                bufferedWriter.write("USER_INFO: $userInfo\n")
                                bufferedWriter.write("DEVICE_INFO: $deviceInfo\n")
                                bufferedWriter.write("ACTIVITY_NAME: $activityName\n")
                                bufferedWriter.write("DEVICE_POSITION: $devicePosition\n")
                                bufferedWriter.write("DEVICE_ORIENTATION: $deviceOrientation\n")

                                tempValues.forEachIndexed { index, item ->
                                    bufferedWriter.write(getLine(item))
                                    if (index != (tempValues.size - 1))
                                        bufferedWriter.write("\n")
                                }
                            }
                        } catch (e: Exception) {
                            loggedErrors.add(e.message ?: "Unknown Error while writing to file")
                            return false
                        }

                        return true
                    }
                }

            } else {
                return true
            }
        }

        fun _removeDelayAmountOfItems(delayInSeconds: Int) {
            if (tempValues.isNotEmpty()) {
                val lastEpoch = tempValues[tempValues.size - 1].timestamp
                val delayInMs = delayInSeconds * 1000
                val lastAllowedEpoch = lastEpoch - delayInMs
                tempValues = tempValues.dropLastWhile {
                    it.timestamp > lastAllowedEpoch
                }.toMutableList()
            }
        }

        fun _processCurrentValue(index: Int, currentValue: SensorValueEntity): Boolean {
            return if (currentValue.sensorType.equals(SensorLogEvent.EVENT.eventName, true)) {
                // Event
                val success: Boolean

                if (currentValue.sensorName.equals(SensorLogEvent.START_LOGGING.eventName, true)) {
                    if (lastEvent.equals(SensorLogEvent.START_LOGGING.eventName, true)) {
                        success = _saveTemp()
                        _clearTemp()
                    } else
                        success = true

                } else if (currentValue.sensorName.equals(SensorLogEvent.STOP_LOGGING.eventName, true)) {
                    val delay = getDelay(currentValue.values)
                    if (delay > 0) {
                        _removeDelayAmountOfItems(delay)
                        success = _saveTemp()
                        _clearTemp()
                    } else {
                        success = _saveTemp()
                        _clearTemp()
                    }

                } else if (currentValue.sensorName.equals(SensorLogEvent.SAVE_ERROR.eventName, true)) {
                    loggedErrors.add("SAVE_ERROR event at index $index")
                    success = _saveTemp()
                    _clearTemp()

                } else if (currentValue.sensorName.equals(SensorLogEvent.STOP_AND_IGNORE_LOGGING.eventName, true)) {
                    _clearTemp()
                    success = true
                } else {
                    loggedErrors.add("Unknown event at index $index")
                    success = true
                }

                lastEvent = currentValue.sensorName
                success

            } else {
                // Sensor Value

                if (!excludedAccuracies.contains(currentValue.valueAccuracy))
                    tempValues.add(currentValue)

                true
            }
        }
        //endregion

        var index = 0
        while (!cursor.isAfterLast) {
            val success: Boolean = _processCurrentValue(index, getCurrentItemFromCursor(cursor))
            if (!success) {
                cursor.close()
                persistLoggedErrors()

                return Result.FAILURE
            }

            cursor.moveToNext()
            index++
        }

        persistLoggedErrors()
        return Result.SUCCESS
    }

    private fun getDelay(values: String): Int {
        return try {
            values.toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun persistLoggedErrors() {
        if (loggedErrors.isNotEmpty()) {
            val errorsJson = gson.toJson(loggedErrors.toList(), stringListType)
            getDefaultSharedPreference(applicationContext).edit {
                putString(id.toString(), errorsJson)
            }
        }
    }
}