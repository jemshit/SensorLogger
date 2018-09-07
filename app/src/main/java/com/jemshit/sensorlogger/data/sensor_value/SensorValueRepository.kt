package com.jemshit.sensorlogger.data.sensor_value

import android.content.Context
import com.jemshit.sensorlogger.data.AppDatabase
import com.jemshit.sensorlogger.helper.SingletonHolder
import com.jemshit.sensorlogger.model.*

class SensorValueRepository private constructor() {
    private lateinit var sensorValueDao: SensorValueDao

    companion object : SingletonHolder<SensorValueRepository, Context>({
        val instance = SensorValueRepository()
        instance.sensorValueDao = AppDatabase.getInstance(it).sensorValueDao()
        instance
    })

    // Must be called from background thread
    fun saveInBatch(entities: List<SensorValueEntity>) {
        sensorValueDao.saveInTransaction(entities)
    }

    // Must be called from background thread
    fun save(entity: SensorValueEntity) {
        sensorValueDao.saveSingle(entity)
    }

    fun getDistinctStatistics(): Map<String, ActivityStatistics> {
        val distinctStats = sensorValueDao.getDistinctStatistics()
        return if (distinctStats.isEmpty())
            mapOf()
        else {
            val statistics: MutableMap<String, ActivityStatistics> = mutableMapOf()
            distinctStats.forEach { distinctEntity ->
                val count = sensorValueDao.getDistinctStatisticsCount(
                        distinctEntity.activityName,
                        distinctEntity.devicePosition,
                        distinctEntity.deviceOrientation,
                        distinctEntity.valueAccuracy,
                        distinctEntity.sensorType
                )

                // Activity does not exist
                if (statistics[distinctEntity.activityName] == null) {
                    statistics[distinctEntity.activityName] = createActivityStatistics(
                            distinctEntity,
                            createPositionStatistics(
                                    distinctEntity,
                                    createOrientationStatistics(
                                            distinctEntity,
                                            createSensorStatistics(distinctEntity, count)
                                    )
                            )
                    )

                } else {
                    // Activity exists
                    val activityStatistics = statistics[distinctEntity.activityName]!!
                    // Position does not exist
                    if (activityStatistics.positionStatistics.find { it.name.equals(distinctEntity.devicePosition, true) } == null) {
                        activityStatistics.positionStatistics.add(
                                createPositionStatistics(
                                        distinctEntity,
                                        createOrientationStatistics(
                                                distinctEntity,
                                                createSensorStatistics(distinctEntity, count)
                                        )
                                )
                        )

                    } else {
                        // Position exists
                        val positionStatistics = activityStatistics.positionStatistics.find { it.name.equals(distinctEntity.devicePosition, true) }!!
                        // Orientation does not exist
                        if (positionStatistics.orientationStatistics.find { it.name.equals(distinctEntity.deviceOrientation, true) } == null) {
                            positionStatistics.orientationStatistics.add(
                                    createOrientationStatistics(
                                            distinctEntity,
                                            createSensorStatistics(distinctEntity, count)
                                    )
                            )

                        } else {
                            // Orientation exists
                            val orientationStatistics = positionStatistics.orientationStatistics.find { it.name.equals(distinctEntity.deviceOrientation, true) }!!
                            // Sensor does not exist
                            if (orientationStatistics.sensorStatistics.find { it.type.equals(distinctEntity.sensorType, true) } == null) {
                                orientationStatistics.sensorStatistics.add(
                                        createSensorStatistics(distinctEntity, count)
                                )

                            } else {
                                // Sensor exists
                                val sensorStatistics = orientationStatistics.sensorStatistics.find { it.type.equals(distinctEntity.sensorType, true) }!!
                                sensorStatistics.increaseAccuracyCount(distinctEntity.valueAccuracy)
                            }

                        }
                    }
                }
            }

            statistics.toMap()
        }
    }


    private fun createSensorStatistics(entity: SensorValueDistinctEntity, count: Long): SensorStatistics {
        return SensorStatistics(type = entity.sensorType).also {
            it.increaseAccuracyCount(entity.valueAccuracy, count)
        }
    }

    private fun createOrientationStatistics(entity: SensorValueDistinctEntity, sensorStatistics: SensorStatistics): OrientationStatistics {
        return OrientationStatistics(
                name = if (entity.deviceOrientation.isBlank()) EMPTY_ORIENTATION else entity.deviceOrientation,
                sensorStatistics = mutableListOf(sensorStatistics)
        )
    }

    private fun createPositionStatistics(entity: SensorValueDistinctEntity, orientationStatistics: OrientationStatistics): PositionStatistics {
        return PositionStatistics(
                name = if (entity.devicePosition.isBlank()) EMPTY_ORIENTATION else entity.devicePosition,
                orientationStatistics = mutableListOf(orientationStatistics)
        )
    }

    private fun createActivityStatistics(entity: SensorValueDistinctEntity, positionStatistics: PositionStatistics): ActivityStatistics {
        return ActivityStatistics(
                name = if (entity.activityName.isBlank()) EMPTY_ORIENTATION else entity.activityName,
                positionStatistics = mutableListOf(positionStatistics)
        )
    }
}