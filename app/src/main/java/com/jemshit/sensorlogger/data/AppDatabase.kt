package com.jemshit.sensorlogger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceDao
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceEntity
import com.jemshit.sensorlogger.data.sensor_value.SensorValueDao
import com.jemshit.sensorlogger.data.sensor_value.SensorValueEntity
import com.jemshit.sensorlogger.data.statistics.StatisticsDao
import com.jemshit.sensorlogger.data.statistics.StatisticsEntity
import com.jemshit.sensorlogger.helper.SingletonHolder


@Database(
        entities = [SensorPreferenceEntity::class, SensorValueEntity::class, StatisticsEntity::class],
        version = 5,
        exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorPreferenceDao(): SensorPreferenceDao
    abstract fun sensorValueDao(): SensorValueDao
    abstract fun statisticsDao(): StatisticsDao

    companion object : SingletonHolder<AppDatabase, Context>({
        Room.databaseBuilder(it.applicationContext, AppDatabase::class.java, "app.db")
                .fallbackToDestructiveMigration()
                .addMigrations(MIGRATION_4_5)
                .build()
    })
}

val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
                "CREATE TABLE `StatisticsEntity` (`id` TEXT NOT NULL, "
                        + "`status` TEXT NOT NULL, "
                        + "`requestTime` INTEGER NOT NULL, "
                        + "`startTime` INTEGER NOT NULL, "
                        + "`finishTime` INTEGER NOT NULL, "
                        + "`activityStatistics` TEXT NOT NULL, "
                        + "`loggedErrors` TEXT NOT NULL, "
                        + "PRIMARY KEY(`id`))")
    }
}