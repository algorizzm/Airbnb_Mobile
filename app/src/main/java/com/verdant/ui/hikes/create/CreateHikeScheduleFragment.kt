package com.verdant.ui.hikes.create

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCreateHikeScheduleBinding.bind(view)

        setupDateSelection()
        observeUi()
    }

    private fun setupDateSelection() {

        binding.layoutStart.setOnClickListener {
            showDatePicker()
        }

        binding.tvStart.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {

        val picker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select hike date")
                .build()

        picker.show(parentFragmentManager, "DATE_PICKER")

        picker.addOnPositiveButtonClickListener { selection ->

            val cal = Calendar.getInstance()

            cal.timeInMillis = selection

            showTimePicker(cal)
        }
    }

    private fun showTimePicker(cal: Calendar) {

        val picker =
            MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(cal.get(Calendar.HOUR_OF_DAY))
                .setMinute(cal.get(Calendar.MINUTE))
                .setTitleText("Select hike time")
                .build()

        picker.show(parentFragmentManager, "TIME_PICKER")

        picker.addOnPositiveButtonClickListener {

            cal.set(Calendar.HOUR_OF_DAY, picker.hour)
            cal.set(Calendar.MINUTE, picker.minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val millis = cal.timeInMillis

            flowVm.setStartMillis(millis)
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
                        } ?: "Select date & time"

                    binding.layoutFloatingCard.tvFloatingHikeTitle.text =
                        state.title.ifBlank {
                            "Untitled Hike"
                        }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}