package com.jemshit.sensorlogger.data.sensor_value

import androidx.room.*
import io.reactivex.Flowable

@Entity
class SensorValueEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0,
                        val phoneUptime: Long,
                        val timestamp: Long,
                        val sensorName: String,
                        val sensorType: String,
                        val activityName: String,
                        val devicePosition: String,
                        val deviceOrientation: String,
                        val valueAccuracy: String,
                        val values: String)

@Dao
interface SensorValueDao {
    @Insert
    fun save(entities: List<SensorValueEntity>)

    @Insert
    fun saveSingle(entity: SensorValueEntity)

    @Transaction
    fun saveInTransaction(entities: List<SensorValueEntity>) {
        save(entities)
    }

    @Query("SELECT * FROM SensorValueEntity LIMIT :limit")
    fun get(limit: Int): List<SensorValueEntity>

    @Query("SELECT * FROM SensorValueEntity")
    fun getAllStream(): Flowable<List<SensorValueEntity>>

    @Query("SELECT * FROM SensorValueEntity ORDER BY timestamp DESC")
    fun getAllSorted(): List<SensorValueEntity>
}