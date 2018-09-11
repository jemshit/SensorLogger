package com.jemshit.sensorlogger.ui.recording_info

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.jakewharton.rxbinding2.view.RxView
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.background_work.*
import com.jemshit.sensorlogger.helper.RxBus
import com.jemshit.sensorlogger.helper.startAppropriateForegroundService
import com.jemshit.sensorlogger.model.ServiceArgumentsEvent
import com.jemshit.sensorlogger.model.ServicePublishArgumentsEvent
import com.jemshit.sensorlogger.model.ServiceStopEvent
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.recording_info_fragment.*

class RecordingInfoFragment : Fragment() {

    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var rxPermissions: RxPermissions

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        compositeDisposable = CompositeDisposable()
        return inflater.inflate(R.layout.recording_info_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        image_close.setOnClickListener {
            findNavController().navigateUp()
        }
        rxPermissions = RxPermissions(this)

        // EventBus
        compositeDisposable.add(
                RxBus.listen(ServiceStopEvent::class.java, AndroidSchedulers.mainThread())
                        .subscribe {
                            enableInput()
                            button_start_recording.text = context!!.getString(R.string.start_recording)
                            button_stop_ignore_recording.visibility = View.GONE
                        }
        )
        compositeDisposable.add(
                RxBus.listen(ServiceArgumentsEvent::class.java, AndroidSchedulers.mainThread())
                        .subscribe {
                            val bundle = it.bundle
                            input_activity_name.setText(bundle.getString(ARG_ACTIVITY_NAME, ""))
                            input_device_position.setText(bundle.getString(ARG_DEVICE_POSITION, ""))
                            input_device_orientation.setText(bundle.getString(ARG_DEVICE_ORIENTATION, ""))
                            val usedStartDelay = bundle.getInt(ARG_START_DELAY, 0)
                            if (usedStartDelay > 0) input_start_delay.setText(usedStartDelay.toString())
                            val usedEndDelay = bundle.getInt(ARG_END_DELAY, 0)
                            if (usedEndDelay > 0) input_end_delay.setText(usedEndDelay.toString())
                            input_age.setText(bundle.getString(ARG_AGE, ""))
                            input_weight.setText(bundle.getString(ARG_WEIGHT, ""))
                            input_height.setText(bundle.getString(ARG_HEIGHT, ""))
                            val usedGender = bundle.getString(ARG_GENDER, "")
                            when (usedGender) {
                                GENDER_MALE -> radio_male.isChecked = true
                                GENDER_FEMALE -> radio_female.isChecked = true
                                else -> {
                                    radio_male.isChecked = false
                                    radio_female.isChecked = false
                                }
                            }
                            val usedExcludedActivities = bundle.getStringArray(ARG_EXCLUDED_ACCURACIES)
                            if (usedExcludedActivities == null) {
                                checkbox_high.isChecked = false
                                checkbox_medium.isChecked = false
                                checkbox_low.isChecked = false
                                checkbox_unreliable.isChecked = false
                                checkbox_unknown.isChecked = false
                            } else {
                                usedExcludedActivities.forEach {
                                    when (it) {
                                        ACCURACY_HIGH_TEXT -> checkbox_high.isChecked = true
                                        ACCURACY_MEDIUM_TEXT -> checkbox_medium.isChecked = true
                                        ACCURACY_LOW_TEXT -> checkbox_low.isChecked = true
                                        ACCURACY_UNRELIABLE_TEXT -> checkbox_unreliable.isChecked = true
                                        ACCURACY_UNKNOWN_TEXT -> checkbox_unknown.isChecked = true
                                    }
                                }
                            }
                        }
        )

        if (isServiceRunningInForeground(context!!, SensorLoggerService::class.java)) {
            RxBus.publish(ServicePublishArgumentsEvent)

            enableInput(false)
            button_start_recording.text = context!!.getString(R.string.stop_recording)
            button_stop_ignore_recording.visibility = View.VISIBLE
        } else {
            enableInput()
            button_start_recording.text = context!!.getString(R.string.start_recording)
            button_stop_ignore_recording.visibility = View.GONE
        }

        button_stop_ignore_recording.setOnClickListener {
            if (isServiceRunningInForeground(context!!, SensorLoggerService::class.java)) {
                val serviceIntent = createServiceIntent(context!!,
                        SensorLoggerService::class.java,
                        null,
                        ServiceCommand.STOP_AND_IGNORE
                )
                context?.startAppropriateForegroundService(serviceIntent)

                enableInput()
                button_start_recording.text = context!!.getString(R.string.start_recording)
                button_stop_ignore_recording.visibility = View.GONE
            } else
                button_stop_ignore_recording.visibility = View.GONE
        }

        compositeDisposable.add(
                RxView.clicks(button_start_recording)
                        .compose(rxPermissions.ensure(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(onNext = { granted ->
                            if (granted) {
                                if (isServiceRunningInForeground(context!!, SensorLoggerService::class.java)) {
                                    val serviceIntent = createServiceIntent(context!!,
                                            SensorLoggerService::class.java,
                                            null,
                                            ServiceCommand.STOP
                                    )
                                    context?.startAppropriateForegroundService(serviceIntent)

                                    enableInput()
                                    button_start_recording.text = context!!.getString(R.string.start_recording)
                                    button_stop_ignore_recording.visibility = View.GONE

                                } else {
                                    enableInput(false)

                                    val activityName = input_activity_name.text.toString()
                                    val devicePosition = input_device_position.text.toString()
                                    val deviceOrientation = input_device_orientation.text.toString()
                                    var startDelay = 0
                                    try {
                                        startDelay = input_start_delay.text.toString().toInt()
                                    } catch (e: Exception) {
                                    }
                                    var endDelay = 0
                                    try {
                                        endDelay = input_end_delay.text.toString().toInt()
                                    } catch (e: Exception) {
                                    }

                                    val bundle = Bundle()
                                    if (activityName.isNotBlank()) bundle.putString(ARG_ACTIVITY_NAME, activityName)
                                    if (devicePosition.isNotBlank()) bundle.putString(ARG_DEVICE_POSITION, devicePosition)
                                    if (deviceOrientation.isNotBlank()) bundle.putString(ARG_DEVICE_ORIENTATION, deviceOrientation)
                                    if (startDelay > 0) bundle.putInt(ARG_START_DELAY, startDelay)
                                    if (endDelay > 0) bundle.putInt(ARG_END_DELAY, endDelay)

                                    val excludedAccuracies = mutableListOf<String>()
                                    if (checkbox_high.isChecked) excludedAccuracies.add(ACCURACY_HIGH_TEXT)
                                    if (checkbox_medium.isChecked) excludedAccuracies.add(ACCURACY_MEDIUM_TEXT)
                                    if (checkbox_low.isChecked) excludedAccuracies.add(ACCURACY_LOW_TEXT)
                                    if (checkbox_unreliable.isChecked) excludedAccuracies.add(ACCURACY_UNRELIABLE_TEXT)
                                    if (checkbox_unknown.isChecked) excludedAccuracies.add(ACCURACY_UNKNOWN_TEXT)

                                    val gender = if (radio_group_gender.checkedRadioButtonId == R.id.radio_male)
                                        GENDER_MALE
                                    else if (radio_group_gender.checkedRadioButtonId == R.id.radio_female)
                                        GENDER_FEMALE
                                    else
                                        ""

                                    bundle.putStringArray(ARG_EXCLUDED_ACCURACIES, excludedAccuracies.toTypedArray())
                                    bundle.putString(ARG_GENDER, gender)
                                    bundle.putString(ARG_AGE, input_age.text.toString())
                                    bundle.putString(ARG_WEIGHT, input_weight.text.toString())
                                    bundle.putString(ARG_HEIGHT, input_height.text.toString())


                                    val serviceIntent = createServiceIntent(context!!,
                                            SensorLoggerService::class.java,
                                            bundle,
                                            ServiceCommand.START
                                    )
                                    context?.startAppropriateForegroundService(serviceIntent)

                                    button_start_recording.text = context!!.getString(R.string.stop_recording)
                                    button_stop_ignore_recording.visibility = View.VISIBLE
                                }
                            } else {
                                Toast.makeText(context, getString(R.string.storage_permission_is_necessary), Toast.LENGTH_SHORT).show()
                            }
                        }, onError = {
                            Toast.makeText(context, it.message
                                    ?: getString(R.string.error), Toast.LENGTH_SHORT).show()
                        })
        )
    }

    private fun enableInput(enable: Boolean = true) {
        input_activity_name.isEnabled = enable
        input_device_position.isEnabled = enable
        input_device_orientation.isEnabled = enable
        input_start_delay.isEnabled = enable
        input_end_delay.isEnabled = enable

        input_age.isEnabled = enable
        input_weight.isEnabled = enable
        input_height.isEnabled = enable
        radio_male.isEnabled = enable
        radio_female.isEnabled = enable
        checkbox_high.isEnabled = enable
        checkbox_medium.isEnabled = enable
        checkbox_low.isEnabled = enable
        checkbox_unreliable.isEnabled = enable
        checkbox_unknown.isEnabled = enable
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
    }
}
