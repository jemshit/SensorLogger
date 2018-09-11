package com.jemshit.sensorlogger.data.sensor_value

import android.database.Cursor
import androidx.room.*
import com.jemshit.sensorlogger.model.SensorLogEvent

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

class SensorValueDistinctEntity(val activityName: String,
                                val devicePosition: String,
                                val deviceOrientation: String,
                                val valueAccuracy: String,
                                val sensorType: String)

@Dao
interface SensorValueDao {
    @Insert
    fun save(entities: List<SensorValueEntity>)

    @Query("DELETE FROM SensorValueEntity")
    fun deleteAll()

    @Insert
    fun saveSingle(entity: SensorValueEntity)

    @Transaction
    fun saveInTransaction(entities: List<SensorValueEntity>) {
        save(entities)
    }

    @Query("SELECT * FROM SensorValueEntity LIMIT :limit")
    fun get(limit: Int): List<SensorValueEntity>

    @Query("SELECT * FROM SensorValueEntity ORDER BY timestamp ASC")
    fun getAllSortedCursor(): Cursor

    @Query("SELECT * FROM SensorValueEntity ORDER BY timestamp, phoneUptime, id ASC LIMIT :limit")
    fun getAllCursorFirstPage(limit: Int): Cursor

    @Query("SELECT * FROM SensorValueEntity WHERE (timestamp>:lastTimestamp AND phoneUptime > :lastPhoneUptime AND id > :lastId)  ORDER BY timestamp, phoneUptime, id ASC LIMIT :limit")
    fun getAllCursorNextPage(limit: Int, lastTimestamp: Long, lastPhoneUptime: Long, lastId: Long): Cursor

    @Query("SELECT DISTINCT activityName, devicePosition, deviceOrientation, valueAccuracy, sensorType FROM SensorValueEntity WHERE sensorType !=:eventName")
    fun getDistinctStatistics(eventName: String = SensorLogEvent.EVENT.eventName)
            : List<SensorValueDistinctEntity>

    @Query("SELECT COUNT(*) FROM SensorValueEntity WHERE activityName = :activityName and devicePosition =:positionName and deviceOrientation = :orientationName and valueAccuracy = :accuracyName and sensorType = :sensorType")
    fun getDistinctStatisticsCount(activityName: String,
                                   positionName: String,
                                   orientationName: String,
                                   accuracyName: String,
                                   sensorType: String): Long
}