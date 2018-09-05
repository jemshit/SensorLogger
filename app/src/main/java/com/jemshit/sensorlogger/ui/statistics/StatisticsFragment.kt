package com.jemshit.sensorlogger.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.jemshit.sensorlogger.R
import io.reactivex.disposables.CompositeDisposable

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
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        statisticsViewModel = ViewModelProviders.of(activity!!).get(StatisticsViewModel::class.java)

        statisticsViewModel.allDataSorted
                .observe(this, Observer { allDataGrouped ->
                    Toast.makeText(context, "All Data ${allDataGrouped.size}", LENGTH_SHORT).show()
                })
        statisticsViewModel.getAllDataSortedGrouped()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}