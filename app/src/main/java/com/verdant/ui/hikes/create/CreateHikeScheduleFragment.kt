package com.verdant.ui.hikes.create

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.verdant.R
import com.verdant.databinding.FragmentCreateHikeScheduleBinding
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Calendar

class CreateHikeScheduleFragment :
    Fragment(R.layout.fragment_create_hike_schedule) {

    private var _binding: FragmentCreateHikeScheduleBinding? = null
    private val binding get() = _binding!!

    private val flowVm get() = createHikeFlowViewModel()

    private val fmt: DateFormat =
        DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT
        )

    private var selectingStart = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCreateHikeScheduleBinding.bind(view)

        setupDateSelection()
        setupTimePicker()
        observeUi()
    }

    private fun setupDateSelection() {

        binding.tvStart.setOnClickListener {
            selectingStart = true
            showDatePicker()
        }

        binding.tvEnd.setOnClickListener {
            selectingStart = false
            showDatePicker()
        }
    }

    private fun showDatePicker() {

        val initial =
            if (selectingStart)
                flowVm.ui.value.startMillis
            else
                flowVm.ui.value.endMillis

        val cal = Calendar.getInstance()

        if (initial != null) {
            cal.timeInMillis = initial
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->

                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)

                applySelectedDate(cal)

            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupTimePicker() {

        val picker = binding.timePicker

        picker.setIs24HourView(false)

        picker.setOnTimeChangedListener { _: TimePicker, hour: Int, minute: Int ->

            val current =
                if (selectingStart)
                    flowVm.ui.value.startMillis
                else
                    flowVm.ui.value.endMillis

            val cal = Calendar.getInstance()

            if (current != null) {
                cal.timeInMillis = current
            }

            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val millis = cal.timeInMillis

            if (selectingStart) {
                flowVm.setStartMillis(millis)
            } else {
                flowVm.setEndMillis(millis)
            }
        }
    }

    private fun applySelectedDate(cal: Calendar) {

        val current =
            if (selectingStart)
                flowVm.ui.value.startMillis
            else
                flowVm.ui.value.endMillis

        if (current != null) {

            val existing = Calendar.getInstance()
            existing.timeInMillis = current

            cal.set(Calendar.HOUR_OF_DAY,
                existing.get(Calendar.HOUR_OF_DAY))

            cal.set(Calendar.MINUTE,
                existing.get(Calendar.MINUTE))
        }

        val millis = cal.timeInMillis

        if (selectingStart) {
            flowVm.setStartMillis(millis)
        } else {
            flowVm.setEndMillis(millis)
        }
    }

    private fun observeUi() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                flowVm.ui.collect { state ->

                    binding.tvStart.text =
                        state.startMillis?.let {
                            fmt.format(it)
                        } ?: "Tap to set start"

                    binding.tvEnd.text =
                        state.endMillis?.let {
                            fmt.format(it)
                        } ?: "Tap to set end"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}