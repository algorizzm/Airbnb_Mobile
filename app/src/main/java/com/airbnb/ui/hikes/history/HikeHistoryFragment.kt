package com.airbnb.ui.hikes.history

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.R
import com.airbnb.core.auth.AuthState
import com.airbnb.core.ui.toast
import com.airbnb.databinding.FragmentHistoryBinding
import com.airbnb.ui.explore.ExploreFragment
import com.airbnb.ui.hikes.HikesViewModel
import com.airbnb.ui.hikes.adapter.UserBookingsAdapter
import kotlinx.coroutines.launch

class HikeHistoryFragment : Fragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookingsViewModel by viewModels()

    // Used for isGuide logic
    private val hikesViewModel: HikesViewModel by viewModels()

    private val adapter = UserBookingsAdapter(

        onItemClick = { row ->

            val bundle = Bundle().apply {

                putString(
                    ExploreFragment.ARG_HIKE_ID,
                    row.booking.hikeId
                )
            }

            val currentDestId = findNavController().currentDestination?.id
            val actionId = if (currentDestId == R.id.myBookingsFragment) {
                R.id.action_myBookingsFragment_to_hikeDetailFragment
            } else {
                R.id.action_hikeHistoryFragment_to_hikeDetailFragment
            }
            try {
                findNavController().navigate(
                    actionId,
                    bundle
                )
            } catch (e: IllegalArgumentException) {
                // ignore
            }
        },

        onCancelOrLeave = { row ->
            viewModel.cancelBooking(row.booking)
        }
    )

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHistoryBinding.bind(view)

        setupRecycler()
        setupClicks()
        observeUi()
    }

    private fun setupRecycler() {

        binding.recyclerBookings.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerBookings.adapter = adapter
    }

    private fun setupClicks() {

        // Track tab
        binding.tabTrack.setOnClickListener {

            findNavController().navigate(
                R.id.hikeFragment
            )
        }

        // Events tab
        binding.tabEvents.setOnClickListener {

            findNavController().navigate(
                R.id.eventsFragment
            )
        }

        // Notifications
        binding.btnNotifications.setOnClickListener {

            findNavController().navigate(
                R.id.notificationsFragment
            )
        }

        // Settings
        binding.btnSettings.setOnClickListener {

            findNavController().navigate(
                R.id.settingsFragment
            )
        }
    }

    private fun observeUi() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                // Guide visibility logic
                launch {

                    hikesViewModel.isGuide.collect { guide ->

                        binding.tabEvents.visibility =
                            if (guide)
                                View.VISIBLE
                            else
                                View.GONE
                    }
                }

                // Main UI state
                launch {

                    viewModel.uiState.collect { state ->

                        // Loading spinner
                        binding.progressLoading.visibility =
                            if (state.loading)
                                View.VISIBLE
                            else
                                View.GONE

                        when {

                            // Guest/loading
                            !state.loading &&
                                    state.rows.isEmpty() &&
                                    state.authState !is AuthState.Authenticated -> {

                                binding.tvStatus.visibility =
                                    View.VISIBLE

                                binding.layoutEmpty.visibility =
                                    View.GONE

                                binding.recyclerBookings.visibility =
                                    View.GONE

                                binding.tvStatus.text =
                                    when (state.authState) {

                                        AuthState.Guest ->
                                            "Please log in to view your hike history."

                                        is AuthState.Loading ->
                                            "Loading your account…"

                                        else -> ""
                                    }
                            }

                            // Empty
                            !state.loading &&
                                    state.rows.isEmpty() -> {

                                binding.tvStatus.visibility =
                                    View.GONE

                                binding.layoutEmpty.visibility =
                                    View.VISIBLE

                                binding.recyclerBookings.visibility =
                                    View.GONE
                            }

                            // Has data
                            state.rows.isNotEmpty() -> {

                                binding.tvStatus.visibility =
                                    View.GONE

                                binding.layoutEmpty.visibility =
                                    View.GONE

                                binding.recyclerBookings.visibility =
                                    View.VISIBLE
                            }

                            else -> {

                                binding.tvStatus.visibility =
                                    View.GONE

                                binding.layoutEmpty.visibility =
                                    View.GONE

                                binding.recyclerBookings.visibility =
                                    View.GONE
                            }
                        }

                        adapter.submitList(state.rows)

                        state.message?.let {

                            toast(it)

                            viewModel.consumeMessage()
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