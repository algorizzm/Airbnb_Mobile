package com.airbnb.ui.explore

import android.graphics.Rect
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
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.R
import com.airbnb.databinding.FragmentExploreBinding
import com.airbnb.ui.explore.adapter.ListingAdapter
import kotlinx.coroutines.launch

class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExploreViewModel by viewModels()

    private lateinit var adapter: ListingAdapter

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentExploreBinding.bind(view)

        setupAdapter()
        setupRecycler()
        setupSearch()
        setupFilters()
        observeUi()
    }

    // =====================================================
    // Adapter
    // =====================================================
    private fun setupAdapter() {

        adapter = ListingAdapter(

            onItemClick = { listing ->

                val bundle = Bundle().apply {
                    putString(ARG_LISTING_ID, listing.id)
                }

                try {

                    findNavController().navigate(
                        R.id.action_exploreFragment_to_listingDetailFragment,
                        bundle
                    )

                } catch (e: IllegalArgumentException) {

                    Toast.makeText(
                        requireContext(),
                        "Listing details coming soon",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },

            onWishlistClick = { listing ->

                viewModel.toggleWishlist(
                    listing.id
                )
            }
        )
    }

    private fun setupRecycler() {

        binding.recyclerHikes.layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )

        binding.recyclerHikes.adapter = adapter

        // Space between cards
        binding.recyclerHikes.addItemDecoration(
            object : RecyclerView.ItemDecoration() {

                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {

                    outRect.right = 8
                }
            }
        )
    }

    // =====================================================
    // Search
    // =====================================================
    private fun setupSearch() {

        binding.etSearch.doAfterTextChanged { text ->

            viewModel.setSearchQuery(
                text?.toString().orEmpty()
            )
        }
    }

    // =====================================================
    // Filters
    // =====================================================
    private fun setupFilters() {

        // Toggle filters panel
        binding.btnToggleFilters.setOnClickListener {

            val panel = binding.layoutFilters
            val chevron = binding.ivFilterChevron

            if (panel.visibility == View.GONE) {

                panel.visibility = View.VISIBLE

                chevron.animate()
                    .rotation(270f)
                    .setDuration(180)
                    .start()

            } else {

                panel.visibility = View.GONE

                chevron.animate()
                    .rotation(90f)
                    .setDuration(180)
                    .start()
            }
        }

        // Clear filters
        binding.tvClearFilters.setOnClickListener {

            binding.etMaxPrice.text?.clear()
            binding.etMinGuests.text?.clear()

            viewModel.setMaxPrice(null)
            viewModel.setMinGuests(null)
        }

        // Max price filter
        binding.etMaxPrice.doAfterTextChanged {

            viewModel.setMaxPrice(
                it?.toString()?.toDoubleOrNull()
            )
        }

        // Guest filter
        binding.etMinGuests.doAfterTextChanged {

            viewModel.setMinGuests(
                it?.toString()?.toIntOrNull()
            )
        }
    }

    // =====================================================
    // Observe UI State
    // =====================================================
    private fun observeUi() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                // Listings
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

                // Wishlist state — update in-place, no adapter recreation
                launch {
                    viewModel.wishlistIds.collect { wishlistIds ->
                        adapter.updateWishlistIds(wishlistIds)
                    }
                }

                // Dynamic Title based on search query
                launch {
                    viewModel.searchQuery.collect { query ->
                        val trimmed = query.trim()
                        binding.tvSectionTitle.text = if (trimmed.isNotEmpty()) {
                            "Stays in \"$trimmed\""
                        } else {
                            "Explore stays"
                        }
                    }
                }

                // Toast messages
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

        // Listing ID
        const val ARG_LISTING_ID = "listingId"

        // Backward compatibility
        const val ARG_HIKE_ID = "hikeId"
    }
}