package com.jemshit.sensorlogger.data.statistics

import androidx.room.*
import androidx.work.State
import io.reactivex.Flowable

@Entity
data class StatisticsEntity(@PrimaryKey val id: String,
                            var status: String = State.ENQUEUED.name,
                            val requestTime: Long = System.currentTimeMillis(),
                            var startTime: Long = 0,
                            var finishTime: Long = 0,
                            var activityStatistics: String = "",
                            var loggedErrors: String = "")


@Dao
interface StatisticsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(entity: StatisticsEntity)

    @Update
    fun update(vararg entities: StatisticsEntity)

    @Query("SELECT * FROM StatisticsEntity WHERE status = 'ENQUEUED' OR status = 'RUNNING' OR status = 'BLOCKED'")
    fun getAllUnfinished(): Flowable<List<StatisticsEntity>>

    @Query("SELECT * FROM StatisticsEntity WHERE status = 'SUCCEEDED' OR status = 'FAILED' OR status = 'CANCELLED'")
    fun getAllFinished(): List<StatisticsEntity>

    @Query("SELECT * FROM StatisticsEntity WHERE id = :id LIMIT 1")
    fun get(id: String): StatisticsEntity

    @Delete
    fun delete(entity: StatisticsEntity)
}