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
import com.airbnb.utils.formatting.DateFormatter
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

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

        binding.btnCheckIn.setOnClickListener {
            showCheckInConfirmationDialog()
        }

        binding.btnCheckOut.setOnClickListener {
            showCheckOutConfirmationDialog()
        }

        binding.btnEarlyCheckOut.setOnClickListener {
            showEarlyCheckOutConfirmationDialog()
        }

        binding.btnLeaveReview.setOnClickListener {
            navigateToReviewSubmission()
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

        // Set reservation summary using centralized formatter
        binding.tvCheckIn.text = DateFormatter.formatFullDate(reservation.checkInDate)
        binding.tvCheckOut.text = DateFormatter.formatFullDate(reservation.checkOutDate)
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
        binding.tvBookedDate.text = DateFormatter.formatFullDate(reservation.createdAt)

        // Show/hide action buttons based on reservation state
        val canCheckIn = reservation.canCheckIn()
        val canCheckOut = reservation.canCheckOut()
        val canEarlyCheckOut = reservation.canEarlyCheckOut()
        val isCancellable = reservation.isCancellable()
        val canReview = reservation.canSubmitReview()
        
        // Debug logging
        android.util.Log.d("TripDetails", "Reservation status: ${reservation.status}")
        android.util.Log.d("TripDetails", "checkedIn: ${reservation.checkedIn}, checkedOut: ${reservation.checkedOut}")
        android.util.Log.d("TripDetails", "canCheckIn: $canCheckIn")
        android.util.Log.d("TripDetails", "canCheckOut: $canCheckOut")
        android.util.Log.d("TripDetails", "canEarlyCheckOut: $canEarlyCheckOut")
        android.util.Log.d("TripDetails", "isCancellable: $isCancellable")
        android.util.Log.d("TripDetails", "canReview: $canReview")
        
        binding.btnCancel.visibility = if (isCancellable) View.VISIBLE else View.GONE
        binding.btnCheckIn.visibility = if (canCheckIn) View.VISIBLE else View.GONE
        binding.btnCheckOut.visibility = if (canCheckOut && !canEarlyCheckOut) View.VISIBLE else View.GONE
        binding.btnEarlyCheckOut.visibility = if (canEarlyCheckOut) View.VISIBLE else View.GONE
        binding.btnLeaveReview.visibility = if (canReview) View.VISIBLE else View.GONE
    }
    
    private fun navigateToReviewSubmission() {
        reservationId?.let { id ->
            val bundle = Bundle().apply {
                putString("reservationId", id)
            }
            findNavController().navigate(
                R.id.action_tripDetails_to_reviewSubmission,
                bundle
            )
        }
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

    private fun showCheckInConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Check In")
            .setMessage("Ready to check in to your stay?")
            .setPositiveButton("Check In") { _, _ ->
                reservationId?.let { viewModel.checkInReservation(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCheckOutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Check Out")
            .setMessage("Ready to check out? We hope you enjoyed your stay!")
            .setPositiveButton("Check Out") { _, _ ->
                reservationId?.let { viewModel.checkOutReservation(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEarlyCheckOutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Early Check Out")
            .setMessage("You're checking out before your scheduled date. Continue?")
            .setPositiveButton("Check Out") { _, _ ->
                reservationId?.let { viewModel.earlyCheckOutReservation(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
