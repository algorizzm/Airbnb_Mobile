package com.airbnb.ui.host.today

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.R
import com.airbnb.data.model.Reservation
import com.airbnb.databinding.FragmentHostTodayBinding
import com.airbnb.ui.host.today.adapter.TodayActiveStayAdapter
import com.airbnb.ui.host.today.adapter.TodayCheckInAdapter
import com.airbnb.ui.host.today.adapter.TodayCheckOutAdapter
import com.airbnb.ui.host.today.adapter.TodayUpcomingAdapter
import kotlinx.coroutines.launch

/**
 * Host Today tab — dynamic reservation activity dashboard.
 *
 * Displays reservations grouped into sections:
 * - Today's Check-Ins
 * - Today's Check-Outs
 * - Active Stays
 * - Upcoming Reservations
 *
 * This is the root destination for the host mode Today tab.
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

    private val viewModel: TodayViewModel by viewModels()

    // =========================================================
    // ADAPTERS
    // =========================================================

    private lateinit var checkInAdapter: TodayCheckInAdapter
    private lateinit var checkOutAdapter: TodayCheckOutAdapter
    private lateinit var activeStayAdapter: TodayActiveStayAdapter
    private lateinit var upcomingAdapter: TodayUpcomingAdapter

    // =========================================================
    // STATE
    // =========================================================

    private var currentFilter: TodayFilter = TodayFilter.TODAY

    // =========================================================
    // LIFECYCLE
    // =========================================================

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHostTodayBinding.bind(view)

        setupAdapters()
        setupRecyclerViews()
        setupFilters()
        setupHeaderButtons()
        observeState()
    }

    private fun setupHeaderButtons() {

        // Quick navigation to Calendar screen
        binding.btnCalendarInfo.setOnClickListener {
            try {
                findNavController().navigate(R.id.hostCalendarFragment)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Opening Calendar...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // =========================================================
    // SETUP
    // =========================================================

    private fun setupAdapters() {
        checkInAdapter = TodayCheckInAdapter(onItemClick = ::navigateToReservationDetail)
        checkOutAdapter = TodayCheckOutAdapter(onItemClick = ::navigateToReservationDetail)
        activeStayAdapter = TodayActiveStayAdapter(onItemClick = ::navigateToReservationDetail)
        upcomingAdapter = TodayUpcomingAdapter(onItemClick = ::navigateToReservationDetail)
    }

    private fun setupRecyclerViews() {
        binding.rvCheckIns.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = checkInAdapter
        }

        binding.rvCheckOuts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = checkOutAdapter
        }

        binding.rvActiveStays.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = activeStayAdapter
        }

        binding.rvUpcoming.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = upcomingAdapter
        }
    }

    private fun setupFilters() {
        updateFilterChips()

        binding.chipToday.setOnClickListener {
            currentFilter = TodayFilter.TODAY
            updateFilterChips()
            updateVisibility()
        }

        binding.chipUpcoming.setOnClickListener {
            currentFilter = TodayFilter.UPCOMING
            updateFilterChips()
            updateVisibility()
        }
    }

    private fun updateFilterChips() {
        // Reset all chips
        binding.chipToday.setBackgroundResource(R.drawable.bg_chip_unselected)
        binding.chipToday.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))

        binding.chipUpcoming.setBackgroundResource(R.drawable.bg_chip_unselected)
        binding.chipUpcoming.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))

        // Highlight selected chip
        when (currentFilter) {
            TodayFilter.TODAY -> {
                binding.chipToday.setBackgroundResource(R.drawable.bg_chip_selected)
                binding.chipToday.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
            TodayFilter.UPCOMING -> {
                binding.chipUpcoming.setBackgroundResource(R.drawable.bg_chip_selected)
                binding.chipUpcoming.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        }
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    private fun navigateToReservationDetail(reservation: Reservation) {
        // Navigate to trip detail screen (reusing existing navigation)
        // This assumes TripDetailsFragment can handle host viewing
        val bundle = Bundle().apply {
            putString("reservationId", reservation.id)
        }
        findNavController().navigate(R.id.action_hostToday_to_tripDetails, bundle)
    }

    // =========================================================
    // OBSERVE STATE
    // =========================================================

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: TodayUiState) {
        // Loading
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        // Update adapters
        checkInAdapter.submitList(state.todaysCheckIns)
        checkOutAdapter.submitList(state.todaysCheckOuts)
        activeStayAdapter.submitList(state.activeStays)
        upcomingAdapter.submitList(state.upcomingReservations)

        // Update visibility based on filter
        updateVisibility()

        // Error
        state.error?.let {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    private fun updateVisibility() {
        val state = viewModel.state.value

        when (currentFilter) {
            TodayFilter.TODAY -> {
                // Show today's sections
                binding.sectionCheckIns.visibility = if (state.todaysCheckIns.isNotEmpty()) View.VISIBLE else View.GONE
                binding.sectionCheckOuts.visibility = if (state.todaysCheckOuts.isNotEmpty()) View.VISIBLE else View.GONE
                binding.sectionActiveStays.visibility = if (state.activeStays.isNotEmpty()) View.VISIBLE else View.GONE
                binding.sectionUpcoming.visibility = View.GONE

                // Empty state for today
                val isTodayEmpty = state.todaysCheckIns.isEmpty() && 
                                   state.todaysCheckOuts.isEmpty() && 
                                   state.activeStays.isEmpty()

                if (isTodayEmpty && !state.isLoading) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.scrollView.visibility = View.GONE
                    binding.tvEmptyTitle.text = "No activity today"
                    binding.tvEmptySubtitle.text = "Check-ins, check-outs, and active stays will appear here."
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.scrollView.visibility = View.VISIBLE
                }
            }

            TodayFilter.UPCOMING -> {
                // Show only upcoming section
                binding.sectionCheckIns.visibility = View.GONE
                binding.sectionCheckOuts.visibility = View.GONE
                binding.sectionActiveStays.visibility = View.GONE
                binding.sectionUpcoming.visibility = if (state.upcomingReservations.isNotEmpty()) View.VISIBLE else View.GONE

                // Empty state for upcoming
                if (state.upcomingReservations.isEmpty() && !state.isLoading) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.scrollView.visibility = View.GONE
                    binding.tvEmptyTitle.text = "No upcoming reservations"
                    binding.tvEmptySubtitle.text = "Future reservations will appear here."
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.scrollView.visibility = View.VISIBLE
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

    // =========================================================
    // FILTER ENUM
    // =========================================================

    private enum class TodayFilter {
        TODAY,
        UPCOMING
    }
}
