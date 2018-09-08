package com.jemshit.sensorlogger.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.SensorLoggerApplication
import com.jemshit.sensorlogger.background_work.SensorLoggerService
import com.jemshit.sensorlogger.background_work.isServiceRunningInForeground
import com.jemshit.sensorlogger.data.sensor_value.SensorValueRepository
import com.jemshit.sensorlogger.model.ActivityStatistics
import kotlinx.coroutines.experimental.IO
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch

sealed class UIWorkStatus {
    object Idle : UIWorkStatus()
    object Loading : UIWorkStatus()
    object Success : UIWorkStatus()
    class Error(val message: String) : UIWorkStatus()
}

class StatisticsViewModel(app: Application) : AndroidViewModel(app) {
    private val _calculationStatus: MutableLiveData<UIWorkStatus> = MutableLiveData()
    val calculationStatus: LiveData<UIWorkStatus> = _calculationStatus
    var statistics: Map<String, ActivityStatistics> = mapOf()
    private var calculationJob: Job? = null
    private val sensorValueRepository by lazy {
        SensorValueRepository.getInstance(getApplication<SensorLoggerApplication>().applicationContext)
    }

    init {
        _calculationStatus.value = UIWorkStatus.Idle
    }

    fun calculateStatistics() {
        if (isServiceRunningInForeground(getApplication<SensorLoggerApplication>().applicationContext, SensorLoggerService::class.java)) {
            _calculationStatus.value = UIWorkStatus.Error(
                    getApplication<SensorLoggerApplication>().applicationContext.getString(R.string.error_statistics_recording_is_alive)
            )
        } else {
            _calculationStatus.value = UIWorkStatus.Loading
            calculationJob = launch(IO) {
                try {
                    statistics = sensorValueRepository.getDistinctStatistics()

                    _calculationStatus.postValue(UIWorkStatus.Success)
                } catch (e: Exception) {
                    _calculationStatus.postValue(UIWorkStatus.Error(
                            e.message
                                    ?: getApplication<SensorLoggerApplication>().applicationContext.getString(R.string.error)
                    ))
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        calculationJob?.cancel()
    }
}