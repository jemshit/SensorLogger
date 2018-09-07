package com.jemshit.sensorlogger.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.ui.statistics.StatisticsViewModel
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity() {

    private lateinit var sensorsViewModel: SensorsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        // findNavController(R.id.navigation_host) = (navigation_host as NavHostFragment).navController
        findNavController(R.id.navigation_host).apply {
            NavigationUI.setupWithNavController(bottom_navigation, this)

            addOnNavigatedListener { _, destination ->
                if (destination.id == R.id.sensorDetailFragment || destination.id == R.id.recordingInfoFragment)
                    bottom_navigation.visibility = View.GONE
                else
                    bottom_navigation.visibility = View.VISIBLE
            }
        }

        sensorsViewModel = ViewModelProviders.of(this).get(SensorsViewModel::class.java)
    }
}
