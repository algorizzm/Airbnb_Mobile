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
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.R
import com.airbnb.databinding.FragmentHostTodayBinding
import kotlinx.coroutines.launch

/**
 * Host Today tab — shows all reservations across all of the host's listings.
 *
 * This is the root destination for the host mode Today tab.
 * It reuses HostReservationsViewModel and HostReservationAdapter, calling
 * loadAllReservationsForHost() instead of the per-listing variant.
 *
 * No toolbar back button — this is a root tab, not a drill-down screen.
 */
class HostTodayFragment : Fragment(R.layout.fragment_host_today) {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding: FragmentHostTodayBinding? = null
    private val binding get() = _binding!!

    // =========================================================
    // VIEWMODEL
    // =========================================================

    private val viewModel: HostReservationsViewModel by viewModels()

    // =========================================================
    // ADAPTER
    // =========================================================

    private lateinit var adapter: HostReservationAdapter

    // =========================================================
    // LIFECYCLE
    // =========================================================

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHostTodayBinding.bind(view)

        setupRecyclerView()
        observeState()

        // Load all reservations for this host (cross-listing)
        viewModel.loadAllReservationsForHost()
    }

    // =========================================================
    // RECYCLER VIEW
    // =========================================================

    private fun setupRecyclerView() {
        adapter = HostReservationAdapter(
            onApproveClick = { reservation ->
                showApproveConfirmation(reservation.id, reservation.guestName)
            },
            onRejectClick = { reservation ->
                showRejectConfirmation(reservation.id, reservation.guestName)
            },
            onCancelClick = { reservation ->
                showCancelConfirmation(reservation.id, reservation.guestName)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HostTodayFragment.adapter
        }
    }

    // =========================================================
    // CONFIRMATIONS
    // =========================================================

    private fun showApproveConfirmation(reservationId: String, guestName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_approve_reservation_title))
            .setMessage(getString(R.string.dialog_approve_reservation_message, guestName))
            .setPositiveButton(getString(R.string.dialog_approve)) { _, _ ->
                viewModel.approveReservation(reservationId)
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun showRejectConfirmation(reservationId: String, guestName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_reject_reservation_title))
            .setMessage(getString(R.string.dialog_reject_reservation_message, guestName))
            .setPositiveButton(getString(R.string.dialog_decline)) { _, _ ->
                viewModel.rejectReservation(reservationId)
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun showCancelConfirmation(reservationId: String, guestName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_cancel_reservation_title))
            .setMessage(getString(R.string.dialog_cancel_reservation_for_guest_message, guestName))
            .setPositiveButton(getString(R.string.dialog_cancel_reservation_confirm)) { _, _ ->
                viewModel.cancelReservation(reservationId)
            }
            .setNegativeButton(getString(R.string.dialog_keep_reservation), null)
            .show()
    }

    // =========================================================
    // OBSERVE
    // =========================================================

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->

                    // Loading
                    binding.progressBar.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

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

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
