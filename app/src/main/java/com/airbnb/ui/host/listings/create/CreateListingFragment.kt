package com.airbnb.ui.host.listings.create

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.R
import com.airbnb.databinding.FragmentCreateListingBinding
import com.airbnb.ui.host.listings.adapter.ImagePreviewAdapter
import com.airbnb.utils.Amenities
import com.airbnb.utils.cloudinary.UploadState
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class CreateListingFragment : Fragment(R.layout.fragment_create_listing) {

    private var _binding: FragmentCreateListingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateListingViewModel by viewModels()
    private var listingId: String? = null

    // ── Image picker (multi-select) ───────────────────────────────────────────

    private val imagePreviewAdapter = ImagePreviewAdapter { uri ->
        viewModel.removeSelectedImage(uri)
    }

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.setSelectedImages(uris)
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateListingBinding.bind(view)

        listingId = arguments?.getString("listingId")

        setupToolbar()
        setupPropertyTypeSpinner()
        setupAmenitiesChips()
        setupGuestCounters()
        setupImagePicker()
        setupSaveButton()
        observeState()

        listingId?.let { viewModel.loadListing(it) }
    }

    // ── Setup helpers ─────────────────────────────────────────────────────────

    private fun setupToolbar() {
        binding.toolbar.apply {
            title = if (listingId == null) "Create Listing" else "Edit Listing"
            setNavigationOnClickListener { findNavController().navigateUp() }
        }
    }

    private fun setupPropertyTypeSpinner() {
        val propertyTypes = listOf(
            "Entire home", "Private room", "Shared room", "Hotel room"
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
            if (current > 1) binding.tvGuestCount.text = (current - 1).toString()
        }
        binding.btnIncreaseGuests.setOnClickListener {
            val current = binding.tvGuestCount.text.toString().toIntOrNull() ?: 1
            if (current < 20) binding.tvGuestCount.text = (current + 1).toString()
        }

        // Bedrooms
        binding.btnDecreaseBedrooms.setOnClickListener {
            val current = binding.tvBedroomCount.text.toString().toIntOrNull() ?: 1
            if (current > 1) binding.tvBedroomCount.text = (current - 1).toString()
        }
        binding.btnIncreaseBedrooms.setOnClickListener {
            val current = binding.tvBedroomCount.text.toString().toIntOrNull() ?: 1
            if (current < 20) binding.tvBedroomCount.text = (current + 1).toString()
        }

        // Bathrooms
        binding.btnDecreaseBathrooms.setOnClickListener {
            val current = binding.tvBathroomCount.text.toString().toIntOrNull() ?: 1
            if (current > 1) binding.tvBathroomCount.text = (current - 1).toString()
        }
        binding.btnIncreaseBathrooms.setOnClickListener {
            val current = binding.tvBathroomCount.text.toString().toIntOrNull() ?: 1
            if (current < 20) binding.tvBathroomCount.text = (current + 1).toString()
        }
    }

    private fun setupAmenitiesChips() {
        val chipGroup = binding.chipGroupAmenities
        chipGroup.removeAllViews()
        Amenities.DEFAULT_AMENITIES.forEach { amenity ->
            val chip = Chip(requireContext()).apply {
                text = amenity
                isCheckable = true
                isClickable = true
            }
            chipGroup.addView(chip)
        }
    }

    /** Wires the Select Photos button and the horizontal preview RecyclerView. */
    private fun setupImagePicker() {
        // Image preview RecyclerView (horizontal strip)
        binding.rvImagePreview.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
            adapter = imagePreviewAdapter
        }

        // Launch system image picker on button tap
        binding.btnSelectImages.setOnClickListener {
            imagePicker.launch("image/*")
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener { onSaveTapped() }
    }

    // ── Save flow ─────────────────────────────────────────────────────────────

    /**
     * Called when the host taps "Save Listing".
     *
     * Decision tree:
     *  - New images selected → upload first, then save automatically on success.
     *  - No new images selected (edit with existing URLs, or listing with no photos)
     *    → save directly, keeping previously uploaded / loaded URLs.
     */
    private fun onSaveTapped() {
        val hasNewImages = viewModel.state.value.selectedImageUris.isNotEmpty()

        if (hasNewImages) {
            // Upload → save chain is triggered reactively via observeState()
            viewModel.uploadImages(requireContext())
        } else {
            // No new picks — save with existing / empty URLs
            saveListing()
        }
    }

    private fun saveListing() {
        val title        = binding.etTitle.text.toString().trim()
        val description  = binding.etDescription.text.toString().trim()
        val location     = binding.etLocation.text.toString().trim()
        val propertyType = binding.spinnerPropertyType.selectedItem.toString()
        val priceText    = binding.etPrice.text.toString().trim()
        val maxGuests    = binding.tvGuestCount.text.toString().toIntOrNull() ?: 1
        val bedrooms     = binding.tvBedroomCount.text.toString().toIntOrNull() ?: 1
        val bathrooms    = binding.tvBathroomCount.text.toString().toIntOrNull() ?: 1
        val amenities    = getSelectedAmenities()

        val price = priceText.toDoubleOrNull()
        if (price == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_invalid_price),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        viewModel.saveListing(
            listingId    = listingId,
            title        = title,
            description  = description,
            location     = location,
            propertyType = propertyType,
            pricePerNight = price,
            maxGuests    = maxGuests,
            bedrooms     = bedrooms,
            bathrooms    = bathrooms,
            amenities    = amenities
        )
    }

    // ── State observation ─────────────────────────────────────────────────────

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->

                    // ── General loading (for edit pre-fill) ───────────────
                    binding.progressBar.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE
                    binding.scrollView.visibility =
                        if (state.isLoading) View.GONE else View.VISIBLE

                    // ── Save button state ─────────────────────────────────
                    val isBusy = state.isSaving ||
                            state.uploadState is UploadState.Uploading ||
                            state.uploadState is UploadState.Progress
                    binding.btnSave.isEnabled = !isBusy
                    binding.btnSave.text = when {
                        state.isSaving -> "Saving…"
                        state.uploadState is UploadState.Uploading -> "Uploading…"
                        state.uploadState is UploadState.Progress ->
                            "Uploading ${(state.uploadState as UploadState.Progress).percent}%…"
                        else -> "Save Listing"
                    }
                    binding.btnSelectImages.isEnabled = !isBusy

                    // ── Image preview strip ───────────────────────────────
                    imagePreviewAdapter.submitList(state.selectedImageUris)
                    binding.rvImagePreview.visibility =
                        if (state.selectedImageUris.isEmpty()) View.GONE else View.VISIBLE

                    // ── Upload progress indicator ─────────────────────────
                    when (val upload = state.uploadState) {
                        is UploadState.Idle -> {
                            binding.progressUpload.visibility = View.GONE
                            // Only hide status if there's no message to show
                            if (binding.tvUploadStatus.text.isNullOrBlank()) {
                                binding.tvUploadStatus.visibility = View.GONE
                            }
                        }
                        is UploadState.Uploading -> {
                            binding.progressUpload.visibility = View.VISIBLE
                            binding.progressUpload.isIndeterminate = true
                            binding.tvUploadStatus.visibility = View.VISIBLE
                            binding.tvUploadStatus.text = "Preparing upload\u2026"
                        }
                        is UploadState.Progress -> {
                            binding.progressUpload.visibility = View.VISIBLE
                            binding.progressUpload.isIndeterminate = false
                            binding.progressUpload.progress = upload.percent
                            binding.tvUploadStatus.visibility = View.VISIBLE
                            val total = state.selectedImageUris.size
                            val done  = (upload.percent * total / 100).coerceAtLeast(0)
                            binding.tvUploadStatus.text = "Uploading $done of $total\u2026"
                        }
                        is UploadState.Success -> {
                            binding.progressUpload.visibility = View.GONE
                            binding.tvUploadStatus.visibility = View.VISIBLE
                            binding.tvUploadStatus.text =
                                "\u2713 ${upload.urls.size} photo(s) uploaded"
                            // Guard: don't double-save if already saving
                            if (!state.isSaving) {
                                saveListing()
                            }
                            viewModel.resetUploadState()
                        }
                        is UploadState.Error -> {
                            binding.progressUpload.visibility = View.GONE
                            binding.tvUploadStatus.visibility = View.VISIBLE
                            binding.tvUploadStatus.text = upload.message
                            // Show Toast so error is visible even if status text is small
                            Toast.makeText(
                                requireContext(),
                                "Upload error: ${upload.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            // If partial success, still save those URLs
                            if (state.uploadedImageUrls.isNotEmpty() && !state.isSaving) {
                                saveListing()
                            }
                            viewModel.resetUploadState()
                        }
                    }

                    // ── Pre-fill form fields (edit mode) ──────────────────
                    state.listing?.let { listing ->
                        binding.etTitle.setText(listing.title)
                        binding.etDescription.setText(listing.description)
                        binding.etLocation.setText(listing.location)
                        binding.etPrice.setText(listing.pricePerNight.toInt().toString())
                        binding.tvGuestCount.text   = listing.maxGuests.toString()
                        binding.tvBedroomCount.text = listing.bedrooms.toString()
                        binding.tvBathroomCount.text = listing.bathrooms.toString()
                        applySelectedAmenities(listing.amenities)

                        val propertyTypes = listOf(
                            "Entire home", "Private room", "Shared room", "Hotel room"
                        )
                        val index = propertyTypes.indexOf(listing.propertyType)
                        if (index >= 0) binding.spinnerPropertyType.setSelection(index)

                        // Show existing image count if editing
                        if (state.uploadedImageUrls.isNotEmpty() &&
                            state.selectedImageUris.isEmpty()) {
                            binding.tvUploadStatus.visibility = View.VISIBLE
                            binding.tvUploadStatus.text =
                                "${state.uploadedImageUrls.size} existing photo(s)"
                        }
                    }

                    // ── Success → navigate away ────────────────────────────
                    if (state.success) {
                        Toast.makeText(
                            requireContext(),
                            if (listingId == null) "Listing created!" else "Listing updated!",
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }

                    // ── Errors ────────────────────────────────────────────
                    state.error?.let { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    // ── Amenities helpers ─────────────────────────────────────────────────────

    private fun getSelectedAmenities(): List<String> {
        val chipGroup = binding.chipGroupAmenities
        val selected = chipGroup.checkedChipIds.mapNotNull { id ->
            chipGroup.findViewById<Chip>(id)?.text?.toString()
        }
        return Amenities.standardize(selected)
    }

    private fun applySelectedAmenities(amenities: List<String>) {
        val standardized = Amenities.standardize(amenities)
        val chipGroup = binding.chipGroupAmenities
        for (index in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(index) as? Chip ?: continue
            chip.isChecked = standardized.contains(chip.text.toString())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
