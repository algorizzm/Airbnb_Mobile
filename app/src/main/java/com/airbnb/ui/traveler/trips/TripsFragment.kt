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
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.R
import com.airbnb.databinding.FragmentTripsBinding
import com.airbnb.ui.traveler.trips.adapter.TripAdapter
import com.airbnb.ui.auth.GuestPromptHelper
import com.airbnb.ui.auth.isUserAuthenticated
import kotlinx.coroutines.launch

class TripsFragment : Fragment(R.layout.fragment_trips) {

    private var _binding: FragmentTripsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TripsViewModel by viewModels()

    private lateinit var adapter: TripAdapter

    private var currentFilter: TripFilter = TripFilter.UPCOMING

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentTripsBinding.bind(view)

        // Check authentication status
        if (!isUserAuthenticated()) {
            showGuestState()
            return
        }

        setupAdapter()
        setupRecycler()
        setupFilters()
        observeUi()
    }

    private fun showGuestState() {
        GuestPromptHelper.setupGuestPrompt(
            promptLayout = binding.layoutGuestPrompt.root,
            fragment = this,
            title = getString(R.string.guest_prompt_title_trips),
            message = getString(R.string.guest_prompt_message_trips),
            iconRes = R.drawable.ic_calendar
        )
        
        // Hide filters and show guest prompt
        binding.layoutFilters.visibility = View.GONE
        GuestPromptHelper.showGuestPrompt(
            promptLayout = binding.layoutGuestPrompt.root,
            contentLayout = binding.recyclerTrips
        )
        binding.tvEmpty.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }

    private fun setupAdapter() {
        adapter = TripAdapter(
            onItemClick = { tripItem ->
                // Navigate to listing detail
                val bundle = Bundle().apply {
                    putString("listingId", tripItem.reservation.listingId)
                }
                try {
                    findNavController().navigate(
                        R.id.action_tripsFragment_to_listingDetailFragment,
                        bundle
                    )
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(requireContext(), getString(R.string.toast_listing_details_coming_soon), Toast.LENGTH_SHORT).show()
                }
            },
            onCancelClick = { tripItem ->
                showCancelConfirmationDialog(tripItem.reservation.id)
            }
        )
    }

    private fun setupRecycler() {
        binding.recyclerTrips.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerTrips.adapter = adapter
    }

    private fun setupFilters() {
        // Set initial filter state
        updateFilterButtons()

        binding.btnUpcoming.setOnClickListener {
            currentFilter = TripFilter.UPCOMING
            updateFilterButtons()
            updateTripsList()
        }

        binding.btnPast.setOnClickListener {
            currentFilter = TripFilter.PAST
            updateFilterButtons()
            updateTripsList()
        }

        binding.btnCancelled.setOnClickListener {
            currentFilter = TripFilter.CANCELLED
            updateFilterButtons()
            updateTripsList()
        }
    }

    private fun updateFilterButtons() {

        binding.btnUpcoming.setBackgroundResource(R.drawable.bg_chip_unselected)
        binding.btnPast.setBackgroundResource(R.drawable.bg_chip_unselected)
        binding.btnCancelled.setBackgroundResource(R.drawable.bg_chip_unselected)

        binding.btnUpcoming.setTextColor(requireContext().getColor(R.color.black))
        binding.btnPast.setTextColor(requireContext().getColor(R.color.black))
        binding.btnCancelled.setTextColor(requireContext().getColor(R.color.black))

        when (currentFilter) {
            TripFilter.UPCOMING -> {
                binding.btnUpcoming.setBackgroundResource(R.drawable.bg_chip_selected)
                binding.btnUpcoming.setTextColor(requireContext().getColor(android.R.color.white))
            }

            TripFilter.PAST -> {
                binding.btnPast.setBackgroundResource(R.drawable.bg_chip_selected)
                binding.btnPast.setTextColor(requireContext().getColor(android.R.color.white))
            }

            TripFilter.CANCELLED -> {
                binding.btnCancelled.setBackgroundResource(R.drawable.bg_chip_selected)
                binding.btnCancelled.setTextColor(requireContext().getColor(android.R.color.white))
            }
        }
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.upcomingTrips.collect {
                        if (currentFilter == TripFilter.UPCOMING) {
                            updateTripsList()
                        }
                    }
                }

                launch {
                    viewModel.pastTrips.collect {
                        if (currentFilter == TripFilter.PAST) {
                            updateTripsList()
                        }
                    }
                }

                launch {
                    viewModel.cancelledTrips.collect {
                        if (currentFilter == TripFilter.CANCELLED) {
                            updateTripsList()
                        }
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
            }
        }
    }

    private fun updateTripsList() {
        val trips = when (currentFilter) {
            TripFilter.UPCOMING -> viewModel.upcomingTrips.value
            TripFilter.PAST -> viewModel.pastTrips.value
            TripFilter.CANCELLED -> viewModel.cancelledTrips.value
        }

        adapter.submitList(trips)

        // Show/hide empty state
        binding.layoutEmptyState.visibility =
            if (trips.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerTrips.visibility = if (trips.isEmpty()) View.GONE else View.VISIBLE

        // Update empty message based on filter
        binding.tvEmpty.text = when (currentFilter) {
            TripFilter.UPCOMING -> getString(R.string.trips_empty_upcoming)
            TripFilter.PAST -> getString(R.string.trips_empty_past)
            TripFilter.CANCELLED -> getString(R.string.trips_empty_cancelled)
        }
    }

    private fun showCancelConfirmationDialog(reservationId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_cancel_reservation_title))
            .setMessage(getString(R.string.dialog_cancel_reservation_message))
            .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                viewModel.cancelReservation(reservationId)
            }
            .setNegativeButton(getString(R.string.dialog_no), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private enum class TripFilter {
        UPCOMING,
        PAST,
        CANCELLED
    }
}
