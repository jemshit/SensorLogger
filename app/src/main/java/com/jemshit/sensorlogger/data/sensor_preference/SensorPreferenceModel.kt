package com.jemshit.sensorlogger.data.sensor_preference

import androidx.room.*
import io.reactivex.Flowable


@Entity
class SensorPreferenceEntity(@PrimaryKey val id: String,
                             var isChecked: Boolean,
                             var samplingPeriod: Int,
                             var samplingPeriodCustom: Int)

@Dao
interface SensorPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(entity: SensorPreferenceEntity)

    @Update
    fun update(vararg entities: SensorPreferenceEntity)

    @Query("SELECT * FROM SensorPreferenceEntity")
    fun getAll(): List<SensorPreferenceEntity>

    @Query("SELECT * FROM SensorPreferenceEntity")
    fun getAllStream(): Flowable<List<SensorPreferenceEntity>>

    @Query("SELECT * FROM SensorPreferenceEntity where id = :id LIMIT 1")
    fun get(id: String): SensorPreferenceEntity
}