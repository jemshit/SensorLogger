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
import com.jemshit.sensorlogger.ui.main.exportBusy
import com.jemshit.sensorlogger.ui.statistics.UIWorkStatus
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.export_fragment.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch


class ExportFragment : Fragment() {

    private var exportViewModel: ExportViewModel? = null
    private lateinit var rxPermissions: RxPermissions
    private lateinit var compositeDisposable: CompositeDisposable
    private var clickedDeleteExportButton = false
    private var clickedDeleteLocalButton = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        compositeDisposable = CompositeDisposable()
        return inflater.inflate(R.layout.export_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.title = getString(R.string.bottom_nav_export)
        clickedDeleteExportButton = false
        clickedDeleteLocalButton = false

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

                                        val gender = if (radio_group_gender.checkedRadioButtonId == R.id.radio_male)
                                            GENDER_MALE
                                        else if (radio_group_gender.checkedRadioButtonId == R.id.radio_female)
                                            GENDER_FEMALE
                                        else
                                            ""

                                        clickedDeleteExportButton = false
                                        clickedDeleteLocalButton = false
                                        exportViewModel!!.export(
                                                excludedAccuracies.toTypedArray(),
                                                input_age.text.toString(),
                                                input_weight.text.toString(),
                                                input_height.text.toString(),
                                                gender
                                        )
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

        compositeDisposable.add(
                RxView.clicks(button_delete_local_data)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(onNext = {
                            if (exportViewModel!!.deleteLocalStatus.value!! !is UIWorkStatus.Loading) {
                                clickedDeleteLocalButton = true
                                exportViewModel!!.deleteLocalData()
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


        exportViewModel!!.deleteFolderStatus
                .observe(this, Observer { status ->
                    when (status) {
                        is UIWorkStatus.Idle -> {
                            enableButtons()
                        }
                        is UIWorkStatus.Loading -> {
                            enableButtons(false)

                            if (clickedDeleteExportButton)
                                Toast.makeText(context, getString(R.string.deleting_export_folder), Toast.LENGTH_SHORT).show()
                        }
                        is UIWorkStatus.Error -> {
                            enableButtons()

                            if (clickedDeleteExportButton)
                                Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                        }
                        is UIWorkStatus.Success -> {
                            enableButtons()

                            if (clickedDeleteExportButton)
                                Toast.makeText(context, getString(R.string.deleted_export_folder), Toast.LENGTH_SHORT).show()
                        }
                    }
                })

        exportViewModel!!.deleteLocalStatus
                .observe(this, Observer { status ->
                    when (status) {
                        is UIWorkStatus.Idle -> {
                            enableButtons()
                        }
                        is UIWorkStatus.Loading -> {
                            enableButtons(false)

                            if (clickedDeleteLocalButton)
                                Toast.makeText(context, getString(R.string.deleting_local_data), Toast.LENGTH_SHORT).show()
                        }
                        is UIWorkStatus.Error -> {
                            enableButtons()

                            if (clickedDeleteLocalButton)
                                Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
                        }
                        is UIWorkStatus.Success -> {
                            enableButtons()

                            if (clickedDeleteLocalButton)
                                Toast.makeText(context, getString(R.string.deleted_local_data), Toast.LENGTH_SHORT).show()
                        }
                    }
                })

        exportViewModel!!.exportStatus
                .observe(this, Observer { status ->
                    when (status) {
                        is UIWorkStatus.Idle -> {
                            enableButtons()
                            showIdle()
                        }
                        is UIWorkStatus.Loading -> {
                            enableButtons(false)
                            showLoading()
                        }
                        is UIWorkStatus.Error -> {
                            enableButtons()
                            showError(status.message)
                        }
                        is UIWorkStatus.Success -> {
                            enableButtons()
                            showSuccess()
                        }
                    }
                })
    }

    private fun showLoading() {
        enableInputs(false)
        text_error.visibility = View.GONE
        text_success.visibility = View.GONE
        progress_bar.visibility = View.VISIBLE
    }

    private fun showIdle() {
        enableInputs(true)
        progress_bar.visibility = View.GONE
        text_error.visibility = View.GONE
        text_success.visibility = View.GONE
    }

    private fun showSuccess() {
        enableInputs(true)
        progress_bar.visibility = View.GONE
        text_error.visibility = View.GONE

        text_success.text = getString(R.string.exported_successfully)
        text_success.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        enableInputs(true)
        progress_bar.visibility = View.GONE
        text_success.visibility = View.GONE
        text_error.text = message
        text_error.visibility = View.VISIBLE
    }

    private fun enableInputs(enable: Boolean = true) {
        checkbox_high.isEnabled = enable
        checkbox_medium.isEnabled = enable
        checkbox_low.isEnabled = enable
        checkbox_unreliable.isEnabled = enable
        checkbox_unknown.isEnabled = enable

        input_age.isEnabled = enable
        input_weight.isEnabled = enable
        input_height.isEnabled = enable
        radio_male.isEnabled = enable
        radio_female.isEnabled = enable
    }

    private fun enableButtons(enable: Boolean = true) {
        if (enable) {
            launch {
                delay(1000) // exportBusy might get updated little late (race condition bwn MainActivity and ExportFragment observer)

                launch(UI) {
                    if (!exportBusy) {
                        button_delete_exported_folder?.isEnabled = enable
                        button_delete_local_data?.isEnabled = enable
                        button_export?.isEnabled = enable
                    }
                }
            }

        } else {
            button_delete_exported_folder.isEnabled = enable
            button_delete_local_data.isEnabled = enable
            button_export.isEnabled = enable
        }
    }
}