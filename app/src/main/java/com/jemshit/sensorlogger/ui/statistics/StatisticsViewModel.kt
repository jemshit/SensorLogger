package com.jemshit.sensorlogger.ui.statistics

import android.app.Application
import android.content.Context
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.SensorLoggerApplication
import com.jemshit.sensorlogger.background_work.SensorLoggerService
import com.jemshit.sensorlogger.background_work.isServiceRunningInForeground
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceEntity
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceRepository
import com.jemshit.sensorlogger.data.sensor_value.SensorValueRepository
import com.jemshit.sensorlogger.model.ActivityStatistics
import com.jemshit.sensorlogger.model.DEFAULT_SAMPLING_PERIOD_CUSTOM
import com.jemshit.sensorlogger.model.getSensorTypePre20
import com.jemshit.sensorlogger.model.maxFrequency
import kotlinx.coroutines.experimental.*

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
    private var sensorPreferences: List<SensorPreferenceEntity> = listOf()
    private var calculationJob: Job? = null
    private val sensorValueRepository by lazy {
        SensorValueRepository.getInstance(getApplication<SensorLoggerApplication>().applicationContext)
    }
    private val sensorPreferenceRepository by lazy {
        SensorPreferenceRepository.getInstance(getApplication<SensorLoggerApplication>().applicationContext)
    }
    private val sensorManager: SensorManager by lazy {
        getApplication<SensorLoggerApplication>().applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
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

                    sensorPreferences = getSensorPreferencesAsync().await()

                    // create async jobs
                    val deferredJobs: MutableList<Deferred<Unit>> = mutableListOf()
                    statistics.forEach { entry ->
                        deferredJobs.add(getActivityDurationAsync(entry.value))
                    }
                    // start jobs
                    deferredJobs.forEach { it.start() }
                    // wait for all jobs' completion
                    deferredJobs.forEach { it.await() }

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

    private fun getSensorPreferencesAsync() = async(IO) {
        sensorPreferenceRepository.getPreferences(onlyActive = false)
    }

    private fun getActivityDurationAsync(stats: ActivityStatistics) = async(DefaultDispatcher, start = CoroutineStart.LAZY) {

        val sensorAndCounts: MutableMap<String, Long> = mutableMapOf()
        stats.sensorAccuracyStatistics.forEach { entry ->
            var totalCount = 0L
            entry.value.forEach {
                totalCount += it.second
            }
            sensorAndCounts[entry.key] = totalCount
        }

        var duration = 0L // seconds
        sensorAndCounts.forEach { entry ->
            val preference = sensorPreferences.find { entry.key.equals(getSensorTypePre20(it.sensorType), true) }
            preference?.let {
                val maxFreqSensor = sensorManager.getDefaultSensor(it.sensorType)?.maxFrequency
                        ?: 100f

                if (it.samplingPeriodCustom != DEFAULT_SAMPLING_PERIOD_CUSTOM) {
                    duration += entry.value / it.samplingPeriodCustom
                } else {
                    when (it.samplingPeriod) {
                        SensorManager.SENSOR_DELAY_FASTEST -> duration += (entry.value / maxFreqSensor).toLong()
                        SensorManager.SENSOR_DELAY_GAME -> duration += entry.value / 50
                        SensorManager.SENSOR_DELAY_UI -> duration += (entry.value / 16.6f).toLong()
                        SensorManager.SENSOR_DELAY_NORMAL -> duration += entry.value / 5
                        else -> 20f
                    }
                }
            }
        }
        stats.durationInMs = duration * 1000

    }

    override fun onCleared() {
        super.onCleared()
        calculationJob?.cancel()
    }
}