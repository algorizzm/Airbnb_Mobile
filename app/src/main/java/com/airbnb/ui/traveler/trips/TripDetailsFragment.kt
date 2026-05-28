package com.airbnb.ui.traveler.trips

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.airbnb.databinding.FragmentTripDetailsBinding
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class TripDetailsFragment : Fragment(R.layout.fragment_trip_details) {

    private var _binding: FragmentTripDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TripDetailsViewModel by viewModels()

    private var reservationId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentTripDetailsBinding.bind(view)

        // Get reservation ID from arguments
        reservationId = arguments?.getString("reservationId")

        if (reservationId == null) {
            Toast.makeText(requireContext(), "Invalid reservation", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        setupUI()
        observeUi()

        // Load reservation details
        viewModel.loadReservation(reservationId!!)
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCancel.setOnClickListener {
            showCancelConfirmationDialog()
        }
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.tripItem.collect { tripItem ->
                        tripItem?.let { bindTripDetails(it) }
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.toast.collect { msg ->
                        if (!msg.isNullOrBlank()) {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            viewModel.consumeToast()
                        }
                    }
                }

                launch {
                    viewModel.navigationEvent.collect { shouldNavigateBack ->
                        if (shouldNavigateBack) {
                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }
    }

    private fun bindTripDetails(tripItem: com.airbnb.data.model.TripItem) {
        val reservation = tripItem.reservation
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        // Load listing image
        if (tripItem.imageUrl().isNotBlank()) {
            Glide.with(this)
                .load(tripItem.imageUrl())
                .placeholder(R.drawable.img_hike_placeholder)
                .error(R.drawable.img_hike_placeholder)
                .centerCrop()
                .into(binding.imgListing)
        }

        // Set status chip
        binding.tvStatusChip.text = reservation.statusLabel()

        // Set listing details
        binding.tvListingTitle.text = tripItem.title()
        binding.tvLocation.text = tripItem.location()

        // Set reservation summary
        binding.tvCheckIn.text = reservation.checkInDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
        binding.tvCheckOut.text = reservation.checkOutDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
        binding.tvGuests.text = reservation.guestSummary()
        binding.tvReservationCode.text = tripItem.reservationCode()

        // Set host information
        binding.tvHostName.text = tripItem.hostName()
        
        val hostAvatarUrl = tripItem.hostAvatarUrl()
        if (!hostAvatarUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(hostAvatarUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .circleCrop()
                .into(binding.imgHostAvatar)
        } else {
            binding.imgHostAvatar.setImageResource(R.drawable.ic_profile)
        }

        // Set payment summary
        binding.tvTotalPrice.text = reservation.formattedTotalPrice()
        binding.tvPaymentStatus.text = when (reservation.paymentStatus.lowercase()) {
            "paid" -> "Paid"
            "unpaid" -> "Unpaid"
            "refunded" -> "Refunded"
            else -> reservation.paymentStatus.replaceFirstChar { it.uppercase() }
        }

        // Set booking timeline
        binding.tvBookedDate.text = reservation.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "N/A"

        // Show cancel button if cancellable
        binding.btnCancel.visibility = if (reservation.isCancellable()) View.VISIBLE else View.GONE
    }

    private fun showCancelConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Reservation")
            .setMessage("Are you sure you want to cancel this reservation?")
            .setPositiveButton("Yes") { _, _ ->
                reservationId?.let { viewModel.cancelReservation(it) }
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
