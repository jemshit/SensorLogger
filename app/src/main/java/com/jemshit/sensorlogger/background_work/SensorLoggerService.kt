package com.jemshit.sensorlogger.background_work

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.*
import android.os.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceEntity
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceRepository
import com.jemshit.sensorlogger.data.sensor_value.SensorValueEntity
import com.jemshit.sensorlogger.helper.RxBus
import com.jemshit.sensorlogger.helper.createActivityFolder
import com.jemshit.sensorlogger.helper.createLogFile
import com.jemshit.sensorlogger.helper.random
import com.jemshit.sensorlogger.model.*
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.*
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


const val ARG_START_DELAY = "start_delay"
const val ARG_END_DELAY = "end_delay"
const val ARG_ACTIVITY_NAME = "activity_name"
const val ARG_DEVICE_POSITION = "device_position"
const val ARG_DEVICE_ORIENTATION = "device_orientation"

internal val BACKGROUND_THREAD_POOL_CONTEXT = newFixedThreadPoolContext(2, "backgroundThreads")
const val BUFFER_TIMESPAN_CONSTRAINT = 3L // seconds
const val BUFFER_COUNT_CONSTRAINT = 500

const val ACCURACY_HIGH_TEXT = "High"
const val ACCURACY_MEDIUM_TEXT = "Medium"
const val ACCURACY_LOW_TEXT = "Low"
const val ACCURACY_UNRELIABLE_TEXT = "Unreliable"
const val ACCURACY_UNKNOWN_TEXT = "Unknown"

const val ARG_EXCLUDED_ACCURACIES = "excluded_accuracies"

const val ARG_AGE = "age"
const val ARG_WEIGHT = "weight"
const val ARG_HEIGHT = "height"
const val ARG_GENDER = "gender"

const val GENDER_MALE = "male"
const val GENDER_FEMALE = "female"
const val EMPTY_ACTIVITY = "EMPTY"
const val EMPTY_POSITION = "EMPTY"
const val EMPTY_ORIENTATION = "EMPTY"


class SensorLoggerService : Service() {

    //region Properties
    private lateinit var sensorManager: SensorManager
    private val sensorEventListeners: MutableList<SensorEventListener> = mutableListOf()

    private lateinit var sensorPreferenceRepository: SensorPreferenceRepository
    private lateinit var activeSensorPreferences: List<SensorPreferenceEntity>
    private lateinit var activeSensorsWithPreferences: List<SensorWithPreference>
    private lateinit var sensorValueProcessor: FlowableProcessor<SensorValueEntity>
    private lateinit var compositeDisposables: CompositeDisposable

    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var initializerJob: Deferred<Unit>
    private lateinit var loggerJob: Job

    private var ignoreData: Boolean = false
    private lateinit var dateFormatter: SimpleDateFormat
    private lateinit var gson: Gson
    private lateinit var floatListType: Type
    private lateinit var stringListType: Type
    private var file: File? = null
    private var lastTimestamp: Long = -1L
    private var fileIsDirty: Boolean = false

    private var startDelay: Int = 0
    private var endDelay: Int = 0
    private var activityName: String = ""
    private var devicePosition: String = ""
    private var deviceOrientation: String = ""

    private var excludedAccuracies: Array<String> = arrayOf()
    private var age: String = ""
    private var weight: String = ""
    private var height: String = ""
    private var gender: String = ""
    private var userInfo: UserInfoModel? = null
    private var deviceInfo: DeviceInfoModel? = null

    //endregion

    // Fired every time startService is called
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { _ ->
            // Fill the local variables
            val bundle = intent.getBundleExtra(ARG_BUNDLE)
            bundle?.let { _ ->
                startDelay = bundle.getInt(ARG_START_DELAY, 0)
                endDelay = bundle.getInt(ARG_END_DELAY, 0)
                activityName = bundle.getString(ARG_ACTIVITY_NAME, EMPTY_ACTIVITY)
                devicePosition = bundle.getString(ARG_DEVICE_POSITION, EMPTY_POSITION)
                deviceOrientation = bundle.getString(ARG_DEVICE_ORIENTATION, EMPTY_ORIENTATION)

                excludedAccuracies = bundle.getStringArray(ARG_EXCLUDED_ACCURACIES) ?: arrayOf()
                age = bundle.getString(ARG_AGE) ?: ""
                weight = bundle.getString(ARG_WEIGHT) ?: ""
                height = bundle.getString(ARG_HEIGHT) ?: ""
                gender = bundle.getString(ARG_GENDER) ?: ""
                userInfo = UserInfoModel(age, weight, height, gender)

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
                            compositeDisposables.add(
                                    sensorValueProcessor
                                            .onBackpressureBuffer()
                                            .buffer(BUFFER_TIMESPAN_CONSTRAINT, TimeUnit.SECONDS, BUFFER_COUNT_CONSTRAINT)
                                            .onBackpressureBuffer()
                                            .subscribeOn(Schedulers.from(BACKGROUND_THREAD_POOL_CONTEXT.executor))
                                            .doOnSubscribe {
                                                // Start Event
                                                createFile()
                                                file?.let { file ->
                                                    try {
                                                        file.bufferedWriter().use { bufferedWriter ->
                                                            bufferedWriter.write("USER_INFO: $userInfo\n")
                                                            bufferedWriter.write("DEVICE_INFO: $deviceInfo\n")
                                                            bufferedWriter.write("ACTIVITY_NAME: $activityName\n")
                                                            bufferedWriter.write("DEVICE_POSITION: $devicePosition\n")
                                                            bufferedWriter.write("DEVICE_ORIENTATION: $deviceOrientation\n")
                                                        }
                                                    } catch (e: Exception) {
                                                    }
                                                } ?: stopMe()
                                            }
                                            // Buffer is emitted when onComplete is called. Then doOnCancel is executed
                                            .subscribe(
                                                    // onNext
                                                    { values ->
                                                        if (values.isNotEmpty()) {
                                                            // After onComplete event from onDestroy(), code runs on main thread
                                                            val isUiThread = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                                                                Looper.getMainLooper().isCurrentThread
                                                            else
                                                                Thread.currentThread() === Looper.getMainLooper().thread

                                                            if (isUiThread)
                                                                launch(BACKGROUND_THREAD_POOL_CONTEXT) {
                                                                    saveToFile(values)
                                                                }
                                                            else {
                                                                saveToFile(values)
                                                            }
                                                        }
                                                    },
                                                    // onError
                                                    {
                                                        stopSelf()
                                                    },
                                                    // onComplete
                                                    {
                                                        launch(BACKGROUND_THREAD_POOL_CONTEXT) {
                                                            if (ignoreData) {
                                                                file?.let { file -> if (file.exists()) file.delete() }
                                                            } else {
                                                                // todo delete delay if has
                                                                /*if (endDelay > 0) {
                                                                    if (fileIsDirty) {
                                                                        val delayInMs = endDelay * 1000
                                                                        val lastAllowedEpoch = lastTimestamp - delayInMs

                                                                        file?.let { file ->

                                                                            tempValues = tempValues.dropLastWhile {
                                                                                it.timestamp > lastAllowedEpoch
                                                                            }.toMutableList()

                                                                            long OldLength = rand.length();
                                                                            long NewLength = OldLength - 20;
                                                                            rand.setLength(NewLength);
                                                                            rand.seek(rand.length());
                                                                        }
                                                                    }
                                                                }*/
                                                            }
                                                        }
                                                    }
                                            )
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
                                                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> ACCURACY_HIGH_TEXT
                                                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> ACCURACY_MEDIUM_TEXT
                                                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> ACCURACY_LOW_TEXT
                                                SensorManager.SENSOR_STATUS_UNRELIABLE -> ACCURACY_UNRELIABLE_TEXT
                                                else -> ACCURACY_UNKNOWN_TEXT
                                            }
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

                    ServiceCommand.STOP.id -> stopMe()

                    ServiceCommand.STOP_AND_IGNORE.id -> stopMe(true)

                    else -> {
                        //
                    }
                }

            } else {
                stopSelf()
                // no intent data means WTF error
            }

        } ?: stopSelf()
        // no intent data means WTF error (we must always get intent even after process death)

        return START_REDELIVER_INTENT
    }

    // Fires when a service is first initialized, before onStartCommand
    override fun onCreate() {
        super.onCreate()
        createAndStartNotification(this)

        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorLogger::RecordingSensorValues").apply {
                acquire()
            }
        }

        //
        compositeDisposables = CompositeDisposable()
        compositeDisposables.add(
                RxBus.listen(ServicePublishArgumentsEvent::class.java, Schedulers.io())
                        .subscribe {
                            val bundle = Bundle()
                            bundle.putString(ARG_ACTIVITY_NAME, activityName)
                            bundle.putString(ARG_DEVICE_POSITION, devicePosition)
                            bundle.putString(ARG_DEVICE_ORIENTATION, deviceOrientation)
                            bundle.putInt(ARG_START_DELAY, startDelay)
                            bundle.putInt(ARG_END_DELAY, endDelay)

                            bundle.putString(ARG_AGE, age)
                            bundle.putString(ARG_WEIGHT, weight)
                            bundle.putString(ARG_HEIGHT, height)
                            bundle.putString(ARG_GENDER, gender)
                            bundle.putStringArray(ARG_EXCLUDED_ACCURACIES, excludedAccuracies)

                            RxBus.publish(ServiceArgumentsEvent(bundle))
                        }
        )

        //
        initializerJob = async(BACKGROUND_THREAD_POOL_CONTEXT) {

            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
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
            stringListType = object : TypeToken<List<String>>() {}.type
            deviceInfo = DeviceInfoModel()
            dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZ", Locale.US)

            Unit
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Cleanup service before destruction
    override fun onDestroy() {
        // ProTip: if process is killed by system, wakelock is released automatically
        wakeLock.release()
        sensorEventListeners.forEach {
            sensorManager.unregisterListener(it)
        }.also {
            sensorEventListeners.clear()
        }
        sensorValueProcessor.onComplete()   // Forces buffer to emit whatever he left
        compositeDisposables.clear()
        initializerJob.cancel()
        loggerJob.cancel()
        RxBus.publish(ServiceStopEvent)
    }


    private fun stopMe(ignoreTheData: Boolean = false) {
        ignoreData = ignoreTheData

        stopForeground(true)
        stopSelf()
    }

    private fun createFile() {
        val folderErrorPair = createActivityFolder(applicationContext, activityName, devicePosition, deviceOrientation)
        if (folderErrorPair.first == null)
            stopMe(true)
        else {
            val fileName = createFilename()
            val fileErrorPair = createLogFile(applicationContext, folderErrorPair.first!!, fileName)
            if (fileErrorPair.first == null)
                stopMe(true)
            else {
                file = fileErrorPair.first!!
            }
        }
    }

    private fun saveToFile(values: List<SensorValueEntity>) {
        file?.let { file ->
            try {
                BufferedWriter(FileWriter(file, true)).use { bufferedWriter ->
                    values.forEachIndexed { index, item ->
                        if (item.valueAccuracy !in excludedAccuracies) {
                            bufferedWriter.write(getLine(item))
                            bufferedWriter.write("\n")
                            lastTimestamp = item.timestamp
                        }
                    }
                    fileIsDirty = true
                }
            } catch (e: Exception) {
                stopMe()
            }
        } ?: stopMe()
    }

    private fun createFilename(): String {
        return dateFormatter.format(Date(System.currentTimeMillis())) + "-" + (0..1000).random().toString() + ".log"
    }

    private fun getLine(item: SensorValueEntity): String {
        if (item.values.isBlank())
            return ""

        val sensorName = getSensorTinyNameFromType(item.sensorType)
        val valuesDecoded = gson.fromJson<List<Float>>(item.values, floatListType)
        var valuesAsString = ""
        valuesDecoded.forEach {
            valuesAsString += it.toString()
            valuesAsString += " "
        }
        valuesAsString.removeSuffix(" ")

        return "${item.timestamp} ${item.phoneUptime} $sensorName: $valuesAsString ${item.valueAccuracy}"
    }
}