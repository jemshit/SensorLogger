package com.jemshit.sensorlogger.background_work

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.*
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceEntity
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceRepository
import com.jemshit.sensorlogger.data.sensor_value.SensorValueEntity
import com.jemshit.sensorlogger.data.sensor_value.SensorValueRepository
import com.jemshit.sensorlogger.helper.RxBus
import com.jemshit.sensorlogger.model.DEFAULT_SAMPLING_PERIOD
import com.jemshit.sensorlogger.model.SensorWithPreference
import com.jemshit.sensorlogger.model.ServiceArgumentsEvent
import com.jemshit.sensorlogger.model.ServiceStopEvent
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import kotlin.concurrent.thread

const val ARG_START_DELAY = "start_delay"
const val ARG_ACTIVITY_NAME = "activity_name"
const val ARG_DEVICE_POSITION = "device_position"
const val ARG_DEVICE_ORIENTATION = "device_orientation"

class SensorLoggerService : Service() {

    private lateinit var sensorManager: SensorManager
    private val sensorEventListeners: MutableList<SensorEventListener> = mutableListOf()

    private lateinit var sensorValueRepository: SensorValueRepository
    private lateinit var sensorPreferenceRepository: SensorPreferenceRepository
    private lateinit var activeSensorPreferences: List<SensorPreferenceEntity>
    private lateinit var activeSensorsWithPreferences: List<SensorWithPreference>
    private lateinit var sensorValueProcessor: FlowableProcessor<SensorValueEntity>

    private lateinit var wakeLock: PowerManager.WakeLock

    private var startDelay: Int = 0
    private var activityName: String = ""
    private var devicePosition: String = ""
    private var deviceOrientation: String = ""

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CustomLog", "onStartCommand")
        intent?.let { _ ->

            if (intent.getIntExtra(ARG_COMMAND, -1) != -1) {
                when (intent.getIntExtra(ARG_COMMAND, -1)) {
                    ServiceCommand.START.id -> {
                        Log.d("CustomLog", "Start Command Received")
                    }
                    ServiceCommand.STOP.id -> {
                        Log.d("CustomLog", "Stop Command Received")
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
                }
            } else {
                Log.d("CustomLog", "No Command Received")
            }

            val bundle = intent.getBundleExtra(ARG_BUNDLE)
            bundle?.let { _ ->
                startDelay = bundle.getInt(ARG_START_DELAY, 0)
                activityName = bundle.getString(ARG_ACTIVITY_NAME, "")
                devicePosition = bundle.getString(ARG_DEVICE_POSITION, "")
                deviceOrientation = bundle.getString(ARG_DEVICE_ORIENTATION, "")

                Log.d("CustomLog", "Bundle: startDelay:$startDelay activityName:$activityName devicePosition:$devicePosition deviceOrientation:$deviceOrientation")

            } ?: Log.d("CustomLog", "Null bundle argument")

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
                Log.d("CustomLog", "WakeLock acquire")
                acquire()
            }
        }
        thread(start = true) {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorValueRepository = SensorValueRepository.getInstance(this)
            sensorPreferenceRepository = SensorPreferenceRepository.getInstance(this)
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

            // Listen sensor and write
            activeSensorsWithPreferences.forEach { sensorWithPreference ->
                val sensorEventListener = object : SensorEventListener2 {
                    override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {}
                    override fun onFlushCompleted(sensor: Sensor?) {}

                    override fun onSensorChanged(sensorEvent: SensorEvent?) {
                        // todo send values through sensorValueProcessor
                        Log.d("xxx", "${sensorEvent!!.values[0]}")
                    }
                }
                sensorEventListeners.add(sensorEventListener)

                sensorManager.registerListener(
                        sensorEventListener,
                        sensorWithPreference.sensor,
                        sensorWithPreference.preference?.samplingPeriod
                                ?: DEFAULT_SAMPLING_PERIOD, // todo
                        0
                )

                // todo listen to sensorValueProcessor with buffer and timeout. then save to repository

                //SystemClock.uptimeMillis()
                //System.currentTimeMillis()
                //System.nanoTime()
            }
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
        }
        RxBus.publish(ServiceStopEvent)
        Log.d("CustomLog", "onDestroy")
        // todo test if called when low memory and restart
    }

}