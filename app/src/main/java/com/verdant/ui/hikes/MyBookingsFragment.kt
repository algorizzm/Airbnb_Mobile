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
import com.verdant.databinding.FragmentMyBookingsBinding
import com.verdant.ui.hikes.adapter.UserBookingsAdapter
import kotlinx.coroutines.launch

class MyBookingsFragment : Fragment(R.layout.fragment_my_bookings) {

    private var _binding: FragmentMyBookingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookingsViewModel by viewModels()

    private val adapter = UserBookingsAdapter { row ->
        viewModel.cancelBooking(row.booking)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMyBookingsBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.recyclerBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerBookings.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressLoading.visibility = if (state.loading) View.VISIBLE else View.GONE

                    val isEmpty = !state.loading && state.rows.isEmpty()
                    binding.tvStatus.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    if (isEmpty) {
                        binding.tvStatus.text = when (state.authState) {
                            AuthState.Guest -> "Please log in to view your bookings."
                            is AuthState.Loading -> "Loading your account…"
                            is AuthState.Authenticated -> {
                                if (state.authState.user == null) {
                                    "Finishing account setup…"
                                } else {
                                    "No bookings yet."
                                }
                            }
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
