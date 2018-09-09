package com.jemshit.sensorlogger.ui.sensor_detail

import android.hardware.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.jakewharton.rxbinding2.widget.RxTextView
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceEntity
import com.jemshit.sensorlogger.helper.toPx
import com.jemshit.sensorlogger.model.*
import com.jemshit.sensorlogger.ui.main.SensorsViewModel
import com.jemshit.sensorlogger.ui.sensor_detail.widgets.SensorInfoWidget
import com.jemshit.sensorlogger.ui.sensor_detail.widgets.SensorValueWidget
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.sensor_detail_fragment.*
import java.util.concurrent.TimeUnit

class SensorDetailFragment : Fragment() {

    private lateinit var sensorsViewModel: SensorsViewModel

    private val sensorId by lazy { SensorDetailFragmentArgs.fromBundle(arguments).sensorId }
    private var sensorWithPreference: SensorWithPreference? = null
    private lateinit var sensorValueInfo: SensorValueInfo

    private var sensorValueWidget: SensorValueWidget? = null
    private var sensorInfoWidget: SensorInfoWidget? = null
    private val cardViewLayoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
        it.setMargins(8.toPx)
    }

    private var sensorEventListener: SensorEventListener? = null
    private lateinit var compositeDisposable: CompositeDisposable


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        compositeDisposable = CompositeDisposable()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.sensor_detail_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        sensorsViewModel = ViewModelProviders.of(activity!!)
                .get(SensorsViewModel::class.java)

        sensorWithPreference = sensorsViewModel.getSensorWithPreference(sensorId)
        sensorWithPreference?.let { sensorWithPreference ->
            val sensor = sensorWithPreference.sensor
            sensorValueInfo = SensorValueInfo(sensor.getValueCount(), sensor.getValueUnit(), sensor.getCategory())

            loadPreferences(sensor, sensorWithPreference.preference)
            addRealTimeInfoWidget()
            fillSensorInfo(sensor)
            listenToSensorData(sensor)
        } ?: showError()

        image_back.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadPreferences(sensor: Sensor, preference: SensorPreferenceEntity?) {
        // Enable/disable
        switch_sensor_check.isChecked = preference?.isChecked ?: false

        // Sampling Period
        if (preference?.samplingPeriodCustom != null && preference.samplingPeriodCustom != DEFAULT_SAMPLING_PERIOD_CUSTOM) {
            radio_button_custom.isChecked = true

            input_layout_sampling_custom.visibility = View.VISIBLE
            input_sampling_custom.setText(preference.samplingPeriodCustom.toString())

        } else {
            input_layout_sampling_custom.visibility = View.GONE
            input_sampling_custom.setText("")

            when (preference?.samplingPeriod ?: DEFAULT_SAMPLING_PERIOD) {
                SensorManager.SENSOR_DELAY_FASTEST -> radio_button_fastest.isChecked = true
                SensorManager.SENSOR_DELAY_GAME -> radio_button_game.isChecked = true
                SensorManager.SENSOR_DELAY_UI -> radio_button_ui.isChecked = true
                SensorManager.SENSOR_DELAY_NORMAL -> radio_button_normal.isChecked = true
                else -> radio_button_normal.isChecked = true
            }
        }
        radio_button_fastest.text = "${context?.getString(R.string.sensor_sampling_fastest)} (${sensor.maxFrequency}Hz)"
        val cappedMinFreq = if (sensor.minFrequency > 3f) sensor.minFrequency else 3f
        radio_button_custom.text = "${context?.getString(R.string.sensor_sampling_custom)} ($cappedMinFreq-${sensor.maxFrequency}Hz)"

        // Change Listener
        radio_group_sampling.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.radio_button_fastest -> {
                    input_layout_sampling_custom.visibility = View.GONE
                    updateSensorPreference(samplingPeriod = SensorManager.SENSOR_DELAY_FASTEST, samplingPeriodCustom = DEFAULT_SAMPLING_PERIOD_CUSTOM)
                }
                R.id.radio_button_game -> {
                    input_layout_sampling_custom.visibility = View.GONE
                    updateSensorPreference(samplingPeriod = SensorManager.SENSOR_DELAY_GAME, samplingPeriodCustom = DEFAULT_SAMPLING_PERIOD_CUSTOM)
                }
                R.id.radio_button_ui -> {
                    input_layout_sampling_custom.visibility = View.GONE
                    updateSensorPreference(samplingPeriod = SensorManager.SENSOR_DELAY_UI, samplingPeriodCustom = DEFAULT_SAMPLING_PERIOD_CUSTOM)
                }
                R.id.radio_button_normal -> {
                    input_layout_sampling_custom.visibility = View.GONE
                    updateSensorPreference(samplingPeriod = SensorManager.SENSOR_DELAY_NORMAL, samplingPeriodCustom = DEFAULT_SAMPLING_PERIOD_CUSTOM)
                }
                R.id.radio_button_custom -> {
                    input_sampling_custom.setText("")
                    input_layout_sampling_custom.visibility = View.VISIBLE
                    try {
                        val customSampling = input_sampling_custom.text.toString().toInt()
                        updateSensorPreference(samplingPeriodCustom = customSampling)
                    } catch (e: Exception) {
                        showError("Custom Sampling Period is Not Valid!")
                    }
                }
            }
        }

        switch_sensor_check.setOnCheckedChangeListener { _, checked ->
            updateSensorPreference(checked)
        }

        compositeDisposable.add(
                RxTextView.textChanges(input_sampling_custom)
                        .debounce(750, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .skip(1)        // use for input but not for click events
                        .subscribe {
                            if (it.isNotBlank()) {
                                try {
                                    val customSampling = it.toString().toInt()
                                    updateSensorPreference(samplingPeriodCustom = customSampling)
                                } catch (e: Exception) {
                                    showError("Custom Sampling Period is Not Valid!")
                                }
                            }
                        }
        )
    }

    private fun addRealTimeInfoWidget() {
        if (sensorValueInfo.valueCount in 1..6) {
            sensorValueWidget = SensorValueWidget(context!!, sensorValueInfo.valueCount).also { widget ->
                widget.layoutParams = cardViewLayoutParams
                layout_root_linear.addView(widget, 1)
            }
        }
    }

    private fun fillSensorInfo(sensor: Sensor) {
        text_toolbar_title.text = sensor.name

        sensorInfoWidget = SensorInfoWidget(context!!).also { widget ->
            widget.layoutParams = cardViewLayoutParams
            layout_root_linear.addView(widget)

            widget.updateInfo(sensor, sensorValueInfo)
        }
    }

    private fun listenToSensorData(sensor: Sensor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sensorEventListener = object : SensorEventCallback() {
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                override fun onFlushCompleted(sensor: Sensor?) {}

                override fun onSensorChanged(sensorEvent: SensorEvent?) {
                    sensorValueChanged(sensorEvent)
                }

                override fun onSensorAdditionalInfo(info: SensorAdditionalInfo?) {
                    super.onSensorAdditionalInfo(info)
                    // todo
                    Log.d("CustomLog", info.toString())
                }
            }
        } else {
            sensorEventListener = object : SensorEventListener2 {
                override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {}
                override fun onFlushCompleted(sensor: Sensor?) {}

                override fun onSensorChanged(sensorEvent: SensorEvent?) {
                    sensorValueChanged(sensorEvent)
                }
            }
        }

        sensorsViewModel.sensorManager.registerListener(
                sensorEventListener,
                sensor,
                sensorWithPreference?.preference?.samplingPeriod ?: DEFAULT_SAMPLING_PERIOD,
                0
        )
    }

    private fun sensorValueChanged(sensorEvent: SensorEvent?) {
        sensorEvent?.let {
            when (sensorValueInfo.valueCount) {
                1 -> sensorValueWidget?.updateValue(1, it.accuracy,
                        x = it.values[0], x_unit = sensorValueInfo.valueUnit)

                2 -> sensorValueWidget?.updateValue(2, it.accuracy,
                        x = it.values[0],
                        y = it.values[1],
                        x_unit = sensorValueInfo.valueUnit,
                        y_unit = sensorValueInfo.valueUnit)

                3 -> sensorValueWidget?.updateValue(3, it.accuracy,
                        x = it.values[0],
                        y = it.values[1], z = it.values[2],
                        x_unit = sensorValueInfo.valueUnit,
                        y_unit = sensorValueInfo.valueUnit,
                        z_unit = sensorValueInfo.valueUnit)

                4 -> {
                    if (it.values.size == 4)
                        sensorValueWidget?.updateValue(4, it.accuracy,
                                x = it.values[0],
                                y = it.values[1], z = it.values[2], x2 = it.values[3],
                                x_unit = sensorValueInfo.valueUnit,
                                y_unit = sensorValueInfo.valueUnit,
                                z_unit = sensorValueInfo.valueUnit,
                                x2_unit = sensorValueInfo.valueUnit)
                    else
                        sensorValueWidget?.updateValue(4, it.accuracy,
                                x = it.values[0],
                                y = it.values[1], z = it.values[2], x2 = 0f,
                                x_unit = sensorValueInfo.valueUnit,
                                y_unit = sensorValueInfo.valueUnit,
                                z_unit = sensorValueInfo.valueUnit,
                                x2_unit = sensorValueInfo.valueUnit)
                }

                5 -> sensorValueWidget?.updateValue(5, it.accuracy,
                        x = it.values[0],
                        y = it.values[1], z = it.values[2], x2 = it.values[3],
                        y2 = it.values[4],
                        x_unit = sensorValueInfo.valueUnit,
                        y_unit = sensorValueInfo.valueUnit,
                        z_unit = sensorValueInfo.valueUnit,
                        x2_unit = sensorValueInfo.valueUnit,
                        y2_unit = sensorValueInfo.valueUnit)

                6 -> sensorValueWidget?.updateValue(6, it.accuracy,
                        x = it.values[0],
                        y = it.values[1], z = it.values[2], x2 = it.values[3],
                        y2 = it.values[4], z2 = it.values[5],
                        x_unit = sensorValueInfo.valueUnit,
                        y_unit = sensorValueInfo.valueUnit,
                        z_unit = sensorValueInfo.valueUnit,
                        x2_unit = sensorValueInfo.valueUnit,
                        y2_unit = sensorValueInfo.valueUnit,
                        z2_unit = sensorValueInfo.valueUnit)

                else -> {
                }
            }
        }
    }


    private fun updateSensorPreference(checked: Boolean = switch_sensor_check.isChecked,
                                       samplingPeriod: Int = sensorWithPreference?.preference?.samplingPeriod
                                               ?: DEFAULT_SAMPLING_PERIOD,
                                       samplingPeriodCustom: Int = sensorWithPreference?.preference?.samplingPeriodCustom
                                               ?: DEFAULT_SAMPLING_PERIOD_CUSTOM) {

        val isPreferenceExists = sensorWithPreference?.preference != null

        // Update local
        val entity = SensorPreferenceEntity(
                sensorWithPreference!!.sensor.uniqueId,
                sensorWithPreference!!.sensor.name,
                sensorWithPreference!!.sensor.type,
                sensorWithPreference!!.sensor.vendor,
                sensorWithPreference!!.sensor.version,
                checked,
                samplingPeriod,
                samplingPeriodCustom
        )
        sensorWithPreference?.preference = entity

        // Persist
        if (isPreferenceExists)
            sensorsViewModel.updateSensorPreference(entity)
        else
            sensorsViewModel.saveSensorPreference(entity)
    }


    private fun showError(message: String = getString(R.string.error)) {
        context?.apply {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
        sensorEventListener?.let {
            sensorsViewModel.sensorManager.unregisterListener(it)
        }
    }
}
