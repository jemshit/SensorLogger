package com.jemshit.sensorlogger.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.jemshit.sensorlogger.R

class MainActivity : AppCompatActivity() {

    private lateinit var sensorsViewModel: SensorsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        sensorsViewModel = ViewModelProviders.of(this).get(SensorsViewModel::class.java)
    }
}
