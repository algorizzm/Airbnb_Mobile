package com.airbnb.ui.hosting

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
import com.airbnb.databinding.FragmentHostReservationsBinding
import kotlinx.coroutines.launch

class HostReservationsFragment : Fragment(R.layout.fragment_host_reservations) {

    private var _binding: FragmentHostReservationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HostReservationsViewModel by viewModels()
    private lateinit var adapter: HostReservationAdapter

    private var listingId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHostReservationsBinding.bind(view)

        listingId = arguments?.getString("listingId")
        if (listingId == null) {
            Toast.makeText(requireContext(), "Invalid listing", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        setupToolbar()
        setupRecyclerView()
        observeState()

        viewModel.loadReservationsForListing(listingId!!)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = HostReservationAdapter(
            onCancelClick = { reservation ->
                showCancelConfirmation(reservation.id, reservation.guestName)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HostReservationsFragment.adapter
        }
    }

    private fun showCancelConfirmation(reservationId: String, guestName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Reservation")
            .setMessage("Cancel reservation for $guestName?")
            .setPositiveButton("Cancel Reservation") { _, _ ->
                viewModel.cancelReservation(reservationId)
            }
            .setNegativeButton("Keep", null)
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    // Loading
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // Reservations
                    adapter.submitList(state.reservations)

                    // Empty state
                    if (state.reservations.isEmpty() && !state.isLoading) {
                        binding.emptyState.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE
                    } else {
                        binding.emptyState.visibility = View.GONE
                        binding.recyclerView.visibility = View.VISIBLE
                    }

                    // Error
                    state.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }

                    // Message
                    state.message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
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
