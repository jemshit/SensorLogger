package com.jemshit.sensorlogger.data.sensor_preference

import android.content.Context
import com.jemshit.sensorlogger.data.AppDatabase
import com.jemshit.sensorlogger.helper.SingletonHolder
import io.reactivex.Flowable
import kotlinx.coroutines.experimental.launch

class SensorPreferenceRepository private constructor() {
    private lateinit var sensorPreferenceDao: SensorPreferenceDao

    companion object : SingletonHolder<SensorPreferenceRepository, Context>({
        val instance = SensorPreferenceRepository()
        instance.sensorPreferenceDao = AppDatabase.getInstance(it).sensorPreferenceDao()
        instance
    })

    fun getPreferences(onlyActive: Boolean = false): List<SensorPreferenceEntity> {
        return if (onlyActive)
            sensorPreferenceDao.getAllActive()
        else
            sensorPreferenceDao.getAll()
    }

    fun getPreferencesStream(): Flowable<List<SensorPreferenceEntity>> {
        return sensorPreferenceDao.getAllStream()
    }

    fun getPreference(id: String): SensorPreferenceEntity {
        return sensorPreferenceDao.get(id)
    }

    fun savePreference(entity: SensorPreferenceEntity) {
        launch {
            sensorPreferenceDao.save(entity)
        }
    }

    fun updatePreference(entity: SensorPreferenceEntity) {
        launch {
            sensorPreferenceDao.update(entity)
        }
    }
}