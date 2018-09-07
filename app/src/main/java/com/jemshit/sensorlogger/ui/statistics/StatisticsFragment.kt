package com.jemshit.sensorlogger.ui.statistics

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.model.ActivityStatistics
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.statistics_fragment.*

class StatisticsFragment : Fragment() {

    private lateinit var statisticsViewModel: StatisticsViewModel
    private lateinit var compositeDisposable: CompositeDisposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        compositeDisposable = CompositeDisposable()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.statistics_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showLoading()
        toolbar.title = getString(R.string.bottom_nav_statistics)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        statisticsViewModel = ViewModelProviders.of(activity!!).get(StatisticsViewModel::class.java)

        statisticsViewModel.calculationStatus
                .observe(this, Observer { status ->
                    when (status) {
                        is CalculationStatus.Idle -> {
                            statisticsViewModel.calculateStatistics()
                            showIdle()
                            Log.d("CustomLog", "Idle")
                        }
                        is CalculationStatus.Loading -> {
                            Log.d("CustomLog", "Loading")
                            showLoading()
                        }
                        is CalculationStatus.Error -> {
                            Log.d("CustomLog", "Error $status.message")
                            showError(status.message)
                        }
                        is CalculationStatus.Success -> {
                            Log.d("CustomLog", "Success ${statisticsViewModel.statistics}")
                            showContent(statisticsViewModel.statistics)
                        }
                    }
                })

        refresh.setOnClickListener {
            if (statisticsViewModel.calculationStatus.value!! !is CalculationStatus.Loading)
                statisticsViewModel.calculateStatistics()
            else
                showError(getString(R.string.error_already_calculating))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    private fun showLoading() {
        progress_bar.visibility = View.VISIBLE
        text_error.visibility = View.GONE
    }


    private fun showIdle() {
        progress_bar.visibility = View.GONE
        text_error.visibility = View.GONE
    }

    private fun showContent(statistics: Map<String, ActivityStatistics>) {
        progress_bar.visibility = View.GONE
        text_error.visibility = View.GONE

        // todo show success
        Log.d("CustomLog", statistics.toString())
    }

    private fun showError(message: String) {
        progress_bar.visibility = View.GONE
        text_error.text = message
        text_error.visibility = View.VISIBLE
    }
}