package com.jemshit.sensorlogger.ui.statistics

import android.app.Application
import android.database.Cursor
import android.util.Log.d
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagedList
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.SensorLoggerApplication
import com.jemshit.sensorlogger.background_work.SensorLoggerService
import com.jemshit.sensorlogger.background_work.isServiceRunningInForeground
import com.jemshit.sensorlogger.data.sensor_value.SensorValueEntity
import com.jemshit.sensorlogger.data.sensor_value.SensorValueRepository
import com.jemshit.sensorlogger.model.*
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers

const val PAGE_SIZE = 6000   //todo

sealed class CalculationStatus {
    object Idle : CalculationStatus()
    object Loading : CalculationStatus()
    object Success : CalculationStatus()
    class Error(val message: String) : CalculationStatus()
}

class StatisticsViewModel(app: Application) : AndroidViewModel(app) {
    private val sensorValueRepository by lazy {
        SensorValueRepository.getInstance(getApplication<SensorLoggerApplication>().applicationContext)
    }
    val calculationStatus: MutableLiveData<CalculationStatus> = MutableLiveData()
    val loggedErrors: MutableList<String> = mutableListOf()
    val allStatistics: MutableList<ActivityStatistics> = mutableListOf()
    private var allDataDisposable: Disposable? = null
    private var pagedList: PagedList<SensorValueEntity>? = null

    init {
        calculationStatus.value = CalculationStatus.Idle
    }

    // todo dont do when recording, because reative db
    // todo save pagedList, calculated statistics until hard refresh requested
    // todo enable end delay always and propagate when stop is clicked
    // todo when exporting, select sensors or activities
    //  speed improvement for calculation like gettin indexes of events beforehand
    //  use multi cpu for calculation
    // todo raw sql insread of this mess
    // todo ignore accuracies low, unreliable, unknown
    fun getAllStatistics() {
        if (isServiceRunningInForeground(getApplication<SensorLoggerApplication>().applicationContext, SensorLoggerService::class.java)) {
            calculationStatus.value = CalculationStatus.Error(
                    getApplication<SensorLoggerApplication>().applicationContext.getString(R.string.error_statistics_recording_is_alive)
            )
        } else {
            allDataDisposable = sensorValueRepository.getAllSorted(PAGE_SIZE)
                    .firstOrError()
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())    // todo
                    .doOnSubscribe { calculationStatus.postValue(CalculationStatus.Loading) }
                    .subscribeBy(onSuccess = { pagedList ->
                        this@StatisticsViewModel.pagedList = pagedList
                        calculateStatistics()
                    }, onError = {
                        calculationStatus.value = CalculationStatus.Error(it.message
                                ?: getApplication<SensorLoggerApplication>().applicationContext.getString(R.string.error))
                    })
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

    private fun calculateStatistics() {
        pagedList?.let { pagedList ->
            d("CustomLog", "Calculating ${pagedList.size}")

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
                                    getApplication<SensorLoggerApplication>().applicationContext
                                            .getString(R.string.error_statistics_multiple_orientation_in_a_session, index.toString())
                            )
                        }

                    }
                            ?: _createCurrentOrientation(orientationName, currentValue.timestamp, currentValue.valueAccuracy)

                    if (currentPosition == null) {
                        currentPosition = PositionStatistics(positionName)
                    } else {
                        if (!currentPosition!!.name.equals(positionName, true)) {
                            loggedErrors.add(
                                    getApplication<SensorLoggerApplication>().applicationContext
                                            .getString(R.string.error_statistics_multiple_position_in_a_session, index.toString())
                            )
                        }
                    }

                    if (currentActivity == null) {
                        currentActivity = ActivityStatistics(activityName)
                    } else {
                        if (!currentActivity!!.name.equals(activityName, true)) {
                            loggedErrors.add(
                                    getApplication<SensorLoggerApplication>().applicationContext
                                            .getString(R.string.error_statistics_multiple_activity_in_a_session, index.toString())
                            )
                        }
                    }
                }
            }

            pagedList.addWeakCallback(pagedList.snapshot(), object : PagedList.Callback() {
                override fun onChanged(position: Int, count: Int) {
                    d("CustomLog", "onChanged pos:$position count:$count pagesize ${pagedList.size}")

                    for (index in position until (position + count)) {
                        if (index == (position + count - 1)) {
                            if (index < (pagedList.size - 1)) {
                                d("CustomLog", "load around ${index + 1}")
                                pagedList.loadAround(index + 1)
                            } else {
                                d("CustomLog", "success at $index")
                                calculationStatus.postValue(CalculationStatus.Success)
                            }
                        } else
                            _processCurrentValue(index, pagedList[index]!!)
                    }
                }

                override fun onInserted(position: Int, count: Int) {
                    loggedErrors.add("New Item is inserted while calculating at $position, count:$count")
                }

                override fun onRemoved(position: Int, count: Int) {
                    loggedErrors.add("New Item is removed while calculating at $position, count:$count")
                }
            })
            //endregion

            var initialLoadIsEnough = true
            for (index in 0 until pagedList.size) {
                if (pagedList[index] != null) {
                    _processCurrentValue(index, pagedList[index]!!)
                } else {
                    initialLoadIsEnough = false
                    d("CustomLog", "load at $index")
                    pagedList.loadAround(index)
                    break
                }
            }

            if (initialLoadIsEnough) {
                d("CustomLog", "initial load is enough")

                calculationStatus.postValue(CalculationStatus.Success)
            }

        }
                ?: calculationStatus.postValue(CalculationStatus.Error("PagedList is null after DB query"))
    }

    private fun getDelay(values: String): Int {
        return try {
            values.toInt()
        } catch (e: Exception) {
            0
        }
    }

    override fun onCleared() {
        super.onCleared()
        allDataDisposable?.dispose()
    }
}