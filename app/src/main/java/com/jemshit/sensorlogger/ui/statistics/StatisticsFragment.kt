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
                            statisticsViewModel.getAllStatistics()
                        }
                        is CalculationStatus.Loading -> {
                            Log.d("CustomLog", "Loading")
                            showLoading()
                        }
                        is CalculationStatus.Error -> {
                            Log.d("CustomLog", "Error")
                            showError(status.message)
                            showLogErrors(statisticsViewModel.loggedErrors)
                        }
                        is CalculationStatus.Success -> {
                            Log.d("CustomLog", "Success")
                            showContent(statisticsViewModel.allStatistics.toList())
                            showLogErrors(statisticsViewModel.loggedErrors)
                        }
                    }
                })

        refresh.setOnClickListener {
            if (statisticsViewModel.calculationStatus.value!! !is CalculationStatus.Loading)
                statisticsViewModel.getAllStatistics()
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

    private fun showContent(statistics: List<ActivityStatistics>) {
        progress_bar.visibility = View.GONE
        text_error.visibility = View.GONE

        // todo show success
        Log.d("CustomLog", statistics.toString())
    }

    private fun showLogErrors(errors: List<String>) {
        if (errors.isEmpty())
            card_log_errors.visibility = View.GONE
        else {
            val stringBuilder = StringBuilder()
            errors.forEachIndexed { index, value ->
                stringBuilder.append(value)
                if (index != (errors.size - 1))
                    stringBuilder.append("\n")
            }

            text_log_errors.text = stringBuilder.toString()
            card_log_errors.visibility = View.VISIBLE
        }
    }

    private fun showError(message: String) {
        progress_bar.visibility = View.GONE
        text_error.text = message
        text_error.visibility = View.VISIBLE
    }
}