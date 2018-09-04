package com.jemshit.sensorlogger.ui.recording_info

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.background_work.*
import com.jemshit.sensorlogger.helper.RxBus
import com.jemshit.sensorlogger.helper.startAppropriateForegroundService
import com.jemshit.sensorlogger.model.ServiceArgumentsEvent
import com.jemshit.sensorlogger.model.ServiceStopEvent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.recording_info_fragment.*

class RecordingInfoFragment : Fragment() {

    private lateinit var compositeDisposable: CompositeDisposable

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

        // EventBus
        compositeDisposable.add(
                RxBus.listen(ServiceStopEvent::class.java, AndroidSchedulers.mainThread())
                        .subscribe {
                            enableInput()
                            button_start_recording.text = context!!.getString(R.string.start_recording)
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
                        }
        )

        if (isServiceRunningInForeground(context!!, SensorLoggerService::class.java)) {
            val serviceIntent = createServiceIntent(context!!,
                    SensorLoggerService::class.java,
                    null,
                    ServiceCommand.PUBLISH_ARGUMENTS
            )
            serviceIntent.putExtras(Intent())   // Make intent of onStartCommand() non null so only process death has null intent
            context?.startAppropriateForegroundService(serviceIntent)

            enableInput(false)
            button_start_recording.text = context!!.getString(R.string.stop_recording)
        } else {
            enableInput()
            button_start_recording.text = context!!.getString(R.string.start_recording)
        }

        button_start_recording.setOnClickListener {
            if (isServiceRunningInForeground(context!!, SensorLoggerService::class.java)) {
                val serviceIntent = createServiceIntent(context!!,
                        SensorLoggerService::class.java,
                        null,
                        ServiceCommand.STOP
                )
                serviceIntent.putExtras(Intent())   // Make intent of onStartCommand() non null so only process death has null intent
                context?.startAppropriateForegroundService(serviceIntent)

                enableInput()
                button_start_recording.text = context!!.getString(R.string.start_recording)

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

                val bundle = Bundle()
                if (activityName.isNotBlank()) bundle.putString(ARG_ACTIVITY_NAME, activityName)
                if (devicePosition.isNotBlank()) bundle.putString(ARG_DEVICE_POSITION, devicePosition)
                if (deviceOrientation.isNotBlank()) bundle.putString(ARG_DEVICE_ORIENTATION, deviceOrientation)
                if (startDelay > 0) bundle.putInt(ARG_START_DELAY, startDelay)

                val serviceIntent = createServiceIntent(context!!,
                        SensorLoggerService::class.java,
                        bundle,
                        ServiceCommand.START
                )
                context?.startAppropriateForegroundService(serviceIntent)

                button_start_recording.text = context!!.getString(R.string.stop_recording)
            }
        }
    }

    private fun enableInput(enable: Boolean = true) {
        input_activity_name.isEnabled = enable
        input_device_position.isEnabled = enable
        input_device_orientation.isEnabled = enable
        input_start_delay.isEnabled = enable
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
    }
}
