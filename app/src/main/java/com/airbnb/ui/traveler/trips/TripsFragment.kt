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
import com.airbnb.ui.auth.GuestPromptDialog
import com.airbnb.ui.auth.isUserAuthenticated
import com.airbnb.ui.traveler.trips.adapter.TripAdapter
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

        // Force sign in for guests
        if (!isUserAuthenticated()) {

            binding.recyclerTrips.visibility = View.GONE
            binding.layoutFilters.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE

            return
        }

        setupAdapter()
        setupRecycler()
        setupFilters()
        observeUi()
    }

    override fun onResume() {
        super.onResume()

        if (!isUserAuthenticated()) {

            val existing =
                parentFragmentManager.findFragmentByTag(
                    GuestPromptDialog.TAG
                )

            if (existing == null) {

                GuestPromptDialog.show(
                    parentFragmentManager
                )
            }

            return
        }

        updateTripsList()
    }

    private fun showGuestPromptDialog() {

        val dialog = GuestPromptDialog()

        dialog.show(
            parentFragmentManager,
            "GuestPromptDialog"
        )
    }

    private fun setupAdapter() {

        adapter = TripAdapter(

            onItemClick = { tripItem ->

                val bundle = Bundle().apply {
                    putString("reservationId", tripItem.reservation.id)
                }

                try {

                    findNavController().navigate(
                        R.id.action_tripsFragment_to_tripDetailsFragment,
                        bundle
                    )

                } catch (e: IllegalArgumentException) {

                    val listingBundle = Bundle().apply {
                        putString("listingId", tripItem.reservation.listingId)
                    }

                    try {

                        findNavController().navigate(
                            R.id.action_tripsFragment_to_listingDetailFragment,
                            listingBundle
                        )

                    } catch (e2: IllegalArgumentException) {

                        Toast.makeText(
                            requireContext(),
                            "Trip details coming soon",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },

            onCancelClick = { tripItem ->
                showCancelConfirmationDialog(tripItem.reservation.id)
            }
        )
    }

    private fun setupRecycler() {

        binding.recyclerTrips.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerTrips.adapter = adapter
    }

    private fun setupFilters() {

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

        binding.btnUpcoming.setTextColor(
            requireContext().getColor(R.color.black)
        )

        binding.btnPast.setTextColor(
            requireContext().getColor(R.color.black)
        )

        binding.btnCancelled.setTextColor(
            requireContext().getColor(R.color.black)
        )

        when (currentFilter) {

            TripFilter.UPCOMING -> {

                binding.btnUpcoming.setBackgroundResource(
                    R.drawable.bg_chip_selected
                )

                binding.btnUpcoming.setTextColor(
                    requireContext().getColor(android.R.color.white)
                )
            }

            TripFilter.ACTIVE_STAY -> {

                binding.btnUpcoming.setBackgroundResource(
                    R.drawable.bg_chip_selected
                )

                binding.btnUpcoming.setTextColor(
                    requireContext().getColor(android.R.color.white)
                )
            }

            TripFilter.PAST -> {

                binding.btnPast.setBackgroundResource(
                    R.drawable.bg_chip_selected
                )

                binding.btnPast.setTextColor(
                    requireContext().getColor(android.R.color.white)
                )
            }

            TripFilter.CANCELLED -> {

                binding.btnCancelled.setBackgroundResource(
                    R.drawable.bg_chip_selected
                )

                binding.btnCancelled.setTextColor(
                    requireContext().getColor(android.R.color.white)
                )
            }
        }
    }

    private fun observeUi() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                launch {

                    viewModel.upcomingTrips.collect {

                        if (currentFilter == TripFilter.UPCOMING) {
                            updateTripsList()
                        }
                    }
                }

                launch {

                    viewModel.activeStayTrips.collect {

                        if (currentFilter == TripFilter.ACTIVE_STAY) {
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

                        binding.progressBar.visibility =
                            if (isLoading) View.VISIBLE else View.GONE

                        if (isLoading) {

                            // Hide everything during loading
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.recyclerTrips.visibility = View.GONE

                        } else {

                            // Restore proper state after loading finishes
                            updateTripsList()
                        }
                    }
                }

                launch {

                    viewModel.toast.collect { msg ->

                        if (!msg.isNullOrBlank()) {

                            Toast.makeText(
                                requireContext(),
                                msg,
                                Toast.LENGTH_SHORT
                            ).show()

                            viewModel.consumeToast()
                        }
                    }
                }
            }
        }
    }

    private fun updateTripsList() {

        // Prevent empty state showing during loading
        if (viewModel.isLoading.value) return

        val trips = when (currentFilter) {

            TripFilter.UPCOMING ->
                viewModel.upcomingTrips.value

            TripFilter.ACTIVE_STAY ->
                viewModel.activeStayTrips.value

            TripFilter.PAST ->
                viewModel.pastTrips.value

            TripFilter.CANCELLED ->
                viewModel.cancelledTrips.value
        }

        adapter.submitList(trips)

        val isEmpty = trips.isEmpty()

        binding.layoutEmptyState.visibility =
            if (isEmpty) View.VISIBLE else View.GONE

        binding.recyclerTrips.visibility =
            if (isEmpty) View.GONE else View.VISIBLE

    }

    private fun showCancelConfirmationDialog(
        reservationId: String
    ) {

        AlertDialog.Builder(requireContext())
            .setTitle(
                getString(R.string.dialog_cancel_reservation_title)
            )
            .setMessage(
                getString(R.string.dialog_cancel_reservation_message)
            )
            .setPositiveButton(
                getString(R.string.dialog_yes)
            ) { _, _ ->

                viewModel.cancelReservation(reservationId)
            }
            .setNegativeButton(
                getString(R.string.dialog_no),
                null
            )
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private enum class TripFilter {
        UPCOMING,
        ACTIVE_STAY,
        PAST,
        CANCELLED
    }
}