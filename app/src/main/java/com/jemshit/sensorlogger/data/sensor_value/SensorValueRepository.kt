package com.jemshit.sensorlogger.data.sensor_value

import android.content.Context
import com.jemshit.sensorlogger.helper.SingletonHolder
import com.jemshit.sensorlogger.data.AppDatabase
import io.reactivex.Flowable
import kotlinx.coroutines.experimental.launch

class SensorValueRepository private constructor() {
    private lateinit var sensorValueDao: SensorValueDao

    companion object : SingletonHolder<SensorValueRepository, Context>({
        val instance = SensorValueRepository()
        instance.sensorValueDao = AppDatabase.getInstance(it).sensorValueDao()
        instance
    })

    fun getByLimit(limit: Int): List<SensorValueEntity> {
        return sensorValueDao.get(limit)
    }

    fun getAllStream(): Flowable<List<SensorValueEntity>> {
        return sensorValueDao.getAllStream()
    }

    fun saveInBatch(entities: List<SensorValueEntity>) {
        launch {
            sensorValueDao.saveInTransaction(entities)
        }
    }
}