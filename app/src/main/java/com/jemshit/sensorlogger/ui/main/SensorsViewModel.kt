package com.jemshit.sensorlogger.ui.main

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceEntity
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceRepository
import com.jemshit.sensorlogger.model.SensorWithPreference
import com.jemshit.sensorlogger.model.uniqueId
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SensorsViewModel(application: Application) : AndroidViewModel(application) {
    // A ViewModel must never reference a view, Lifecycle, or any class that may hold a reference to the activity context.
    val sensorManager: SensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensorPreferenceRepository = SensorPreferenceRepository.getInstance(application.applicationContext)
    private val compositeDisposable = CompositeDisposable()


    private var _sensorsWithPreferences: MutableLiveData<List<SensorWithPreference>> = MutableLiveData()
    var sensorsWithPreferences: LiveData<List<SensorWithPreference>> = _sensorsWithPreferences
    private val _sensorsWithPreferencesLocal: MutableList<SensorWithPreference> = mutableListOf()

    fun retrieveSensorList() {
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)

        compositeDisposable.add(
                sensorPreferenceRepository.getPreferencesStream()
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.computation())
                        .doOnNext { sensorPreferences ->
                            updateSensorsWithPreferences(sensors, sensorPreferences)
                        }
                        .subscribe()
        )
    }

    private fun updateSensorsWithPreferences(sensors: List<Sensor>, sensorPreferences: List<SensorPreferenceEntity>) {
        val sensorsWithPreference = sensors.map { sensor ->
            val preference = sensorPreferences.find {
                it.id.equals(sensor.uniqueId, true)
            }

            SensorWithPreference(sensor, preference)
        }

        _sensorsWithPreferencesLocal.clear()
        _sensorsWithPreferencesLocal.addAll(sensorsWithPreference)

        _sensorsWithPreferences.postValue(sensorsWithPreference)
    }

    fun getSensorWithPreference(id: String): SensorWithPreference? {
        return _sensorsWithPreferencesLocal.firstOrNull { model ->
            model.sensor.uniqueId.equals(id, true)
        }
    }

    // todo while writing to file, write DEV user phoneId + PHONE: model, name, manufacturer, SAMPLING: 20hz
    // todo there can be max 3 foreground service at same time, when memory is needed, oldest one dies (dont listen to music)
    fun saveSensorPreference(model: SensorPreferenceEntity) {
        sensorPreferenceRepository.savePreference(model)
    }

    fun updateSensorPreference(model: SensorPreferenceEntity) {
        sensorPreferenceRepository.updatePreference(model)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }
}
