package com.jemshit.sensorlogger.ui.statistics.widget

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.SensorManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.data.sensor_preference.SensorPreferenceEntity
import com.jemshit.sensorlogger.helper.toDelimeterString
import com.jemshit.sensorlogger.helper.toPx
import com.jemshit.sensorlogger.helper.toTimeString
import com.jemshit.sensorlogger.model.ActivityStatistics
import com.jemshit.sensorlogger.model.PositionStatistics
import com.jemshit.sensorlogger.model.getSensorSimpleNameFromType
import kotlinx.android.synthetic.main.statistics_activity_item_widget.view.*
import kotlinx.android.synthetic.main.statistics_position_item_widget.view.*
import kotlinx.android.synthetic.main.statistics_sensor_item_widget.view.*

@SuppressLint("ViewConstructor")
class StatisticsActivityItemWidget(context: Context,
                                   activityName: String,
                                   stats: ActivityStatistics,
                                   sensorPreferences: List<SensorPreferenceEntity>,
                                   sensorManager: SensorManager)
    : MaterialCardView(context) {

    init {
        inflate(context, R.layout.statistics_activity_item_widget, this)
        setBackgroundColor(ContextCompat.getColor(context, R.color.white))
        setContentPadding(4.toPx, 4.toPx, 4.toPx, 4.toPx)

        stats.sensorAccuracyStatistics.forEach { entry ->
            val sensorItemWidget = StatisticsSensorItemWidget(context, entry.key, entry.value)
            layout_sensors.addView(sensorItemWidget)
        }

        stats.positionStatistics.forEach { item ->
            val positionItemWidget = StatisticsPositionItemWidget(context, item)
            layout_positions.addView(positionItemWidget)
        }
        text_activity_name.text = "${activityName.toUpperCase()} (${stats.count.toDelimeterString()} - ${stats.durationInMs.toTimeString(enableDay = true)})"
    }
}

@SuppressLint("ViewConstructor")
class StatisticsSensorItemWidget(context: Context, sensorType: String, stats: List<Pair<String, Long>>) : ConstraintLayout(context) {

    init {
        setup(sensorType, stats)
    }

    private fun setup(sensorType: String, stats: List<Pair<String, Long>>) {
        MaterialCardView.inflate(context, R.layout.statistics_sensor_item_widget, this)

        var totalCount = 0L
        val accuracyStatisticsText = StringBuilder()
        stats.forEach {
            totalCount += it.second
            accuracyStatisticsText.append(it.first)
            accuracyStatisticsText.append(" (")
            accuracyStatisticsText.append(it.second.toDelimeterString())
            accuracyStatisticsText.append(")")
            accuracyStatisticsText.append("\n")
        }

        accuracyStatisticsText.removeSuffix("\n")

        text_accuracy_statistics.text = accuracyStatisticsText.toString()
        text_sensor_name.text = "${getSensorSimpleNameFromType(sensorType)} (${totalCount.toDelimeterString()})"
    }
}

@SuppressLint("ViewConstructor")
class StatisticsPositionItemWidget(context: Context, positionStatistics: PositionStatistics) : ConstraintLayout(context) {

    init {
        setup(positionStatistics)
    }

    private fun setup(positionStatistics: PositionStatistics) {
        MaterialCardView.inflate(context, R.layout.statistics_position_item_widget, this)
        text_position_name.text = "${positionStatistics.name} (${positionStatistics.count.toDelimeterString()})"

        val orientationStatisticsText = StringBuilder()
        positionStatistics.orientationStatistics.forEach {
            orientationStatisticsText.append(it.name)
            orientationStatisticsText.append(" (")
            orientationStatisticsText.append(it.count.toDelimeterString())
            orientationStatisticsText.append(")")
            orientationStatisticsText.append("\n")
        }
        orientationStatisticsText.removeSuffix("\n")

        text_orientation_statistics.text = orientationStatisticsText.toString()
    }
}
