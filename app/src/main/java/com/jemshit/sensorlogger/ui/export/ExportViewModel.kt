package com.jemshit.sensorlogger.ui.export

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.SensorLoggerApplication
import com.jemshit.sensorlogger.background_work.*
import com.jemshit.sensorlogger.data.PREF_KEY_LAST_WORKER_ID
import com.jemshit.sensorlogger.data.getDefaultSharedPreference
import com.jemshit.sensorlogger.data.sensor_value.SensorValueRepository
import com.jemshit.sensorlogger.helper.deleteExportFolder
import com.jemshit.sensorlogger.ui.main.statisticsBusy
import com.jemshit.sensorlogger.ui.statistics.UIWorkStatus
import kotlinx.coroutines.experimental.IO
import kotlinx.coroutines.experimental.launch
import java.lang.reflect.Type
import java.util.*

class ExportViewModel(application: Application) : AndroidViewModel(application) {
    private val _exportStatus: MutableLiveData<UIWorkStatus> = MutableLiveData()
    val exportStatus: LiveData<UIWorkStatus> = _exportStatus
    private val _deleteFolderStatus: MutableLiveData<UIWorkStatus> = MutableLiveData()
    val deleteFolderStatus: LiveData<UIWorkStatus> = _deleteFolderStatus
    private val _deleteLocalStatus: MutableLiveData<UIWorkStatus> = MutableLiveData()
    val deleteLocalStatus: LiveData<UIWorkStatus> = _deleteLocalStatus

    private val workManager: WorkManager = WorkManager.getInstance()
    private var lastWorkStatus: LiveData<WorkStatus>? = null
    private var lastWorkStatusObserver: Observer<WorkStatus>? = null
    private val gson = Gson()
    private val stringListType: Type = object : TypeToken<List<String>>() {}.type
    private val sensorValueRepository by lazy {
        SensorValueRepository.getInstance(getApplication<SensorLoggerApplication>().applicationContext)
    }

    init {
        _exportStatus.value = UIWorkStatus.Loading
        _deleteFolderStatus.value = UIWorkStatus.Idle
        _deleteLocalStatus.value = UIWorkStatus.Idle

        val lastWorkId = getDefaultSharedPreference(application.applicationContext).getString(PREF_KEY_LAST_WORKER_ID, "")!!
        if (lastWorkId.isBlank()) {
            _exportStatus.value = UIWorkStatus.Idle
        } else {
            observeLastWorkState(lastWorkId)
        }
    }

    private fun observeLastWorkState(lastWorkId: String) {
        // remove observer
        lastWorkStatus?.let {
            lastWorkStatusObserver?.let { observer ->
                it.removeObserver(observer)
            }
        }

        lastWorkStatusObserver = Observer { workStatus ->
            if (workStatus == null) {
                _exportStatus.value = UIWorkStatus.Idle
            } else if (!workStatus.state.isFinished) {
                _exportStatus.value = UIWorkStatus.Loading
            } else {
                when (workStatus.state) {
                    androidx.work.State.SUCCEEDED -> {
                        _exportStatus.value = UIWorkStatus.Success
                    }
                    androidx.work.State.FAILED -> {
                        val lastWorkId = getDefaultSharedPreference(getApplication<SensorLoggerApplication>().applicationContext).getString(PREF_KEY_LAST_WORKER_ID, "")!!
                        if (lastWorkId.isBlank()) {
                            _exportStatus.postValue(UIWorkStatus.Error("Failed"))
                        } else {
                            val loggedErrors = getDefaultSharedPreference(getApplication<SensorLoggerApplication>().applicationContext).getString(lastWorkId, "")!!
                            if (loggedErrors.isBlank()) {
                                val loggedErrorsAsList = gson.fromJson<List<String>>(loggedErrors, stringListType)
                                val loggedErrorsAsString = StringBuilder()
                                loggedErrorsAsList.forEach {
                                    loggedErrorsAsString.append(it)
                                    loggedErrorsAsString.append("\n")
                                }
                                _exportStatus.postValue(UIWorkStatus.Error("Failed:\n $loggedErrorsAsString"))
                            } else {
                                _exportStatus.postValue(UIWorkStatus.Error("Failed\n$loggedErrors"))
                            }
                        }
                    }
                    androidx.work.State.CANCELLED -> {
                        _exportStatus.value = UIWorkStatus.Error("Cancelled")
                    }
                    else -> {
                        _exportStatus.value = UIWorkStatus.Error("Unknown Work State")
                    }
                }
            }
        }

        // observe
        lastWorkStatus = workManager.getStatusById(UUID.fromString(lastWorkId))
        lastWorkStatus?.observeForever(lastWorkStatusObserver!!)
    }

    fun export(excludedAccuracies: Array<String>, age: String, weight: String, height: String, gender: String) {
        if (isServiceRunningInForeground(getApplication<SensorLoggerApplication>().applicationContext, SensorLoggerService::class.java)) {
            _exportStatus.value = UIWorkStatus.Error(
                    getApplication<SensorLoggerApplication>().applicationContext.getString(R.string.error_export_recording_is_alive)
            )
        } else if (statisticsBusy) {
            _exportStatus.value = UIWorkStatus.Error(
                    getApplication<SensorLoggerApplication>().applicationContext.getString(R.string.error_export_statistics_is_busy)
            )
        } else {
            _exportStatus.value = UIWorkStatus.Loading

            val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .build()

            val inputData = Data.Builder()
            inputData.putStringArray(ARG_EXCLUDED_ACCURACIES, excludedAccuracies)
            inputData.putString(ARG_AGE, age)
            inputData.putString(ARG_WEIGHT, weight)
            inputData.putString(ARG_HEIGHT, height)
            inputData.putString(ARG_GENDER, gender)

            val workRequest = OneTimeWorkRequest
                    .Builder(ExportWorker::class.java)
                    .setConstraints(constraints)
                    .setInputData(inputData.build())
                    .build()

            getDefaultSharedPreference(getApplication<SensorLoggerApplication>().applicationContext).edit {
                putString(PREF_KEY_LAST_WORKER_ID, workRequest.id.toString())
            }

            workManager.enqueue(workRequest)
            observeLastWorkState(workRequest.id.toString())
        }
    }

    fun deleteExportedData() {
        _deleteFolderStatus.value = UIWorkStatus.Loading

        launch(IO) {
            val success = deleteExportFolder(getApplication<SensorLoggerApplication>().applicationContext)
            if (success)
                _deleteFolderStatus.postValue(UIWorkStatus.Success)
            else
                _deleteFolderStatus.postValue(UIWorkStatus.Error("Could not delete Export Folder."))
        }
    }

    fun deleteLocalData() {
        _deleteLocalStatus.value = UIWorkStatus.Loading

        if (isServiceRunningInForeground(getApplication<SensorLoggerApplication>().applicationContext, SensorLoggerService::class.java)) {
            _deleteLocalStatus.value = UIWorkStatus.Error(
                    getApplication<SensorLoggerApplication>().applicationContext.getString(R.string.error_export_recording_is_alive)
            )
        } else if (statisticsBusy) {
            _deleteLocalStatus.value = UIWorkStatus.Error(
                    getApplication<SensorLoggerApplication>().applicationContext.getString(R.string.error_delete_local_statistics_is_busy)
            )
        } else {
            launch(IO) {
                try {
                    sensorValueRepository.deleteAll()
                    _deleteLocalStatus.postValue(UIWorkStatus.Success)
                } catch (e: Exception) {
                    _deleteLocalStatus.postValue(UIWorkStatus.Error("Could not delete Local Data ${e.message}"))
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        lastWorkStatusObserver?.let {
            lastWorkStatus?.removeObserver(it)
        }
    }
}