package com.airbnb.ui.hosting

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.airbnb.databinding.FragmentCreateListingBinding
import kotlinx.coroutines.launch

class CreateListingFragment : Fragment(R.layout.fragment_create_listing) {

    private var _binding: FragmentCreateListingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateListingViewModel by viewModels()
    private var listingId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateListingBinding.bind(view)

        // Check if editing existing listing
        listingId = arguments?.getString("listingId")
        
        setupToolbar()
        setupPropertyTypeSpinner()
        setupGuestCounters()
        setupSaveButton()
        observeState()

        // Load existing listing if editing
        listingId?.let { viewModel.loadListing(it) }
    }

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = if (listingId == null) "Create Listing" else "Edit Listing"
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun setupPropertyTypeSpinner() {
        val propertyTypes = listOf(
            "Entire home",
            "Private room",
            "Shared room",
            "Hotel room"
        )
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            propertyTypes
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPropertyType.adapter = adapter
    }

    private fun setupGuestCounters() {
        // Max Guests
        binding.btnDecreaseGuests.setOnClickListener {
            val current = binding.tvGuestCount.text.toString().toIntOrNull() ?: 1
            if (current > 1) {
                binding.tvGuestCount.text = (current - 1).toString()
            }
        }
        binding.btnIncreaseGuests.setOnClickListener {
            val current = binding.tvGuestCount.text.toString().toIntOrNull() ?: 1
            if (current < 20) {
                binding.tvGuestCount.text = (current + 1).toString()
            }
        }

        // Bedrooms
        binding.btnDecreaseBedrooms.setOnClickListener {
            val current = binding.tvBedroomCount.text.toString().toIntOrNull() ?: 1
            if (current > 1) {
                binding.tvBedroomCount.text = (current - 1).toString()
            }
        }
        binding.btnIncreaseBedrooms.setOnClickListener {
            val current = binding.tvBedroomCount.text.toString().toIntOrNull() ?: 1
            if (current < 20) {
                binding.tvBedroomCount.text = (current + 1).toString()
            }
        }

        // Bathrooms
        binding.btnDecreaseBathrooms.setOnClickListener {
            val current = binding.tvBathroomCount.text.toString().toIntOrNull() ?: 1
            if (current > 1) {
                binding.tvBathroomCount.text = (current - 1).toString()
            }
        }
        binding.btnIncreaseBathrooms.setOnClickListener {
            val current = binding.tvBathroomCount.text.toString().toIntOrNull() ?: 1
            if (current < 20) {
                binding.tvBathroomCount.text = (current + 1).toString()
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveListing()
        }
    }

    private fun saveListing() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val propertyType = binding.spinnerPropertyType.selectedItem.toString()
        val priceText = binding.etPrice.text.toString().trim()
        val maxGuests = binding.tvGuestCount.text.toString().toIntOrNull() ?: 1
        val bedrooms = binding.tvBedroomCount.text.toString().toIntOrNull() ?: 1
        val bathrooms = binding.tvBathroomCount.text.toString().toIntOrNull() ?: 1
        val imageUrl = binding.etImageUrl.text.toString().trim()

        val price = priceText.toDoubleOrNull()
        if (price == null) {
            Toast.makeText(requireContext(), "Invalid price", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse amenities (comma-separated)
        val amenitiesText = binding.etAmenities.text.toString().trim()
        val amenities = if (amenitiesText.isNotBlank()) {
            amenitiesText.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            emptyList()
        }

        viewModel.saveListing(
            listingId = listingId,
            title = title,
            description = description,
            location = location,
            propertyType = propertyType,
            pricePerNight = price,
            maxGuests = maxGuests,
            bedrooms = bedrooms,
            bathrooms = bathrooms,
            amenities = amenities,
            imageUrl = imageUrl
        )
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    // Loading
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.scrollView.visibility = if (state.isLoading) View.GONE else View.VISIBLE

                    // Saving
                    binding.btnSave.isEnabled = !state.isSaving
                    binding.btnSave.text = if (state.isSaving) "Saving..." else "Save Listing"

                    // Populate form if editing
                    state.listing?.let { listing ->
                        binding.etTitle.setText(listing.title)
                        binding.etDescription.setText(listing.description)
                        binding.etLocation.setText(listing.location)
                        binding.etPrice.setText(listing.pricePerNight.toInt().toString())
                        binding.tvGuestCount.text = listing.maxGuests.toString()
                        binding.tvBedroomCount.text = listing.bedrooms.toString()
                        binding.tvBathroomCount.text = listing.bathrooms.toString()
                        binding.etImageUrl.setText(listing.imageUrl)
                        binding.etAmenities.setText(listing.amenities.joinToString(", "))

                        // Set property type spinner
                        val propertyTypes = listOf("Entire home", "Private room", "Shared room", "Hotel room")
                        val index = propertyTypes.indexOf(listing.propertyType)
                        if (index >= 0) {
                            binding.spinnerPropertyType.setSelection(index)
                        }
                    }

                    // Success
                    if (state.success) {
                        Toast.makeText(
                            requireContext(),
                            if (listingId == null) "Listing created!" else "Listing updated!",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }

                    // Error
                    state.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
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
