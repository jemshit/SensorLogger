package com.jemshit.sensorlogger.ui.sensor_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceEntity
import com.jemshit.sensorlogger.model.DEFAULT_SAMPLING_PERIOD
import com.jemshit.sensorlogger.model.DEFAULT_SAMPLING_PERIOD_CUSTOM
import com.jemshit.sensorlogger.model.uniqueId
import com.jemshit.sensorlogger.ui.main.SensorsViewModel
import kotlinx.android.synthetic.main.sensor_list_fragment.*

class SensorListFragment : Fragment() {

    private lateinit var sensorsViewModel: SensorsViewModel
    private lateinit var sensorListAdapter: SensorListAdapter
    private val navigationController by lazy { findNavController(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.sensor_list_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sensorsViewModel = ViewModelProviders.of(activity!!)
                .get(SensorsViewModel::class.java)
        sensorsViewModel.retrieveSensorList()

        sensorListAdapter = SensorListAdapter(
                { sensor ->
                    val directions = SensorListFragmentDirections.startSensorDetail().setSensorId(sensor.uniqueId)
                    navigationController.navigate(directions)
                },
                { sensorWithPreference, checked ->
                    val isPreferenceExists = sensorWithPreference.preference != null

                    val entity = SensorPreferenceEntity(
                            sensorWithPreference.sensor.uniqueId,
                            sensorWithPreference.sensor.name,
                            sensorWithPreference.sensor.type,
                            sensorWithPreference.sensor.vendor,
                            sensorWithPreference.sensor.version,
                            checked,
                            sensorWithPreference.preference?.samplingPeriod
                                    ?: DEFAULT_SAMPLING_PERIOD,
                            sensorWithPreference.preference?.samplingPeriodCustom
                                    ?: DEFAULT_SAMPLING_PERIOD_CUSTOM
                    )
                    if (isPreferenceExists)
                        sensorsViewModel.updateSensorPreference(entity)
                    else
                        sensorsViewModel.saveSensorPreference(entity)

                })
        recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = sensorListAdapter
        }

        sensorsViewModel.sensorsWithPreferences
                .observe(this, Observer { sensors ->
                    toolbar.title = getString(R.string.sensor_list, sensors.size.toString())
                    sensorListAdapter.items = sensors
                })

        fab.setOnClickListener {
            val directions = SensorListFragmentDirections.startRecordingInfoFragment()
            navigationController.navigate(directions)
        }
    }

}
