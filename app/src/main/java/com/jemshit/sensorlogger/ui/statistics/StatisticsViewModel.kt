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

sealed class CalculationStatus {
    object Idle : CalculationStatus()
    object Loading : CalculationStatus()
    object Success : CalculationStatus()
    class Error(val message: String) : CalculationStatus()
}

class StatisticsViewModel(app: Application) : AndroidViewModel(app) {
    private val _calculationStatus: MutableLiveData<CalculationStatus> = MutableLiveData()
    val calculationStatus: LiveData<CalculationStatus> = _calculationStatus
    var statistics: Map<String, ActivityStatistics> = mapOf()
    private var calculationJob: Job? = null
    private val sensorValueRepository by lazy {
        SensorValueRepository.getInstance(getApplication<SensorLoggerApplication>().applicationContext)
    }

    // todo dont record while work
    init {
        _calculationStatus.value = CalculationStatus.Idle
    }

    // todo enable end delay always and propagate when stop is clicked
    // todo when exporting, select sensors or activities
    // todo ignore accuracies low, unreliable, unknown
    fun calculateStatistics() {
        if (isServiceRunningInForeground(getApplication<SensorLoggerApplication>().applicationContext, SensorLoggerService::class.java)) {
            _calculationStatus.value = CalculationStatus.Error(
                    getApplication<SensorLoggerApplication>().applicationContext.getString(R.string.error_statistics_recording_is_alive)
            )
        } else {
            _calculationStatus.value = CalculationStatus.Loading
            calculationJob = launch(IO) {
                try {
                    statistics = sensorValueRepository.getDistinctStatistics()

                    _calculationStatus.postValue(CalculationStatus.Success)
                } catch (e: Exception) {
                    _calculationStatus.postValue(CalculationStatus.Error(
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