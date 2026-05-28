package com.airbnb.ui.listings

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
import com.airbnb.R
import com.airbnb.databinding.FragmentListingDetailsBinding
import com.airbnb.ui.adapter.ReviewsAdapter
import com.airbnb.ui.auth.GuestPromptDialog
import com.airbnb.ui.auth.isUserAuthenticated
import kotlinx.coroutines.launch
import com.airbnb.core.ui.AvatarHelper

class ListingDetailFragment : Fragment(R.layout.fragment_listing_details) {

    private var _binding: FragmentListingDetailsBinding? = null
    private val binding get() = _binding!!

    private val reviewsAdapter = ReviewsAdapter()

    private val listingId: String by lazy {
        arguments?.getString(ARG_LISTING_ID).orEmpty()
    }

    private val viewModel: ListingDetailViewModel by viewModels {
        ListingDetailViewModel.Factory(listingId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentListingDetailsBinding.bind(view)

        setupUI()
        setupReviewsRecyclerView()
        observeViewModel()
    }

    private fun setupReviewsRecyclerView() {
        binding.reviewsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewsAdapter
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnReserve.setOnClickListener {
            if (!isUserAuthenticated()) {
                val listingId = arguments?.getString(ARG_LISTING_ID)
                val bundle = Bundle().apply {
                    putString("listingId", listingId)
                }
                GuestPromptDialog.show(
                    childFragmentManager,
                    destId = R.id.createReservationFragment,
                    destArgs = bundle,
                    onSuccess = {
                        navigateToReservation()
                    }
                )
                return@setOnClickListener
            }
            navigateToReservation()
        }
    }

    private fun navigateToReservation() {
        val listingId = arguments?.getString(ARG_LISTING_ID)
        if (listingId != null) {
            val bundle = Bundle().apply {
                putString("listingId", listingId)
            }
            try {
                findNavController().navigate(
                    R.id.action_listingDetailFragment_to_createReservationFragment,
                    bundle
                )
            } catch (e: IllegalArgumentException) {
                Toast.makeText(requireContext(), getString(R.string.toast_navigation_not_configured), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                // Observe error state
                launch {
                    viewModel.error.collect { error ->
                        if (error != null) {
                            binding.tvError.text = error
                            binding.tvError.visibility = View.VISIBLE
                            binding.contentContainer.visibility = View.GONE
                            binding.bottomReserveSection.visibility = View.GONE
                        } else {
                            binding.tvError.visibility = View.GONE
                        }
                    }
                }

                // Observe listing data
                launch {
                    viewModel.listing.collect { listing ->
                        if (listing != null) {
                            binding.contentContainer.visibility = View.VISIBLE
                            binding.bottomReserveSection.visibility = View.VISIBLE

                            // Basic info
                            binding.tvTitle.text = listing.title
                            binding.tvLocation.text = listing.location
                            binding.tvPropertyType.text = listing.propertyType.ifBlank { "Property" }

                            // Room details
                            binding.tvGuests.text = listing.guestSummary()
                            binding.tvBedrooms.text = when (listing.bedrooms) {
                                1 -> "1 bedroom"
                                else -> "${listing.bedrooms} bedrooms"
                            }
                            binding.tvBathrooms.text = when (listing.bathrooms) {
                                1 -> "1 bathroom"
                                else -> "${listing.bathrooms} bathrooms"
                            }

                            // Host info
                            binding.tvHostName.text =
                                listing.hostName.ifBlank { "Host" }

                            try {

                                AvatarHelper.bind(
                                    imgView = binding.imgHostAvatar,
                                    tvInitial = null,
                                    name = listing.hostName,
                                    imageUrl = listing.hostProfileImage
                                )

                            } catch (e: Exception) {

                                binding.imgHostAvatar.setImageResource(
                                    R.drawable.ic_profile
                                )
                            }

                            // Description
                            binding.tvDescription.text = listing.description.ifBlank { "No description available" }

                            // Amenities
                            if (listing.amenities.isNotEmpty()) {
                                binding.tvAmenities.text = listing.amenities.joinToString("\n") { "• $it" }
                            } else {
                                binding.tvAmenities.text = "No amenities listed"
                            }

                            // Price using centralized formatter
                            binding.tvPrice.text = listing.formattedPrice()

                            // Rating summary
                            binding.tvRatingSummary.text = listing.formattedRating()

                            // TODO: Load image from listing.imageUrl using Glide/Coil in future
                        }
                    }
                }

                // Observe reviews
                launch {
                    viewModel.reviews.collect { reviews ->
                        if (reviews.isNotEmpty()) {
                            binding.reviewsSection.visibility = View.VISIBLE
                            binding.reviewsDivider.visibility = View.VISIBLE
                            reviewsAdapter.submitList(reviews.take(5)) // Show max 5 reviews
                        } else {
                            binding.reviewsSection.visibility = View.GONE
                            binding.reviewsDivider.visibility = View.GONE
                        }
                    }
                }

                // Observe toast messages
                launch {
                    viewModel.toast.collect { message ->
                        if (!message.isNullOrBlank()) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            viewModel.consumeToast()
                        }
                    }
                }

                // Observe if this is user's own listing
                launch {
                    viewModel.isOwnListing.collect { isOwnListing ->
                        if (isOwnListing) {
                            binding.bottomReserveSection.visibility = View.GONE
                            binding.tvOwnListingMessage.visibility = View.VISIBLE
                        } else {
                            binding.bottomReserveSection.visibility = View.VISIBLE
                            binding.tvOwnListingMessage.visibility = View.GONE
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
        const val ARG_LISTING_ID = "listingId"
    }
}
