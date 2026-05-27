package com.airbnb.ui.hosting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Listing
import com.airbnb.data.repository.ListingRepository
import com.airbnb.data.repository.UserRepository
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
    val success: Boolean = false
)

class CreateListingViewModel(
    private val listingRepository: ListingRepository = ListingRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(CreateListingState())
    val state: StateFlow<CreateListingState> = _state.asStateFlow()

    /**
     * Loads an existing listing for editing.
     */
    fun loadListing(listingId: String) {
        _state.value = _state.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            listingRepository.getListing(listingId)
                .onSuccess { listing ->
                    _state.value = _state.value.copy(
                        listing = listing,
                        isLoading = false
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

    /**
     * Creates or updates a listing.
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
        amenities: List<String>,
        imageUrl: String
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
            // Fetch user's name from Firestore
            val user = userRepository.getUser(currentUser.uid).getOrNull()
            val hostName = user?.name?.ifBlank { null } 
                ?: user?.fname?.ifBlank { null }
                ?: currentUser.displayName 
                ?: currentUser.email?.substringBefore("@") 
                ?: "Host"

            val listing = Listing(
                id = listingId ?: "",
                title = title,
                description = description,
                location = location,
                propertyType = propertyType,
                pricePerNight = pricePerNight,
                maxGuests = maxGuests,
                bedrooms = bedrooms,
                bathrooms = bathrooms,
                amenities = amenities,
                imageUrl = imageUrl,
                hostId = currentUser.uid,
                hostName = hostName,
                createdAt = if (listingId.isNullOrBlank()) Timestamp.now() else _state.value.listing?.createdAt,
                updatedAt = Timestamp.now()
            )

            val result = if (listingId.isNullOrBlank()) {
                listingRepository.createListing(listing)
            } else {
                listingRepository.updateListing(listing).map { listingId }
            }

            result
                .onSuccess {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        success = true
                    )
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
     * Clears the error after it's been shown.
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
