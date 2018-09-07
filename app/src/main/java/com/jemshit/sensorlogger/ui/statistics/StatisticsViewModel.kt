package com.jemshit.sensorlogger.ui.statistics

import android.app.Application
import android.util.Log.d
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.*
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.SensorLoggerApplication
import com.jemshit.sensorlogger.background_work.SensorLoggerService
import com.jemshit.sensorlogger.background_work.StatisticsWorker
import com.jemshit.sensorlogger.background_work.isServiceRunningInForeground
import com.jemshit.sensorlogger.data.sensor_value.SensorValueRepository
import com.jemshit.sensorlogger.data.statistics.StatisticsEntity
import com.jemshit.sensorlogger.data.statistics.StatisticsRepository
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.IO
import kotlinx.coroutines.experimental.launch
import java.util.*

sealed class CalculationStatus {
    object Idle : CalculationStatus()
    object Loading : CalculationStatus()
    object Success : CalculationStatus()    // todo ayri yerde de tut, cunku error(stop recording gibi) gosterirsen bunu kaybedersin
    class Error(val message: String) : CalculationStatus()
}

class StatisticsViewModel(app: Application) : AndroidViewModel(app) {
    private val _calculationStatus: MutableLiveData<CalculationStatus> = MutableLiveData()
    val calculationStatus: LiveData<CalculationStatus> = _calculationStatus
    var statistics: StatisticsEntity? = null
    private val workManager: WorkManager = WorkManager.getInstance()
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()
    private val statisticsRepository by lazy {
        StatisticsRepository.getInstance(getApplication<SensorLoggerApplication>().applicationContext)
    }
    private val sensorValueRepository by lazy {
        SensorValueRepository.getInstance(getApplication<SensorLoggerApplication>().applicationContext)
    }
    private var unfinishedWorkStatus: LiveData<WorkStatus>? = null
    private var unfinishedWorkStatusObserver: Observer<WorkStatus>? = null

    // todo iki tab arasi oynarsan ve deepLoada tiklarsan, loadingde kaliyor
    // todo loading iken, tablar degistirisen success callback cok oluyor
    // todo dont record while work
    init {
        _calculationStatus.value = CalculationStatus.Loading

        // todo mini statistics
        launch {
            d("CustomLog", "mini statistics: ${sensorValueRepository.getDistinctStatistics()}")
        }

        statisticsRepository.getAllUnfinished()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = { unfinishedWorks ->
                    if (unfinishedWorks.isNotEmpty()) {
                        if (unfinishedWorks.size > 1) {
                            val oldStatisticsWork = unfinishedWorks.sortedBy { it.requestTime }.first()
                            workManager.cancelWorkById(UUID.fromString(oldStatisticsWork.id))
                            launch(IO) { statisticsRepository.delete(oldStatisticsWork) }
                        } else
                            registerToWorkStatus(unfinishedWorks[0])

                    } else {
                        launch(IO) {
                            val finishedWorks = statisticsRepository.getAllFinished()
                            if (finishedWorks.isEmpty())
                                _calculationStatus.postValue(CalculationStatus.Idle)
                            else {
                                statistics = finishedWorks
                                        .sortedBy { it.finishTime }
                                        .last()
                                _calculationStatus.postValue(CalculationStatus.Success)
                            }
                        }
                    }
                }, onError = {
                    _calculationStatus.value = CalculationStatus.Error(
                            getApplication<SensorLoggerApplication>().applicationContext.getString(R.string.error)
                    )
                })
                .addTo(compositeDisposable)
    }

    // todo enable end delay always and propagate when stop is clicked
    // todo when exporting, select sensors or activities
    // todo ignore accuracies low, unreliable, unknown

    private fun registerToWorkStatus(unfinishedWork: StatisticsEntity) {
        unfinishedWorkStatus?.let {
            unfinishedWorkStatusObserver?.let { observer ->
                it.removeObserver(observer)
            }
        }

        unfinishedWorkStatusObserver = Observer { workStatus ->
            if (workStatus == null) {
                launch(IO) {
                    val statistics = statisticsRepository.getById(unfinishedWork.id)
                    statistics.status = State.FAILED.name
                    statisticsRepository.update(statistics)
                }
            } else if (!workStatus.state.name.equals(unfinishedWork.status, true)) {
                launch(IO) {
                    val statistics = statisticsRepository.getById(unfinishedWork.id)
                    statistics.status = workStatus.state.name
                    statisticsRepository.update(statistics)
                }
            } else {
                // DB and WorkManager statuses are same
            }
        }
        unfinishedWorkStatus = workManager.getStatusById(UUID.fromString(unfinishedWork.id))
        unfinishedWorkStatus!!.observeForever(unfinishedWorkStatusObserver!!)
    }

    fun calculateDeepStatistics() {
        if (isServiceRunningInForeground(getApplication<SensorLoggerApplication>().applicationContext, SensorLoggerService::class.java)) {
            _calculationStatus.value = CalculationStatus.Error(
                    getApplication<SensorLoggerApplication>().applicationContext.getString(R.string.error_statistics_recording_is_alive)
            )
        } else {
            _calculationStatus.value = CalculationStatus.Loading

            launch {
                val constraints = Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                val workRequest = OneTimeWorkRequest
                        .Builder(StatisticsWorker::class.java)
                        .setConstraints(constraints)
                        .build()

                val statisticsEntity = StatisticsEntity(workRequest.id.toString())
                statisticsRepository.save(statisticsEntity)

                workManager.enqueue(workRequest)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
        unfinishedWorkStatusObserver?.let {
            unfinishedWorkStatus?.removeObserver(it)
        }
    }
}