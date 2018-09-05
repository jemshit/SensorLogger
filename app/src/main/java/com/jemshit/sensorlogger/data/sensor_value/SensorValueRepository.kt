package com.jemshit.sensorlogger.data.sensor_value

import android.content.Context
import com.jemshit.sensorlogger.data.AppDatabase
import com.jemshit.sensorlogger.helper.SingletonHolder
import io.reactivex.Flowable

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

    fun getAllSorted(): List<SensorValueEntity> {
        return sensorValueDao.getAllSorted()
    }

    fun getAllStream(): Flowable<List<SensorValueEntity>> {
        return sensorValueDao.getAllStream()
    }

    // Must be called from background thread
    fun saveInBatch(entities: List<SensorValueEntity>) {
        sensorValueDao.saveInTransaction(entities)
    }

    // Must be called from background thread
    fun save(entity: SensorValueEntity) {
        sensorValueDao.saveSingle(entity)
    }
}