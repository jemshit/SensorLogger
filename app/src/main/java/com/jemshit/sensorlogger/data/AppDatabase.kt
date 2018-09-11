package com.jemshit.sensorlogger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceDao
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceEntity
import com.jemshit.sensorlogger.helper.SingletonHolder


@Database(
        entities = [SensorPreferenceEntity::class],
        version = 7,
        exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorPreferenceDao(): SensorPreferenceDao

    companion object : SingletonHolder<AppDatabase, Context>({
        Room.databaseBuilder(it.applicationContext, AppDatabase::class.java, "app.db")
                .fallbackToDestructiveMigration()
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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

val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
                "DROP TABLE StatisticsEntity")
    }
}

val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
                "DROP TABLE SensorValueEntity")
    }
}