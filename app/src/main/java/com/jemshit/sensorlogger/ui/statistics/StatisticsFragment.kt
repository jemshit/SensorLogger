package com.jemshit.sensorlogger.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.helper.toPx
import com.jemshit.sensorlogger.model.ActivityStatistics
import com.jemshit.sensorlogger.ui.statistics.widget.StatisticsActivityItemWidget
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.statistics_fragment.*

class StatisticsFragment : Fragment() {

    private lateinit var statisticsViewModel: StatisticsViewModel
    private lateinit var compositeDisposable: CompositeDisposable
    private lateinit var widgetLayoutParams: LinearLayout.LayoutParams

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
        widgetLayoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        widgetLayoutParams.setMargins(8.toPx)
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
                        }
                        is CalculationStatus.Loading -> {
                            showLoading()
                        }
                        is CalculationStatus.Error -> {
                            showError(status.message)
                        }
                        is CalculationStatus.Success -> {
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
        text_error.visibility = View.GONE
        layout_stats.visibility = View.GONE
        progress_bar.visibility = View.VISIBLE
    }


    private fun showIdle() {
        progress_bar.visibility = View.GONE
        text_error.visibility = View.GONE
    }

    private fun showContent(statistics: Map<String, ActivityStatistics>) {

        layout_stats.removeAllViews()
        statistics.forEach { entry ->
            val widget = StatisticsActivityItemWidget(context!!, entry.key, entry.value)
            widget.layoutParams = widgetLayoutParams
            layout_stats.addView(widget)
        }

        progress_bar.visibility = View.GONE
        text_error.visibility = View.GONE
        layout_stats.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        progress_bar.visibility = View.GONE
        text_error.text = message
        text_error.visibility = View.VISIBLE
    }
}