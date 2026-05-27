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
import com.airbnb.databinding.FragmentHostListingsBinding
import kotlinx.coroutines.launch

class HostListingsFragment : Fragment(R.layout.fragment_host_listings) {

    private var _binding: FragmentHostListingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HostListingsViewModel by viewModels()
    private lateinit var adapter: HostListingAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHostListingsBinding.bind(view)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = HostListingAdapter(
            onEditClick = { listing ->
                val bundle = Bundle().apply {
                    putString("listingId", listing.id)
                }
                findNavController().navigate(
                    R.id.action_hostListingsFragment_to_createListingFragment,
                    bundle
                )
            },
            onDeleteClick = { listing ->
                showDeleteConfirmation(listing.id, listing.title)
            },
            onViewReservationsClick = { listing ->
                val bundle = Bundle().apply {
                    putString("listingId", listing.id)
                }
                findNavController().navigate(
                    R.id.action_hostListingsFragment_to_hostReservationsFragment,
                    bundle
                )
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HostListingsFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabCreateListing.setOnClickListener {
            findNavController().navigate(R.id.action_hostListingsFragment_to_createListingFragment)
        }
    }

    private fun showDeleteConfirmation(listingId: String, title: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Listing")
            .setMessage("Are you sure you want to delete \"$title\"? This will also cancel all associated reservations.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteListing(listingId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    // Loading
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // Listings
                    adapter.submitList(state.listings)

                    // Empty state
                    if (state.listings.isEmpty() && !state.isLoading) {
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
