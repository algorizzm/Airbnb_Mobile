package com.airbnb.ui.host.listings.create

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Listing
import com.airbnb.data.repository.ListingRepository
import com.airbnb.data.repository.UserRepository
import com.airbnb.utils.Amenities
import com.airbnb.utils.cloudinary.CloudinaryManager
import com.airbnb.utils.cloudinary.ImageCompressor
import com.airbnb.utils.cloudinary.UploadState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateListingState(
    val listing: Listing? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    /** Current state of the Cloudinary image upload pipeline. */
    val uploadState: UploadState = UploadState.Idle,
    /** URIs the user has picked — shown in the preview strip before upload. */
    val selectedImageUris: List<Uri> = emptyList(),
    /** Cloudinary secure_urls ready to be saved to Firestore. */
    val uploadedImageUrls: List<String> = emptyList()
)

class CreateListingViewModel(
    private val listingRepository: ListingRepository = ListingRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(CreateListingState())
    val state: StateFlow<CreateListingState> = _state.asStateFlow()

    // ── Listing loading ───────────────────────────────────────────────────────

    /**
     * Loads an existing listing for editing.
     */
    fun loadListing(listingId: String) {
        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            listingRepository.getListing(listingId)
                .onSuccess { listing ->
                    // Pre-populate uploadedImageUrls from existing listing data
                    // so an edit that skips re-uploading keeps the current URLs.
                    val existingUrls = buildList {
                        if (listing.imageUrl.isNotBlank()) add(listing.imageUrl)
                        listing.galleryImageUrls.forEach { url ->
                            if (url.isNotBlank() && url != listing.imageUrl) add(url)
                        }
                    }
                    _state.value = _state.value.copy(
                        listing = listing,
                        isLoading = false,
                        uploadedImageUrls = existingUrls
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load listing"
                    )
                }
        }
    }

    // ── Image selection ───────────────────────────────────────────────────────

    /**
     * Stores the URIs picked by the user. Triggers a recomposition of the
     * preview strip but does NOT start upload yet.
     */
    fun setSelectedImages(uris: List<Uri>) {
        _state.value = _state.value.copy(
            selectedImageUris = uris,
            // Reset any previous upload result when new images are chosen
            uploadState = UploadState.Idle,
            uploadedImageUrls = emptyList()
        )
    }

    /**
     * Removes a single URI from the pending selection queue.
     */
    fun removeSelectedImage(uri: Uri) {
        val updated = _state.value.selectedImageUris.filter { it != uri }
        _state.value = _state.value.copy(selectedImageUris = updated)
    }

    // ── Image upload ──────────────────────────────────────────────────────────

    /**
     * Compresses and uploads all selected images to Cloudinary.
     *
     * Progress is emitted through [UploadState.Progress] (per-image granularity).
     * On success, [UploadState.Success] is set and the caller should then
     * invoke [saveListing].
     *
     * @param context Required for content resolver (compression) and Cloudinary dispatch.
     */
    fun uploadImages(context: Context) {
        val uris = _state.value.selectedImageUris
        if (uris.isEmpty()) return

        _state.value = _state.value.copy(uploadState = UploadState.Uploading)

        viewModelScope.launch {
            val uploadedUrls = mutableListOf<String>()
            val errors = mutableListOf<String>()

            uris.forEachIndexed { index, uri ->
                // Emit per-image progress (index+1 of total)
                val percent = ((index.toFloat() / uris.size) * 100).toInt()
                _state.value = _state.value.copy(
                    uploadState = UploadState.Progress(percent)
                )

                // 1. Compress before upload
                val compressedUri = runCatching {
                    ImageCompressor.compress(context, uri)
                }.getOrElse { e ->
                    errors.add("Image ${index + 1}: compression failed — ${e.message}")
                    return@forEachIndexed
                }

                // 2. Upload to Cloudinary
                CloudinaryManager.uploadImage(context, compressedUri)
                    .onSuccess { url -> uploadedUrls.add(url) }
                    .onFailure { e -> errors.add("Image ${index + 1}: ${e.message}") }
            }

            // Final state
            when {
                errors.isEmpty() -> {
                    _state.value = _state.value.copy(
                        uploadState = UploadState.Success(uploadedUrls),
                        uploadedImageUrls = uploadedUrls
                    )
                }
                uploadedUrls.isEmpty() -> {
                    // All failed
                    _state.value = _state.value.copy(
                        uploadState = UploadState.Error(
                            "All uploads failed:\n${errors.joinToString("\n")}"
                        )
                    )
                }
                else -> {
                    // Partial success — keep what we got, report failures
                    _state.value = _state.value.copy(
                        uploadState = UploadState.Error(
                            "${errors.size}/${uris.size} images failed. " +
                            "${uploadedUrls.size} uploaded successfully.\n" +
                            errors.joinToString("\n")
                        ),
                        uploadedImageUrls = uploadedUrls
                    )
                }
            }
        }
    }

    /**
     * Resets upload state back to Idle (e.g. after showing error to user).
     */
    fun resetUploadState() {
        _state.value = _state.value.copy(uploadState = UploadState.Idle)
    }

    // ── Listing save ──────────────────────────────────────────────────────────

    /**
     * Creates or updates a listing in Firestore.
     *
     * Image URLs are taken from [CreateListingState.uploadedImageUrls].
     * If no images were uploaded (e.g. edit with no new picks), the existing
     * URLs from the loaded listing are preserved automatically.
     */
    fun saveListing(
        listingId: String?,
        title: String,
        description: String,
        location: String,
        propertyType: String,
        pricePerNight: Double,
        maxGuests: Int,
        bedrooms: Int,
        bathrooms: Int,
        amenities: List<String>
    ) {
        // Validation
        if (title.isBlank()) {
            _state.value = _state.value.copy(error = "Title is required")
            return
        }
        if (description.isBlank()) {
            _state.value = _state.value.copy(error = "Description is required")
            return
        }
        if (location.isBlank()) {
            _state.value = _state.value.copy(error = "Location is required")
            return
        }
        if (pricePerNight <= 0) {
            _state.value = _state.value.copy(error = "Price must be greater than 0")
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            _state.value = _state.value.copy(error = "You must be logged in")
            return
        }

        _state.value = _state.value.copy(isSaving = true, error = null)

        viewModelScope.launch {
            // Resolve host display name
            val user = userRepository.getUser(currentUser.uid).getOrNull()
            val hostName = user?.name?.ifBlank { null }
                ?: user?.fname?.ifBlank { null }
                ?: currentUser.displayName
                ?: currentUser.email?.substringBefore("@")
                ?: "Host"

            val standardizedAmenities = Amenities.standardize(amenities)

            // Resolve image URLs: newly uploaded → else existing listing URLs → else empty
            val imageUrls = _state.value.uploadedImageUrls
            val coverImageUrl = imageUrls.firstOrNull() ?: ""
            val galleryUrls   = if (imageUrls.size > 1) imageUrls.drop(1) else emptyList()

            val listing = Listing(
                id               = listingId ?: "",
                title            = title,
                description      = description,
                location         = location,
                propertyType     = propertyType,
                pricePerNight    = pricePerNight,
                maxGuests        = maxGuests,
                bedrooms         = bedrooms,
                bathrooms        = bathrooms,
                amenities        = standardizedAmenities,
                imageUrl         = coverImageUrl,
                galleryImageUrls = galleryUrls,
                hostId           = currentUser.uid,
                hostName         = hostName,
                hostProfileImage = user?.profileImage
                    ?.ifBlank { null }
                    ?: currentUser.photoUrl?.toString()
                    ?: "",
                createdAt = if (listingId.isNullOrBlank()) {
                    Timestamp.now()
                } else {
                    _state.value.listing?.createdAt
                },
                updatedAt = Timestamp.now()
            )

            val result = if (listingId.isNullOrBlank()) {
                listingRepository.createListing(listing)
            } else {
                listingRepository.updateListing(listing).map { listingId }
            }

            result
                .onSuccess {
                    _state.value = _state.value.copy(isSaving = false, success = true)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to save listing"
                    )
                }
        }
    }

    /**
     * Clears the error state after it's been shown to the user.
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
