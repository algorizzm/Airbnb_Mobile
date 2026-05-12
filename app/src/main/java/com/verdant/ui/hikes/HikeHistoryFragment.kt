package com.verdant.ui.hikes

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.verdant.R
import com.verdant.core.auth.AuthState
import com.verdant.core.ui.toast
import com.verdant.databinding.FragmentHistoryBinding
import com.verdant.ui.explore.ExploreFragment
import com.verdant.ui.hikes.adapter.UserBookingsAdapter
import kotlinx.coroutines.launch

class HikeHistoryFragment : Fragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookingsViewModel by viewModels()

    private val adapter = UserBookingsAdapter(
        onItemClick = { row ->
            // Navigate to hike detail so the user can see full info
            val bundle = Bundle().apply {
                putString(ExploreFragment.ARG_HIKE_ID, row.booking.hikeId)
            }
            findNavController().navigate(R.id.hikeDetailFragment, bundle)
        },
        onCancelOrLeave = { row ->
            viewModel.cancelBooking(row.booking)
        }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)

        binding.recyclerBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerBookings.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    // Loading spinner
                    binding.progressLoading.visibility =
                        if (state.loading) View.VISIBLE else View.GONE

                    when {
                        // Auth/loading message (guest, loading account)
                        !state.loading && state.rows.isEmpty() &&
                                state.authState !is AuthState.Authenticated -> {
                            binding.tvStatus.visibility = View.VISIBLE
                            binding.layoutEmpty.visibility = View.GONE
                            binding.recyclerBookings.visibility = View.GONE
                            binding.tvStatus.text = when (state.authState) {
                                AuthState.Guest -> "Please log in to view your hike history."
                                is AuthState.Loading -> "Loading your account…"
                                else -> ""
                            }
                        }
                        // Authenticated but empty
                        !state.loading && state.rows.isEmpty() -> {
                            binding.tvStatus.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.VISIBLE
                            binding.recyclerBookings.visibility = View.GONE
                        }
                        // Has data
                        state.rows.isNotEmpty() -> {
                            binding.tvStatus.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.GONE
                            binding.recyclerBookings.visibility = View.VISIBLE
                        }
                        else -> {
                            binding.tvStatus.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.GONE
                            binding.recyclerBookings.visibility = View.GONE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
