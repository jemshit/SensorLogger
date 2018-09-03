package com.jemshit.sensorlogger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jemshit.sensorlogger.helper.SingletonHolder
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceDao
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceEntity
import com.jemshit.sensorlogger.data.sensor_value.SensorValueDao
import com.jemshit.sensorlogger.data.sensor_value.SensorValueEntity

@Database(
        entities = [SensorPreferenceEntity::class, SensorValueEntity::class],
        version = 3,
        exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorPreferenceDao(): SensorPreferenceDao
    abstract fun sensorValueDao(): SensorValueDao

    companion object : SingletonHolder<AppDatabase, Context>({
        Room.databaseBuilder(it.applicationContext, AppDatabase::class.java, "app.db")
                .fallbackToDestructiveMigration()
                .build()
    })
}
