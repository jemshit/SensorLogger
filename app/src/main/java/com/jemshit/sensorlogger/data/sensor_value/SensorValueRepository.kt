package com.jemshit.sensorlogger.data.sensor_value

import android.content.Context
import android.database.Cursor
import com.jemshit.sensorlogger.data.AppDatabase
import com.jemshit.sensorlogger.helper.SingletonHolder

class SensorValueRepository private constructor() {
    private lateinit var sensorValueDao: SensorValueDao

    companion object : SingletonHolder<SensorValueRepository, Context>({
        val instance = SensorValueRepository()
        instance.sensorValueDao = AppDatabase.getInstance(it).sensorValueDao()
        instance
    })

    fun getAllSortedCursor(): Cursor {
        return sensorValueDao.getAllSortedCursor()
    }

    // Must be called from background thread
    fun saveInBatch(entities: List<SensorValueEntity>) {
        sensorValueDao.saveInTransaction(entities)
    }

    // Must be called from background thread
    fun save(entity: SensorValueEntity) {
        sensorValueDao.saveSingle(entity)
    }

    fun getDistinctStatistics(): List<SensorValueDistinctEntity> {
        return sensorValueDao.getDistinctStatistics()
    }

    fun getDistinctStatisticsCount(activityName: String,
                                   positionName: String,
                                   orientationName: String): Int {
        return sensorValueDao.getDistinctStatisticsCount(activityName, positionName, orientationName)
    }
}