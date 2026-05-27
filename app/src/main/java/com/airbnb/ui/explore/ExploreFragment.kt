package com.airbnb.ui.explore

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.R
import com.airbnb.core.auth.AuthState
import com.airbnb.data.session.UserSessionManager
import com.airbnb.databinding.FragmentExploreBinding
import com.airbnb.ui.explore.adapter.ListingAdapter
import kotlinx.coroutines.launch

class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExploreViewModel by viewModels()

    private val adapter = ListingAdapter { listing ->

        val bundle = Bundle().apply {
            putString(ARG_LISTING_ID, listing.id)
        }

        try {
            findNavController().navigate(
                R.id.action_exploreFragment_to_listingDetailFragment,
                bundle
            )
        } catch (e: IllegalArgumentException) {
            // Navigation action not yet defined - placeholder
            Toast.makeText(requireContext(), "Listing details coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentExploreBinding.bind(view)

        setupTopBar()
        setupRecycler()
        setupSearch()
        setupFilters()
        observeUi()
    }

    private fun setupTopBar() {

        binding.btnNotifications.setOnClickListener {

            findNavController().navigate(
                R.id.notificationsFragment
            )
        }

        binding.btnSettings.setOnClickListener {

            findNavController().navigate(
                R.id.settingsFragment
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                UserSessionManager.authState.collect { state ->

                    val visible =
                        if (state is AuthState.Guest)
                            View.GONE
                        else
                            View.VISIBLE

                    binding.btnNotifications.visibility = visible
                    binding.btnSettings.visibility = visible
                }
            }
        }
    }

    private fun setupRecycler() {

        binding.recyclerHikes.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerHikes.adapter = adapter
    }

    private fun setupSearch() {

        binding.etSearch.doAfterTextChanged { text ->

            viewModel.setSearchQuery(
                text?.toString().orEmpty()
            )
        }
    }

    private fun setupFilters() {

        // ── Filter panel toggle ───────────────────────────────
        binding.btnToggleFilters.setOnClickListener {

            val panel = binding.layoutFilters
            val chevron = binding.ivFilterChevron

            if (panel.visibility == View.GONE) {

                panel.visibility = View.VISIBLE
                chevron.rotation = 270f

            } else {

                panel.visibility = View.GONE
                chevron.rotation = 90f
            }
        }

        // ── Clear filters ─────────────────────────────────────
        binding.tvClearFilters.setOnClickListener {

            binding.etMaxPrice.text?.clear()
            binding.etMinGuests.text?.clear()

            viewModel.setMaxPrice(null)
            viewModel.setMinGuests(null)
        }

        // ── Numeric filters ───────────────────────────────────
        binding.etMaxPrice.doAfterTextChanged {

            viewModel.setMaxPrice(
                it?.toString()?.toDoubleOrNull()
            )
        }

        binding.etMinGuests.doAfterTextChanged {

            viewModel.setMinGuests(
                it?.toString()?.toIntOrNull()
            )
        }
    }

    private fun observeUi() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                launch {

                    viewModel.displayListings.collect { listings ->

                        adapter.submitList(listings)

                        binding.tvEmpty.visibility =
                            if (listings.isEmpty())
                                View.VISIBLE
                            else
                                View.GONE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        // Listing-related constant (Phase 2+)
        const val ARG_LISTING_ID = "listingId"
        
        // Hiking-related constant (backward compatibility)
        const val ARG_HIKE_ID = "hikeId"
    }
}