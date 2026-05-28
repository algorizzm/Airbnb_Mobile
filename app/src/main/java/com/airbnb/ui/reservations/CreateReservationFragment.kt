package com.airbnb.ui.reservations

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
import com.airbnb.utils.formatting.CurrencyFormatter
import com.airbnb.utils.formatting.DateFormatter
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
        binding.btnCheckOut.isEnabled = false
        binding.btnCheckOut.alpha = 0.5f
        binding.btnReserve.isEnabled = false
        binding.btnReserve.alpha = 0.6f

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
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.reservation_select_check_in_date))
            .setSelection(viewModel.checkInDate.value?.time ?: MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            viewModel.setCheckInDate(dateFromPickerSelection(selection))
        }
        picker.show(parentFragmentManager, "checkInDatePicker")
    }

    private fun showCheckOutDatePicker() {
        val checkInDate = viewModel.checkInDate.value
        if (checkInDate == null) {
            Toast.makeText(requireContext(), getString(R.string.toast_select_check_in_first), Toast.LENGTH_SHORT).show()
            return
        }

        val minDate = Calendar.getInstance()
        minDate.time = checkInDate
        minDate.add(Calendar.DAY_OF_MONTH, 1) // Day after check-in
        val constraints = CalendarConstraints.Builder()
            .setStart(minDate.timeInMillis)
            .setOpenAt(minDate.timeInMillis)
            .setValidator(DateValidatorPointForward.from(minDate.timeInMillis))
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.reservation_select_check_out_date))
            .setSelection(viewModel.checkOutDate.value?.time ?: minDate.timeInMillis)
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            viewModel.setCheckOutDate(dateFromPickerSelection(selection))
        }
        picker.show(parentFragmentManager, "checkOutDatePicker")
    }

    private fun dateFromPickerSelection(selection: Long): Date {
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = selection
        }
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, utcCalendar.get(Calendar.YEAR))
            set(Calendar.MONTH, utcCalendar.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utcCalendar.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
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
                        binding.btnCheckOut.isEnabled = date != null
                        binding.btnCheckOut.alpha = if (date != null) 1f else 0.5f
                        if (date != null) {
                            binding.tvCheckInDate.text = dateFormat.format(date)
                            binding.tvCheckInDate.setTextColor(
                                resources.getColor(android.R.color.white, null)
                            )
                        } else {
                            binding.tvCheckInDate.text = getString(R.string.reservation_select_date)
                            binding.tvCheckInDate.setTextColor(
                                resources.getColor(android.R.color.darker_gray, null)
                            )
                        }
                        updateReserveButtonState()
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
                            binding.tvCheckOutDate.text = getString(R.string.reservation_select_date)
                            binding.tvCheckOutDate.setTextColor(
                                resources.getColor(android.R.color.darker_gray, null)
                            )
                        }
                        updateReserveButtonState()
                    }
                }

                // Observe number of guests
                launch {
                    viewModel.numberOfGuests.collect { count ->
                        binding.tvGuestCount.text = count.toString()
                    }
                }

                // Observe number of nights using centralized formatter
                launch {
                    viewModel.numberOfNights.collect { nights ->
                        binding.tvNightsLabel.text = when (nights) {
                            0 -> "0 nights"
                            1 -> "1 night"
                            else -> "$nights nights"
                        }
                        updateReserveButtonState()
                    }
                }

                // Observe total price using centralized formatter
                launch {
                    viewModel.totalPrice.collect { price ->
                        binding.tvSubtotal.text = CurrencyFormatter.formatPrice(price)
                        binding.tvTotalPrice.text = CurrencyFormatter.formatPrice(price)
                    }
                }

                // Observe loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        updateReserveButtonState()
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

    private fun updateReserveButtonState() {
        val canReserve = viewModel.checkInDate.value != null &&
            viewModel.checkOutDate.value != null &&
            viewModel.numberOfNights.value > 0 &&
            !viewModel.isLoading.value
        binding.btnReserve.isEnabled = canReserve
        binding.btnReserve.alpha = if (canReserve) 1f else 0.6f
    }
}
