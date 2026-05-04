package com.hikora.ui.hikes

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hikora.R
import com.hikora.databinding.FragmentMyBookingsBinding
import com.hikora.ui.hikes.adapter.UserBookingsAdapter
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
                    adapter.submitList(state.rows)
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
