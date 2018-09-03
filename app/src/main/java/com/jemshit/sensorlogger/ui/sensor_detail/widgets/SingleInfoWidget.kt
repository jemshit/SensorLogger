package com.jemshit.sensorlogger.ui.sensor_detail.widgets

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView
import com.jemshit.sensorlogger.R
import kotlinx.android.synthetic.main.single_info_widget.view.*

class SingleInfoWidget : MaterialCardView {
    constructor(context: Context) : super(context) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setup()
    }

    private fun setup() {
        inflate(context, R.layout.single_info_widget, this)
        text_value.text = "0.0"
    }

    fun updateInfo(value: Float) {
        text_value.text = value.toString()
    }
}