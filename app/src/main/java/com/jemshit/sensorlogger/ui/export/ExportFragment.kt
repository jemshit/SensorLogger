package com.jemshit.sensorlogger.ui.export

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.jakewharton.rxbinding2.view.RxView
import com.jemshit.sensorlogger.R
import com.jemshit.sensorlogger.background_work.*
import com.jemshit.sensorlogger.ui.statistics.UIWorkStatus
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.export_fragment.*


class ExportFragment : Fragment() {

    private var exportViewModel: ExportViewModel? = null
    private lateinit var rxPermissions: RxPermissions
    private lateinit var compositeDisposable: CompositeDisposable
    private var clickedDeleteExportButton = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        compositeDisposable = CompositeDisposable()
        return inflater.inflate(R.layout.export_fragment, container, false)
    }

    // todo delete local data
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.title = getString(R.string.bottom_nav_export)
        clickedDeleteExportButton = false

        rxPermissions = RxPermissions(this)
        compositeDisposable.add(
                RxView.clicks(button_export)
                        .compose(rxPermissions.ensure(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(onNext = { granted ->
                            if (granted) {
                                if (exportViewModel != null) {
                                    if (exportViewModel!!.exportStatus.value!! !is UIWorkStatus.Loading) {
                                        val excludedAccuracies = mutableListOf<String>()
                                        if (checkbox_high.isChecked) excludedAccuracies.add(ACCURACY_HIGH_TEXT)
                                        if (checkbox_medium.isChecked) excludedAccuracies.add(ACCURACY_MEDIUM_TEXT)
                                        if (checkbox_low.isChecked) excludedAccuracies.add(ACCURACY_LOW_TEXT)
                                        if (checkbox_unreliable.isChecked) excludedAccuracies.add(ACCURACY_UNRELIABLE_TEXT)
                                        if (checkbox_unknown.isChecked) excludedAccuracies.add(ACCURACY_UNKNOWN_TEXT)

                                        clickedDeleteExportButton = false
                                        exportViewModel!!.export(excludedAccuracies.toTypedArray())
                                    }
                                }
                            } else {
                                Toast.makeText(context, getString(R.string.storage_permission_is_necessary), Toast.LENGTH_SHORT).show()
                            }
                        }, onError = {
                            Toast.makeText(context, it.message
                                    ?: getString(R.string.error), Toast.LENGTH_SHORT).show()
                        })
        )

        compositeDisposable.add(
                RxView.clicks(button_delete_exported_folder)
                        .compose(rxPermissions.ensure(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(onNext = { granted ->
                            if (exportViewModel != null) {
                                if (granted) {
                                    if (exportViewModel!!.deleteFolderStatus.value!! !is UIWorkStatus.Loading) {
                                        clickedDeleteExportButton = true
                                        exportViewModel!!.deleteExportedData()
                                    }
                                }
                            } else {
                                Toast.makeText(context, getString(R.string.storage_permission_is_necessary), Toast.LENGTH_SHORT).show()
                            }
                        }, onError = {
                            it.message
                                    ?: Toast.makeText(context, getString(R.string.error), Toast.LENGTH_SHORT).show()
                        })
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        compositeDisposable.clear()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        exportViewModel = ViewModelProviders.of(activity!!).get(ExportViewModel::class.java)

        exportViewModel!!.exportStatus
                .observe(this, Observer { status ->
                    when (status) {
                        is UIWorkStatus.Idle -> {
                            button_export.isEnabled = true
                            showIdle()
                        }
                        is UIWorkStatus.Loading -> {
                            button_export.isEnabled = false
                            showLoading()
                        }
                        is UIWorkStatus.Error -> {
                            button_export.isEnabled = true
                            showError(status.message)
                        }
                        is UIWorkStatus.Success -> {
                            button_export.isEnabled = true
                            showSuccess()
                        }
                    }
                })

        exportViewModel!!.deleteFolderStatus
                .observe(this, Observer { status ->
                    when (status) {
                        is UIWorkStatus.Idle -> {
                            button_delete_exported_folder.isEnabled = true
                        }
                        is UIWorkStatus.Loading -> {
                            button_delete_exported_folder.isEnabled = false
                            if (clickedDeleteExportButton)
                                Toast.makeText(context, getString(R.string.deleting_export_folder), Toast.LENGTH_SHORT).show()
                        }
                        is UIWorkStatus.Error -> {
                            button_delete_exported_folder.isEnabled = true
                            if (clickedDeleteExportButton)
                                Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                        }
                        is UIWorkStatus.Success -> {
                            button_delete_exported_folder.isEnabled = true
                            if (clickedDeleteExportButton)
                                Toast.makeText(context, getString(R.string.deleted_export_folder), Toast.LENGTH_SHORT).show()
                        }
                    }
                })
    }

    private fun showLoading() {
        enableCheckboxes(false)
        text_error.visibility = View.GONE
        text_success.visibility = View.GONE
        progress_bar.visibility = View.VISIBLE
    }


    private fun showIdle() {
        enableCheckboxes(true)
        progress_bar.visibility = View.GONE
        text_error.visibility = View.GONE
        text_success.visibility = View.GONE
    }

    private fun showSuccess() {
        enableCheckboxes(true)
        progress_bar.visibility = View.GONE
        text_error.visibility = View.GONE

        text_success.text = getString(R.string.exported_successfully)
        text_success.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        enableCheckboxes(true)
        progress_bar.visibility = View.GONE
        text_success.visibility = View.GONE
        text_error.text = message
        text_error.visibility = View.VISIBLE
    }

    private fun enableCheckboxes(enable: Boolean = true) {
        checkbox_high.isEnabled = enable
        checkbox_medium.isEnabled = enable
        checkbox_low.isEnabled = enable
        checkbox_unreliable.isEnabled = enable
        checkbox_unknown.isEnabled = enable
    }
}