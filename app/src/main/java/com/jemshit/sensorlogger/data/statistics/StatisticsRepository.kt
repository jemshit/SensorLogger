package com.jemshit.sensorlogger.data.statistics

import android.content.Context
import com.jemshit.sensorlogger.data.AppDatabase
import com.jemshit.sensorlogger.helper.SingletonHolder
import io.reactivex.Flowable

class StatisticsRepository private constructor() {
    private lateinit var statisticsDao: StatisticsDao

    companion object : SingletonHolder<StatisticsRepository, Context>({
        val instance = StatisticsRepository()
        instance.statisticsDao = AppDatabase.getInstance(it).statisticsDao()
        instance
    })

    fun save(entity: StatisticsEntity) {
        statisticsDao.save(entity)
    }

    fun update(entity: StatisticsEntity) {
        statisticsDao.update(entity)
    }

    fun getAllFinished(): List<StatisticsEntity> {
        return statisticsDao.getAllFinished()
    }

    fun getAllUnfinished(): Flowable<List<StatisticsEntity>> {
        return statisticsDao.getAllUnfinished()
    }

    fun getById(id: String): StatisticsEntity {
        return statisticsDao.get(id)
    }

    fun delete(entity: StatisticsEntity) {
        return statisticsDao.delete(entity)
    }
}