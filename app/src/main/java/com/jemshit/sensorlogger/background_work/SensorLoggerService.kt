package com.jemshit.sensorlogger.background_work

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.*
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceEntity
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceRepository
import com.jemshit.sensorlogger.data.sensor_value.SensorValueEntity
import com.jemshit.sensorlogger.data.sensor_value.SensorValueRepository
import com.jemshit.sensorlogger.helper.RxBus
import com.jemshit.sensorlogger.model.*
import io.reactivex.disposables.Disposable
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import kotlinx.coroutines.experimental.*
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

const val ARG_START_DELAY = "start_delay"
const val ARG_ACTIVITY_NAME = "activity_name"
const val ARG_DEVICE_POSITION = "device_position"
const val ARG_DEVICE_ORIENTATION = "device_orientation"

internal val BACKGROUND_THREAD_POOL_CONTEXT = newFixedThreadPoolContext(2, "backgroundThreads")

class SensorLoggerService : Service() {

    private lateinit var sensorManager: SensorManager
    private val sensorEventListeners: MutableList<SensorEventListener> = mutableListOf()

    private lateinit var sensorValueRepository: SensorValueRepository
    private lateinit var sensorPreferenceRepository: SensorPreferenceRepository
    private lateinit var activeSensorPreferences: List<SensorPreferenceEntity>
    private lateinit var activeSensorsWithPreferences: List<SensorWithPreference>
    private lateinit var sensorValueProcessor: FlowableProcessor<SensorValueEntity>
    private var sensorValueDisposable: Disposable? = null

    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var initializerJob: Deferred<Unit>
    private lateinit var loggerJob: Job
    private lateinit var gson: Gson
    private lateinit var floatListType: Type

    private var startDelay: Int = 0
    private var activityName: String = ""
    private var devicePosition: String = ""
    private var deviceOrientation: String = ""

    private lateinit var ACCURACY_HIGH: String
    private lateinit var ACCURACY_MEDIUM: String
    private lateinit var ACCURACY_LOW: String
    private lateinit var ACCURACY_UNRELIABLE: String
    private lateinit var ACCURACY_UNKNOWN: String

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CustomLog", "onStartCommand")

        intent?.let { _ ->
            // Fill the local variables
            val bundle = intent.getBundleExtra(ARG_BUNDLE)
            bundle?.let { _ ->
                startDelay = bundle.getInt(ARG_START_DELAY, 0)
                activityName = bundle.getString(ARG_ACTIVITY_NAME, "")
                devicePosition = bundle.getString(ARG_DEVICE_POSITION, "")
                deviceOrientation = bundle.getString(ARG_DEVICE_ORIENTATION, "")

            }

            // Execute commands
            if (intent.getIntExtra(ARG_COMMAND, -1) != -1) {

                when (intent.getIntExtra(ARG_COMMAND, -1)) {
                    ServiceCommand.START.id -> {
                        loggerJob = launch(BACKGROUND_THREAD_POOL_CONTEXT) {
                            // Start after initialization is complete
                            initializerJob.await()

                            if (startDelay > 0)
                                delay(startDelay * 1000)

                            // Unregister if exists
                            sensorEventListeners.forEach {
                                sensorManager.unregisterListener(it)
                            }.also {
                                sensorEventListeners.clear()
                            }

                            // Persist to DB
                            // todo timespan bitmeden destroy olursa veri kaybi, timespani azalt. yada end delay hesaplarken bunu da hesaba kat
                            sensorValueDisposable = sensorValueProcessor
                                    .onBackpressureBuffer()
                                    .buffer(5, TimeUnit.SECONDS, 1000) // todo
                                    .onBackpressureBuffer()
                                    .subscribe(
                                            // onNext
                                            { values ->
                                                if (values.isNotEmpty())
                                                    sensorValueRepository.saveInBatch(values)
                                            },
                                            // onError
                                            {
                                                val epochTimeMs = System.currentTimeMillis()
                                                val nanoTimeNs = System.nanoTime()
                                                sensorValueRepository.save(
                                                        SensorValueEntity(
                                                                0,
                                                                nanoTimeNs,
                                                                epochTimeMs,
                                                                SensorLogEvent.SAVE_ERROR.eventName,
                                                                SensorLogEvent.EVENT.eventName,
                                                                activityName,
                                                                devicePosition,
                                                                deviceOrientation,
                                                                "",
                                                                ""
                                                        )
                                                )
                                            }
                                    )


                            // Sensor change listeners
                            activeSensorsWithPreferences.forEach { sensorWithPreference ->
                                val sensorEventListener = object : SensorEventListener2 {
                                    override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {}
                                    override fun onFlushCompleted(sensor: Sensor?) {}

                                    override fun onSensorChanged(sensorEvent: SensorEvent?) {
                                        sensorEvent?.let { event ->
                                            val epochTimeMs = System.currentTimeMillis()
                                            val nanoTimeNs = event.timestamp

                                            val accuracy = when (event.accuracy) {
                                                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> ACCURACY_HIGH
                                                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> ACCURACY_MEDIUM
                                                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> ACCURACY_LOW
                                                SensorManager.SENSOR_STATUS_UNRELIABLE -> ACCURACY_UNRELIABLE
                                                else -> ACCURACY_UNKNOWN
                                            }
                                            // todo loses precision of float
                                            val valuesEncoded: String = gson.toJson(event.values.toList(), floatListType)

                                            val entity = SensorValueEntity(
                                                    0,
                                                    nanoTimeNs,
                                                    epochTimeMs,
                                                    sensorWithPreference.sensor.name,
                                                    sensorWithPreference.sensor.getTypePre20(),
                                                    activityName,
                                                    devicePosition,
                                                    deviceOrientation,
                                                    accuracy,
                                                    valuesEncoded
                                            )
                                            sensorValueProcessor.onNext(entity)
                                        }
                                    }
                                }
                                sensorEventListeners.add(sensorEventListener)

                                val customSamplingFrequency = sensorWithPreference.preference!!.samplingPeriodCustom
                                if (customSamplingFrequency > 3) {  // 3 = SensorManager.SENSOR_DELAY_NORMAL
                                    val customSamplingFrequencyMicroSec = (1000 / customSamplingFrequency) * 1000
                                    sensorManager.registerListener(
                                            sensorEventListener,
                                            sensorWithPreference.sensor,
                                            customSamplingFrequencyMicroSec,
                                            0
                                    )
                                } else {
                                    sensorManager.registerListener(
                                            sensorEventListener,
                                            sensorWithPreference.sensor,
                                            sensorWithPreference.preference!!.samplingPeriod,
                                            0
                                    )
                                }
                            }

                        }
                    }

                    ServiceCommand.STOP.id -> {
                        stopForeground(true)
                        stopSelf()
                    }

                    ServiceCommand.PUBLISH_ARGUMENTS.id -> {
                        val bundle = Bundle()
                        bundle.putString(ARG_ACTIVITY_NAME, activityName)
                        bundle.putString(ARG_DEVICE_POSITION, devicePosition)
                        bundle.putString(ARG_DEVICE_ORIENTATION, deviceOrientation)
                        bundle.putInt(ARG_START_DELAY, startDelay)

                        RxBus.publish(ServiceArgumentsEvent(bundle))
                    }

                    else -> {
                    }
                }

            } else {
                Log.d("CustomLog", "No Command Received")
            }


        } ?: Log.d("CustomLog", "Returned from service death!")

        return START_STICKY
    }

    // Before onStartCommand
    override fun onCreate() {
        // Fires when a service is first initialized
        super.onCreate()
        Log.d("CustomLog", "onCreate")
        createAndStartNotification(this)

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorLogger::RecordingSensorValues").apply {
                acquire()
            }
        }

        initializerJob = async(BACKGROUND_THREAD_POOL_CONTEXT) {

            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorValueRepository = SensorValueRepository.getInstance(this@SensorLoggerService)
            sensorPreferenceRepository = SensorPreferenceRepository.getInstance(this@SensorLoggerService)
            sensorValueProcessor = PublishProcessor.create<SensorValueEntity>().toSerialized()
            activeSensorPreferences = sensorPreferenceRepository.getPreferences(onlyActive = true)
            activeSensorsWithPreferences = activeSensorPreferences
                    .filter {
                        sensorManager.getDefaultSensor(it.sensorType) != null
                    }
                    .mapNotNull { preference ->
                        val sensorList: List<Sensor> = sensorManager.getSensorList(preference.sensorType)
                        if (sensorList.isEmpty())
                            null
                        else {
                            val foundSensor = sensorList.firstOrNull { sensor ->
                                sensor.name.equals(preference.sensorName, true)
                                        && sensor.vendor.equals(preference.sensorVendorName, true)
                                        && sensor.version == preference.sensorVersion
                            }

                            if (foundSensor != null) {
                                SensorWithPreference(foundSensor, preference)
                            } else
                                null
                        }
                    }

            gson = Gson()
            floatListType = object : TypeToken<List<Float>>() {}.type

            ACCURACY_HIGH = getString(R.string.sensor_accuracy_high)
            ACCURACY_MEDIUM = getString(R.string.sensor_accuracy_medium)
            ACCURACY_LOW = getString(R.string.sensor_accuracy_low)
            ACCURACY_UNRELIABLE = getString(R.string.sensor_accuracy_unreliable)
            ACCURACY_UNKNOWN = getString(R.string.sensor_accuracy_unknown)

            Unit
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        // Cleanup service before destruction
        wakeLock.release()
        sensorEventListeners.forEach {
            sensorManager.unregisterListener(it)
        }.also {
            sensorEventListeners.clear()
        }
        initializerJob.cancel()
        loggerJob.cancel()
        sensorValueDisposable?.dispose()
        RxBus.publish(ServiceStopEvent)
        Log.d("CustomLog", "onDestroy")
        // todo test if called when low memory and restart
    }

}