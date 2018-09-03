package com.jemshit.sensorlogger.ui.sensor_detail.widgets

import android.content.Context
import android.hardware.SensorManager.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.jemshit.sensorlogger.R
import kotlinx.android.synthetic.main.sensor_value_widget.view.*

class SensorValueWidget : MaterialCardView {
    constructor(context: Context, valueCount: Int) : super(context) {
        setup(valueCount)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setup()
    }

    private fun setup(valueCount: Int = 0) {
        inflate(context, R.layout.sensor_value_widget, this)
        setBackgroundColor(ContextCompat.getColor(context, R.color.white))

        text_x.text = "0.0"
        text_y.text = "0.0"
        text_z.text = "0.0"
        text_x2.text = "0.0"
        text_y2.text = "0.0"
        text_z2.text = "0.0"

        when (valueCount) {
            0 -> {
                // all gone by default
            }
            1 -> {
                group_x.visibility = View.VISIBLE
            }
            2 -> {
                group_x.visibility = View.VISIBLE
                group_y.visibility = View.VISIBLE
            }
            3 -> {
                group_x.visibility = View.VISIBLE
                group_y.visibility = View.VISIBLE
                group_z.visibility = View.VISIBLE
            }
            4 -> {
                group_x.visibility = View.VISIBLE
                group_y.visibility = View.VISIBLE
                group_z.visibility = View.VISIBLE
                group_x2.visibility = View.VISIBLE
            }
            5 -> {
                group_x.visibility = View.VISIBLE
                group_y.visibility = View.VISIBLE
                group_z.visibility = View.VISIBLE
                group_x2.visibility = View.VISIBLE
                group_y2.visibility = View.VISIBLE
            }
            6 -> {
                group_x.visibility = View.VISIBLE
                group_y.visibility = View.VISIBLE
                group_z.visibility = View.VISIBLE
                group_x2.visibility = View.VISIBLE
                group_y2.visibility = View.VISIBLE
                group_z2.visibility = View.VISIBLE
            }
            else -> {
                // all gone by default
            }
        }
    }

    fun updateValue(valueCount: Int,
                    accuracy: Int,
                    x: Float = 0f, y: Float = 0f, z: Float = 0f,
                    x2: Float = 0f, y2: Float = 0f, z2: Float = 0f,
                    x_unit: String = "", y_unit: String = "", z_unit: String = "",
                    x2_unit: String = "", y2_unit: String = "", z2_unit: String = "") {

        when (accuracy) {
            SENSOR_STATUS_UNRELIABLE -> text_accuracy.text = context.getString(R.string.sensor_accuracy_unreliable)
            SENSOR_STATUS_ACCURACY_LOW -> text_accuracy.text = context.getString(R.string.sensor_accuracy_low)
            SENSOR_STATUS_ACCURACY_MEDIUM -> text_accuracy.text = context.getString(R.string.sensor_accuracy_medium)
            SENSOR_STATUS_ACCURACY_HIGH -> text_accuracy.text = context.getString(R.string.sensor_accuracy_high)
        }

        when (valueCount) {
            0 -> {
                group_x.visibility = View.GONE
                group_y.visibility = View.GONE
                group_z.visibility = View.GONE
                group_x2.visibility = View.GONE
                group_y2.visibility = View.GONE
                group_z2.visibility = View.GONE
            }
            1 -> {
                group_x.visibility = View.VISIBLE
                group_y.visibility = View.GONE
                group_z.visibility = View.GONE
                group_x2.visibility = View.GONE
                group_y2.visibility = View.GONE
                group_z2.visibility = View.GONE

                text_x.text = x.toString()
                text_x_unit.text = x_unit
            }
            2 -> {
                group_x.visibility = View.VISIBLE
                group_y.visibility = View.VISIBLE
                group_z.visibility = View.GONE
                group_x2.visibility = View.GONE
                group_y2.visibility = View.GONE
                group_z2.visibility = View.GONE

                text_x.text = x.toString()
                text_x_unit.text = x_unit
                text_y.text = y.toString()
                text_y_unit.text = y_unit
            }
            3 -> {
                group_x.visibility = View.VISIBLE
                group_y.visibility = View.VISIBLE
                group_z.visibility = View.VISIBLE
                group_x2.visibility = View.GONE
                group_y2.visibility = View.GONE
                group_z2.visibility = View.GONE

                text_x.text = x.toString()
                text_x_unit.text = x_unit
                text_y.text = y.toString()
                text_y_unit.text = y_unit
                text_z.text = z.toString()
                text_z_unit.text = z_unit
            }
            4 -> {
                group_x.visibility = View.VISIBLE
                group_y.visibility = View.VISIBLE
                group_z.visibility = View.VISIBLE
                group_x2.visibility = View.VISIBLE
                group_y2.visibility = View.GONE
                group_z2.visibility = View.GONE

                text_x.text = x.toString()
                text_x_unit.text = x_unit
                text_y.text = y.toString()
                text_y_unit.text = y_unit
                text_z.text = z.toString()
                text_z_unit.text = z_unit
                text_x2.text = x2.toString()
                text_x2_unit.text = x2_unit
            }
            5 -> {
                group_x.visibility = View.VISIBLE
                group_y.visibility = View.VISIBLE
                group_z.visibility = View.VISIBLE
                group_x2.visibility = View.VISIBLE
                group_y2.visibility = View.VISIBLE
                group_z2.visibility = View.GONE

                text_x.text = x.toString()
                text_x_unit.text = x_unit
                text_y.text = y.toString()
                text_y_unit.text = y_unit
                text_z.text = z.toString()
                text_z_unit.text = z_unit
                text_x2.text = x2.toString()
                text_x2_unit.text = x2_unit
                text_y2.text = y2.toString()
                text_y2_unit.text = y2_unit
            }
            6 -> {
                group_x.visibility = View.VISIBLE
                group_y.visibility = View.VISIBLE
                group_z.visibility = View.VISIBLE
                group_x2.visibility = View.VISIBLE
                group_y2.visibility = View.VISIBLE
                group_z2.visibility = View.VISIBLE

                text_x.text = x.toString()
                text_x_unit.text = x_unit
                text_y.text = y.toString()
                text_y_unit.text = y_unit
                text_z.text = z.toString()
                text_z_unit.text = z_unit
                text_x2.text = x2.toString()
                text_x2_unit.text = x2_unit
                text_y2.text = y2.toString()
                text_y2_unit.text = y2_unit
                text_z2.text = z2.toString()
                text_z2_unit.text = z2_unit
            }
            else -> {
                group_x.visibility = View.GONE
                group_y.visibility = View.GONE
                group_z.visibility = View.GONE
                group_x2.visibility = View.GONE
                group_y2.visibility = View.GONE
                group_z2.visibility = View.GONE
            }
        }
    }
}