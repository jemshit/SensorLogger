package com.jemshit.sensorlogger.data.sensor_value

import android.content.Context
import android.database.Cursor
import com.jemshit.sensorlogger.background_work.*
import com.jemshit.sensorlogger.data.AppDatabase
import com.jemshit.sensorlogger.helper.SingletonHolder
import com.jemshit.sensorlogger.model.*
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

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

    fun getAllSortedCursor(): Cursor {
        return sensorValueDao.getAllSortedCursor()
    }

    private fun calculateStatisticsAsync(statistics: MutableMap<String, ActivityStatistics>,
                                         distinctEntity: SensorValueDistinctEntity) = async<Unit>(start = CoroutineStart.LAZY, context = DefaultDispatcher) {
        val count = sensorValueDao.getDistinctStatisticsCount(
                distinctEntity.activityName,
                distinctEntity.devicePosition,
                distinctEntity.deviceOrientation,
                distinctEntity.valueAccuracy,
                distinctEntity.sensorType
        )

        // Activity does not exist
        val activityName = if (distinctEntity.activityName.isBlank()) EMPTY_ACTIVITY else distinctEntity.activityName
        if (statistics[activityName] == null) {
            statistics[activityName] = createActivityStatistics(
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
            val activityStatistics = statistics[activityName]!!
            // Position does not exist
            val positionName = if (distinctEntity.devicePosition.isBlank()) EMPTY_POSITION else distinctEntity.devicePosition
            if (activityStatistics.positionStatistics.find { it.name.equals(positionName, true) } == null) {
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
                val positionStatistics = activityStatistics.positionStatistics.find { it.name.equals(positionName, true) }!!
                // Orientation does not exist
                val orientationName = if (distinctEntity.deviceOrientation.isBlank()) EMPTY_ORIENTATION else distinctEntity.deviceOrientation
                if (positionStatistics.orientationStatistics.find { it.name.equals(orientationName, true) } == null) {
                    positionStatistics.orientationStatistics.add(
                            createOrientationStatistics(
                                    distinctEntity,
                                    createSensorStatistics(distinctEntity, count)
                            )
                    )

                } else {
                    // Orientation exists
                    val orientationStatistics = positionStatistics.orientationStatistics.find { it.name.equals(orientationName, true) }!!
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

    private fun calculateSensorAccuracyStatisticsAsync(activityStats: ActivityStatistics) = async<Unit>(start = CoroutineStart.LAZY, context = DefaultDispatcher) {
        val sensorAccuracyStats = mutableMapOf<String, MutableList<Pair<String, Long>>>()

        activityStats.positionStatistics.forEach { positionStats ->
            positionStats.orientationStatistics.forEach { orientationStats ->
                orientationStats.sensorStatistics.forEach { sensorStats ->

                    if (sensorAccuracyStats.contains(sensorStats.type)) {
                        val currentPairs = sensorAccuracyStats[sensorStats.type]!!

                        if (sensorStats.highAccuracyCount != 0L) {
                            val accuracyFound = currentPairs.find { it.first.equals(ACCURACY_HIGH_TEXT, true) }
                            val newAccuracy: Pair<String, Long>
                            newAccuracy = if (accuracyFound != null)
                                Pair(ACCURACY_HIGH_TEXT, accuracyFound.second + sensorStats.highAccuracyCount)
                            else
                                Pair(ACCURACY_HIGH_TEXT, sensorStats.highAccuracyCount)

                            currentPairs.remove(accuracyFound)
                            currentPairs.add(newAccuracy)
                        }
                        if (sensorStats.mediumAccuracyCount != 0L) {
                            val accuracyFound = currentPairs.find { it.first.equals(ACCURACY_MEDIUM_TEXT, true) }
                            val newAccuracy: Pair<String, Long>
                            newAccuracy = if (accuracyFound != null)
                                Pair(ACCURACY_MEDIUM_TEXT, accuracyFound.second + sensorStats.mediumAccuracyCount)
                            else
                                Pair(ACCURACY_MEDIUM_TEXT, sensorStats.mediumAccuracyCount)

                            currentPairs.remove(accuracyFound)
                            currentPairs.add(newAccuracy)
                        }
                        if (sensorStats.lowAccuracyCount != 0L) {
                            val accuracyFound = currentPairs.find { it.first.equals(ACCURACY_LOW_TEXT, true) }
                            val newAccuracy: Pair<String, Long>
                            newAccuracy = if (accuracyFound != null)
                                Pair(ACCURACY_LOW_TEXT, accuracyFound.second + sensorStats.lowAccuracyCount)
                            else
                                Pair(ACCURACY_LOW_TEXT, sensorStats.lowAccuracyCount)

                            currentPairs.remove(accuracyFound)
                            currentPairs.add(newAccuracy)
                        }
                        if (sensorStats.unreliableAccuracyCount != 0L) {
                            val accuracyFound = currentPairs.find { it.first.equals(ACCURACY_UNRELIABLE_TEXT, true) }
                            val newAccuracy: Pair<String, Long>
                            newAccuracy = if (accuracyFound != null)
                                Pair(ACCURACY_UNRELIABLE_TEXT, accuracyFound.second + sensorStats.unreliableAccuracyCount)
                            else
                                Pair(ACCURACY_UNRELIABLE_TEXT, sensorStats.unreliableAccuracyCount)

                            currentPairs.remove(accuracyFound)
                            currentPairs.add(newAccuracy)
                        }
                        if (sensorStats.unknownAccuracyCount != 0L) {
                            val accuracyFound = currentPairs.find { it.first.equals(ACCURACY_UNKNOWN_TEXT, true) }
                            val newAccuracy: Pair<String, Long>
                            newAccuracy = if (accuracyFound != null)
                                Pair(ACCURACY_UNKNOWN_TEXT, accuracyFound.second + sensorStats.unknownAccuracyCount)
                            else
                                Pair(ACCURACY_UNKNOWN_TEXT, sensorStats.unknownAccuracyCount)

                            currentPairs.remove(accuracyFound)
                            currentPairs.add(newAccuracy)
                        }

                    } else {
                        val pairs: MutableList<Pair<String, Long>> = mutableListOf()

                        if (sensorStats.highAccuracyCount != 0L) pairs.add(Pair(ACCURACY_HIGH_TEXT, sensorStats.highAccuracyCount))
                        if (sensorStats.mediumAccuracyCount != 0L) pairs.add(Pair(ACCURACY_MEDIUM_TEXT, sensorStats.mediumAccuracyCount))
                        if (sensorStats.lowAccuracyCount != 0L) pairs.add(Pair(ACCURACY_LOW_TEXT, sensorStats.lowAccuracyCount))
                        if (sensorStats.unreliableAccuracyCount != 0L) pairs.add(Pair(ACCURACY_UNRELIABLE_TEXT, sensorStats.unreliableAccuracyCount))
                        if (sensorStats.unknownAccuracyCount != 0L) pairs.add(Pair(ACCURACY_UNKNOWN_TEXT, sensorStats.unknownAccuracyCount))

                        sensorAccuracyStats[sensorStats.type] = pairs
                    }

                    Unit
                }
            }
        }

        activityStats.sensorAccuracyStatistics = sensorAccuracyStats.toMap()
    }

    suspend fun getDistinctStatistics(): Map<String, ActivityStatistics> {
        val distinctStats = sensorValueDao.getDistinctStatistics()
        return if (distinctStats.isEmpty())
            mapOf()
        else {
            val statistics: MutableMap<String, ActivityStatistics> = mutableMapOf()
            // create async jobs
            val deferredJobs: MutableList<Deferred<Unit>> = mutableListOf()
            distinctStats.forEach { distinctEntity ->
                deferredJobs.add(calculateStatisticsAsync(statistics, distinctEntity))
            }
            // start jobs
            deferredJobs.forEach { it.start() }
            // wait for all jobs' completion
            deferredJobs.forEach { it.await() }

            // create async sensorAccuracy jobs
            val deferredSensorAccuracyJobs: MutableList<Deferred<Unit>> = mutableListOf()
            statistics.values.forEach { activityStats ->
                deferredSensorAccuracyJobs.add(calculateSensorAccuracyStatisticsAsync(activityStats))
            }
            // start jobs
            deferredSensorAccuracyJobs.forEach { it.start() }
            // wait for all jobs' completion
            deferredSensorAccuracyJobs.forEach { it.await() }

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
                name = if (entity.devicePosition.isBlank()) EMPTY_POSITION else entity.devicePosition,
                orientationStatistics = mutableListOf(orientationStatistics)
        )
    }

    private fun createActivityStatistics(entity: SensorValueDistinctEntity, positionStatistics: PositionStatistics): ActivityStatistics {
        return ActivityStatistics(
                name = if (entity.activityName.isBlank()) EMPTY_ACTIVITY else entity.activityName,
                positionStatistics = mutableListOf(positionStatistics),
                durationInMs = 0L
        )
    }
}