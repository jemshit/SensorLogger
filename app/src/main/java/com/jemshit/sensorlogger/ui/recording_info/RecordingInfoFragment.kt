package com.jemshit.sensorlogger.ui.recording_info

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.jemshit.sensorlogger.R
import kotlinx.android.synthetic.main.recording_info_fragment.*

class RecordingInfoFragment : Fragment() {

    companion object {
        fun newInstance() = RecordingInfoFragment()
    }

    private lateinit var viewModel: RecordingInfoViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.recording_info_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(RecordingInfoViewModel::class.java)
        // TODO: Use the ViewModel

        image_close.setOnClickListener {
            findNavController().navigateUp()
        }
    }

}
