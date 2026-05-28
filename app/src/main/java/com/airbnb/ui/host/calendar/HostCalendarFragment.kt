package com.airbnb.ui.host.calendar

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.R
import com.airbnb.core.ui.EditTextDialog
import com.airbnb.databinding.FragmentHostCalendarBinding
import com.airbnb.utils.DateNormalizationUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import android.widget.AdapterView

/**
 * Host Calendar screen - allows hosts to:
 * - View reservations for their listings
 * - Block date ranges
 * - Unblock date ranges
 * - Switch between multiple listings
 */
class HostCalendarFragment : Fragment(R.layout.fragment_host_calendar) {
    
    private var _binding: FragmentHostCalendarBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HostCalendarViewModel by viewModels()
    
    private lateinit var reservationsAdapter: CalendarReservationsAdapter
    private lateinit var blockedDatesAdapter: BlockedDatesAdapter
    
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    private var selectedStartDate: Date? = null
    private var selectedEndDate: Date? = null
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHostCalendarBinding.bind(view)
        
        setupAdapters()
        setupListingSpinner()
        setupBlockDateButton()
        observeViewModel()
    }
    
    private fun setupAdapters() {
        // Reservations adapter
        reservationsAdapter = CalendarReservationsAdapter()
        binding.rvReservations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reservationsAdapter
        }
        
        // Blocked dates adapter
        blockedDatesAdapter = BlockedDatesAdapter(
            onUnblockClick = { blockedDate ->
                viewModel.unblockDateRange(blockedDate.id)
            }
        )
        binding.rvBlockedDates.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = blockedDatesAdapter
        }
    }

    private fun setupListingSpinner() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hostListings.collect { listings ->

                    if (listings.isEmpty()) {
                        binding.spinnerListings.visibility = View.GONE
                        binding.tvNoListings.visibility = View.VISIBLE
                        binding.layoutCalendarContent.visibility = View.GONE
                        return@collect
                    }

                    binding.spinnerListings.visibility = View.VISIBLE
                    binding.tvNoListings.visibility = View.GONE
                    binding.layoutCalendarContent.visibility = View.VISIBLE

                    val listingTitles = listings.map { it.title }

                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        listingTitles
                    )

                    adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item
                    )

                    binding.spinnerListings.adapter = adapter

                    binding.spinnerListings.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {

                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                viewModel.selectListing(listings[position])
                            }

                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                }
            }
        }
    }
    
    private fun setupBlockDateButton() {
        binding.btnBlockDates.setOnClickListener {
            showDateRangePicker()
        }
    }
    
    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance()
        
        // Select start date
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedStartDate = DateNormalizationUtil.normalizeToMidnight(calendar.time)
                
                // Now select end date
                showEndDatePicker()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
            setTitle("Select start date")
            show()
        }
    }
    
    private fun showEndDatePicker() {
        val startDate = selectedStartDate ?: return
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        calendar.add(Calendar.DAY_OF_MONTH, 1) // End date must be after start date
        
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedEndDate = DateNormalizationUtil.normalizeToMidnight(calendar.time)
                
                // Show reason dialog
                showReasonDialog()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = startDate.time + (24 * 60 * 60 * 1000) // Next day
            setTitle("Select end date")
            show()
        }
    }
    
    private fun showReasonDialog() {
        val startDate = selectedStartDate ?: return
        val endDate = selectedEndDate ?: return
        
        EditTextDialog.show(
            context = requireContext(),
            title = "Block Dates",
            hint = "Reason (optional)",
            initial = "",
            maxLength = 200,
            multiLine = false,
            onSave = { reason ->
                viewModel.blockDateRange(startDate, endDate, reason)
                selectedStartDate = null
                selectedEndDate = null
            }
        )
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.reservations.collect { reservations ->
                        reservationsAdapter.submitList(reservations)

                        binding.tvNoReservations.visibility =
                            if (reservations.isEmpty()) View.VISIBLE
                            else View.GONE
                    }
                }

                launch {
                    viewModel.blockedDates.collect { blockedDates ->
                        blockedDatesAdapter.submitList(blockedDates)

                        binding.tvNoBlockedDates.visibility =
                            if (blockedDates.isEmpty()) View.VISIBLE
                            else View.GONE
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility =
                            if (isLoading) View.VISIBLE
                            else View.GONE
                    }
                }

                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            Toast.makeText(
                                requireContext(),
                                it,
                                Toast.LENGTH_LONG
                            ).show()

                            viewModel.clearError()
                        }
                    }
                }

                launch {
                    viewModel.successMessage.collect { message ->
                        message?.let {
                            Toast.makeText(
                                requireContext(),
                                it,
                                Toast.LENGTH_SHORT
                            ).show()

                            viewModel.clearSuccess()
                        }
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
