package com.jemshit.sensorlogger.data.sensor_value

import android.content.Context
import android.database.Cursor
import androidx.paging.PagedList
import androidx.paging.RxPagedListBuilder
import com.jemshit.sensorlogger.data.AppDatabase
import com.jemshit.sensorlogger.helper.SingletonHolder
import io.reactivex.BackpressureStrategy
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

    fun getAllSorted(pageSize: Int): Flowable<PagedList<SensorValueEntity>> {
        val config = PagedList.Config.Builder()
                .setPageSize(pageSize)
                .setPrefetchDistance(pageSize / 4)
                .setEnablePlaceholders(true)
                .setInitialLoadSizeHint(pageSize)
                .build()
        return RxPagedListBuilder(sensorValueDao.getAllSorted(), config)
                .buildFlowable(BackpressureStrategy.BUFFER)
    }

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
}