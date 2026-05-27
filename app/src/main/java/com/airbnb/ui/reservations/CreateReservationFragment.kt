package com.airbnb.ui.reservations

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.airbnb.databinding.FragmentCreateReservationBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CreateReservationFragment : Fragment(R.layout.fragment_create_reservation) {

    private var _binding: FragmentCreateReservationBinding? = null
    private val binding get() = _binding!!

    private val listingId: String by lazy {
        arguments?.getString(ARG_LISTING_ID).orEmpty()
    }

    private val viewModel: CreateReservationViewModel by viewModels {
        CreateReservationViewModel.Factory(listingId)
    }

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateReservationBinding.bind(view)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCheckIn.setOnClickListener {
            showCheckInDatePicker()
        }

        binding.btnCheckOut.setOnClickListener {
            showCheckOutDatePicker()
        }

        binding.btnDecreaseGuests.setOnClickListener {
            val current = viewModel.numberOfGuests.value
            if (current > 1) {
                viewModel.setNumberOfGuests(current - 1)
            }
        }

        binding.btnIncreaseGuests.setOnClickListener {
            val current = viewModel.numberOfGuests.value
            viewModel.setNumberOfGuests(current + 1)
        }

        binding.btnReserve.setOnClickListener {
            viewModel.createReservation()
        }
    }

    private fun showCheckInDatePicker() {
        val calendar = Calendar.getInstance()
        val checkInDate = viewModel.checkInDate.value
        if (checkInDate != null) {
            calendar.time = checkInDate
        }

        val minDate = Calendar.getInstance()
        minDate.add(Calendar.DAY_OF_MONTH, 1) // Tomorrow

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance()
                selected.set(year, month, dayOfMonth)
                viewModel.setCheckInDate(selected.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = minDate.timeInMillis
            show()
        }
    }

    private fun showCheckOutDatePicker() {
        val checkInDate = viewModel.checkInDate.value
        if (checkInDate == null) {
            Toast.makeText(requireContext(), "Please select check-in date first", Toast.LENGTH_SHORT).show()
            return
        }

        val calendar = Calendar.getInstance()
        val checkOutDate = viewModel.checkOutDate.value
        if (checkOutDate != null) {
            calendar.time = checkOutDate
        } else {
            calendar.time = checkInDate
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val minDate = Calendar.getInstance()
        minDate.time = checkInDate
        minDate.add(Calendar.DAY_OF_MONTH, 1) // Day after check-in

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance()
                selected.set(year, month, dayOfMonth)
                viewModel.setCheckOutDate(selected.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = minDate.timeInMillis
            show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe listing
                launch {
                    viewModel.listing.collect { listing ->
                        if (listing != null) {
                            binding.tvListingTitle.text = listing.title
                            binding.tvListingLocation.text = listing.location
                            binding.tvPricePerNight.text = listing.formattedPrice()
                            binding.tvMaxGuests.text = "Maximum ${listing.maxGuests} guests"
                        }
                    }
                }

                // Observe check-in date
                launch {
                    viewModel.checkInDate.collect { date ->
                        if (date != null) {
                            binding.tvCheckInDate.text = dateFormat.format(date)
                            binding.tvCheckInDate.setTextColor(
                                resources.getColor(android.R.color.white, null)
                            )
                        } else {
                            binding.tvCheckInDate.text = "Select date"
                            binding.tvCheckInDate.setTextColor(
                                resources.getColor(android.R.color.darker_gray, null)
                            )
                        }
                    }
                }

                // Observe check-out date
                launch {
                    viewModel.checkOutDate.collect { date ->
                        if (date != null) {
                            binding.tvCheckOutDate.text = dateFormat.format(date)
                            binding.tvCheckOutDate.setTextColor(
                                resources.getColor(android.R.color.white, null)
                            )
                        } else {
                            binding.tvCheckOutDate.text = "Select date"
                            binding.tvCheckOutDate.setTextColor(
                                resources.getColor(android.R.color.darker_gray, null)
                            )
                        }
                    }
                }

                // Observe number of guests
                launch {
                    viewModel.numberOfGuests.collect { count ->
                        binding.tvGuestCount.text = count.toString()
                    }
                }

                // Observe number of nights
                launch {
                    viewModel.numberOfNights.collect { nights ->
                        binding.tvNightsLabel.text = when (nights) {
                            0 -> "0 nights"
                            1 -> "1 night"
                            else -> "$nights nights"
                        }
                    }
                }

                // Observe total price
                launch {
                    viewModel.totalPrice.collect { price ->
                        binding.tvSubtotal.text = "₱${price.toInt()}"
                        binding.tvTotalPrice.text = "₱${price.toInt()}"
                    }
                }

                // Observe loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.btnReserve.isEnabled = !isLoading
                    }
                }

                // Observe reservation created
                launch {
                    viewModel.reservationCreated.collect { created ->
                        if (created) {
                            // Navigate back or to trips screen
                            findNavController().popBackStack()
                        }
                    }
                }

                // Observe toast messages
                launch {
                    viewModel.toast.collect { message ->
                        if (!message.isNullOrBlank()) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            viewModel.consumeToast()
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

    companion object {
        const val ARG_LISTING_ID = "listingId"
    }
}
