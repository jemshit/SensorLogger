package com.jemshit.sensorlogger.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jemshit.sensorlogger.SensorLoggerApplication
import com.jemshit.sensorlogger.data.sensor_value.SensorValueEntity
import com.jemshit.sensorlogger.data.sensor_value.SensorValueRepository
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch

class StatisticsViewModel(app: Application) : AndroidViewModel(app) {
    private val sensorValueRepository by lazy {
        SensorValueRepository.getInstance(getApplication<SensorLoggerApplication>().applicationContext)
    }
    private val _allDataSorted: MutableLiveData<Map<String, List<SensorValueEntity>>> = MutableLiveData()
    val allDataSorted: LiveData<Map<String, List<SensorValueEntity>>> = _allDataSorted
    private var allDataSortedJob: Job? = null

    fun getAllDataSortedGrouped() {
        allDataSortedJob = launch {
            val data = sensorValueRepository.getAllSorted()
            val dataGrouped = data.groupBy {
                it.activityName
            }
            _allDataSorted.postValue(dataGrouped)
        }
    }

    override fun onCleared() {
        super.onCleared()
        allDataSortedJob?.cancel()
    }
}