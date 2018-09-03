package com.jemshit.sensorlogger.ui.sensor_list

import android.hardware.Sensor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.helper.Diffable
import com.jemshit.sensorlogger.helper.calculateDiff
import com.jemshit.sensorlogger.model.SensorWithPreference
import kotlinx.android.synthetic.main.sensor_item.view.*
import kotlin.properties.Delegates

class SensorListAdapter(private val clickListener: (Sensor) -> Unit,
                        private val checkListener: (SensorWithPreference, Boolean) -> Unit)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items: List<Diffable> by Delegates.observable(listOf()) { _, old, new ->
        calculateDiff(old, new).dispatchUpdatesTo(this)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val textView = LayoutInflater.from(parent.context)
                .inflate(R.layout.sensor_item, parent, false)
        return MyViewHolder(textView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MyViewHolder) {
            if (items[position] is SensorWithPreference) {
                holder.bind(items[position] as SensorWithPreference, clickListener, checkListener)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = items.size

    class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(model: SensorWithPreference,
                 clickListener: (Sensor) -> Unit,
                 checkListener: (SensorWithPreference, Boolean) -> Unit) {

            view.text_name.text = model.sensor.name
            view.checkbox.setOnCheckedChangeListener(null)  // Important (wtf)
            view.checkbox.isChecked = model.preference?.isChecked ?: false

            view.text_name.setOnClickListener { clickListener(model.sensor) }
            view.checkbox.setOnCheckedChangeListener { _, checked ->
                checkListener(model, checked)
            }
        }
    }
}
