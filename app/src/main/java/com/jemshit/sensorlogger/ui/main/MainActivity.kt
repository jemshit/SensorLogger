package com.jemshit.sensorlogger.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.ui.export.ExportViewModel
import com.jemshit.sensorlogger.ui.statistics.StatisticsViewModel
import com.jemshit.sensorlogger.ui.statistics.UIWorkStatus
import kotlinx.android.synthetic.main.main_activity.*

var statisticsBusy = false
var exportBusy = false

class MainActivity : AppCompatActivity() {

    private lateinit var sensorsViewModel: SensorsViewModel
    private lateinit var statisticsViewModel: StatisticsViewModel
    private lateinit var exportViewModel: ExportViewModel

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
        statisticsViewModel = ViewModelProviders.of(this).get(StatisticsViewModel::class.java)
        exportViewModel = ViewModelProviders.of(this).get(ExportViewModel::class.java)

        // Statuses
        statisticsViewModel.calculationStatus.observe(this, Observer {
            statisticsBusy = when (it) {
                is UIWorkStatus.Idle, is UIWorkStatus.Error, is UIWorkStatus.Success -> false
                is UIWorkStatus.Loading -> true
            }
        })
        exportViewModel.exportStatus.observe(this, Observer {
            exportBusy = when (it) {
                is UIWorkStatus.Idle, is UIWorkStatus.Error, is UIWorkStatus.Success -> false
                is UIWorkStatus.Loading -> true
            }
        })
        exportViewModel.deleteLocalStatus.observe(this, Observer {
            exportBusy = when (it) {
                is UIWorkStatus.Idle, is UIWorkStatus.Error, is UIWorkStatus.Success -> false
                is UIWorkStatus.Loading -> true
            }
        })
        exportViewModel.deleteFolderStatus.observe(this, Observer {
            exportBusy = when (it) {
                is UIWorkStatus.Idle, is UIWorkStatus.Error, is UIWorkStatus.Success -> false
                is UIWorkStatus.Loading -> true
            }
        })
    }
}
