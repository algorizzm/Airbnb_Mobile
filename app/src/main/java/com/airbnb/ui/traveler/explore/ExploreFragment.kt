package com.airbnb.ui.traveler.explore

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
import com.airbnb.ui.traveler.explore.adapter.ListingAdapter
import com.airbnb.ui.traveler.wishlist.CreateCollectionDialog
import com.airbnb.ui.traveler.wishlist.SelectCollectionDialog
import com.airbnb.ui.auth.GuestPromptDialog
import com.airbnb.ui.auth.isUserAuthenticated
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
                if (!isUserAuthenticated()) {
                    GuestPromptDialog.show(
                        childFragmentManager,
                        onSuccess = {
                            handleWishlistClick(listing.id)
                        }
                    )
                } else {
                    handleWishlistClick(listing.id)
                }
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



    // =====================================================
    // Wishlist Collection Handling
    // =====================================================
    private fun handleWishlistClick(listingId: String) {
        // Check if listing is already in wishlist (sourced from collections, not legacy doc)
        val isInWishlist = viewModel.wishlistIds.value.contains(listingId)

        if (isInWishlist) {
            // Remove from all collections
            viewModel.toggleWishlist(listingId)
        } else {
            // Check how many collections exist to determine UX path
            lifecycleScope.launch {
                val userId = com.airbnb.core.auth.AuthManager.currentUserId() ?: return@launch
                val collectionRepo = com.airbnb.data.repository.WishlistCollectionRepository()

                collectionRepo.getCollections(userId)
                    .onSuccess { collections ->
                        when {
                            collections.isEmpty() -> {
                                // Bug 2 Fix: No collections yet — show first-save dialog
                                // instead of silently auto-creating Favorites.
                                showFirstSaveDialog(listingId)
                            }
                            collections.size == 1 -> {
                                // Single collection — auto-save with no friction
                                viewModel.toggleWishlist(listingId)
                            }
                            else -> {
                                // Multiple collections — let user pick
                                showCollectionSelectionDialog(listingId)
                            }
                        }
                    }
                    .onFailure {
                        // Fallback to default behavior
                        viewModel.toggleWishlist(listingId)
                    }
            }
        }
    }

    /**
     * Bug 2 Fix: Shows a lightweight premium Bottom Sheet dialog for the very first save action.
     * The user can choose to save directly to Favorites (auto-created)
     * or create a custom collection first. Matches Airbnb's premium bottom sheet design.
     */
    private fun showFirstSaveDialog(listingId: String) {
        val firstSaveDialog = com.airbnb.ui.traveler.wishlist.FirstSaveDialog.newInstance()
        firstSaveDialog.setOnSaveToFavoritesListener {
            // Auto-create Favorites and save — zero friction path
            viewModel.toggleWishlist(listingId)
        }
        firstSaveDialog.setOnCreateCollectionListener {
            // Open the create collection dialog; after creation, save to it
            val createDialog = CreateCollectionDialog()
            createDialog.setOnCollectionCreated { collection ->
                viewModel.addToCollection(listingId, collection.id)
            }
            createDialog.show(childFragmentManager, "CreateCollectionDialog")
        }
        firstSaveDialog.show(childFragmentManager, "FirstSaveDialog")
    }

    private fun showCollectionSelectionDialog(listingId: String) {
        val dialog = SelectCollectionDialog.newInstance(listingId)
        dialog.setOnCollectionSelected { collectionId ->
            viewModel.addToCollection(listingId, collectionId)
        }
        dialog.show(childFragmentManager, "SelectCollectionDialog")
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