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
import com.jemshit.sensorlogger.background_work.ARG_EXCLUDED_ACCURACIES
import com.jemshit.sensorlogger.background_work.ExportWorker
import com.jemshit.sensorlogger.background_work.SensorLoggerService
import com.jemshit.sensorlogger.background_work.isServiceRunningInForeground
import com.jemshit.sensorlogger.data.PREF_KEY_LAST_WORKERI_ID
import com.jemshit.sensorlogger.data.getDefaultSharedPreference
import com.jemshit.sensorlogger.helper.deleteExportFolder
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
    private val workManager: WorkManager = WorkManager.getInstance()
    private var lastWorkStatus: LiveData<WorkStatus>? = null
    private var lastWorkStatusObserver: Observer<WorkStatus>? = null
    private val gson = Gson()
    private val stringListType: Type = object : TypeToken<List<String>>() {}.type

    init {
        _exportStatus.value = UIWorkStatus.Loading
        _deleteFolderStatus.value = UIWorkStatus.Idle

        val lastWorkId = getDefaultSharedPreference(application.applicationContext).getString(PREF_KEY_LAST_WORKERI_ID, "")!!
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
                        val lastWorkId = getDefaultSharedPreference(getApplication<SensorLoggerApplication>().applicationContext).getString(PREF_KEY_LAST_WORKERI_ID, "")!!
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

    fun export(excludedAccuracies: Array<String>) {
        if (isServiceRunningInForeground(getApplication<SensorLoggerApplication>().applicationContext, SensorLoggerService::class.java)) {
            _exportStatus.value = UIWorkStatus.Error(
                    getApplication<SensorLoggerApplication>().applicationContext.getString(R.string.error_export_recording_is_alive)
            )
        } else {
            _exportStatus.value = UIWorkStatus.Loading

            val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()

            val inputData = Data.Builder()
            inputData.putStringArray(ARG_EXCLUDED_ACCURACIES, excludedAccuracies)

            val workRequest = OneTimeWorkRequest
                    .Builder(ExportWorker::class.java)
                    .setConstraints(constraints)
                    .setInputData(inputData.build())
                    .build()

            getDefaultSharedPreference(getApplication<SensorLoggerApplication>().applicationContext).edit {
                putString(PREF_KEY_LAST_WORKERI_ID, workRequest.id.toString())
            }

            workManager.enqueue(workRequest)
            observeLastWorkState(workRequest.id.toString())
        }
    }

    fun deleteExportedData() {
        launch(IO) {
            val success = deleteExportFolder(getApplication<SensorLoggerApplication>().applicationContext)
            if (success)
                _deleteFolderStatus.postValue(UIWorkStatus.Success)
            else
                _deleteFolderStatus.postValue(UIWorkStatus.Error("Could not delete Export Folder."))
        }
    }

    override fun onCleared() {
        super.onCleared()
        lastWorkStatusObserver?.let {
            lastWorkStatus?.removeObserver(it)
        }
    }
}