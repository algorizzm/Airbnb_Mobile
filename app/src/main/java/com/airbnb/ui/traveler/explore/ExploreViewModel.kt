package com.airbnb.ui.traveler.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.core.auth.AuthManager
import com.airbnb.data.model.Listing
import com.airbnb.data.repository.ListingRepository
import com.airbnb.data.repository.WishlistRepository
import com.airbnb.data.repository.WishlistCollectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val listingRepository: ListingRepository = ListingRepository(),
    private val wishlistRepository: WishlistRepository = WishlistRepository(),
    private val collectionRepository: WishlistCollectionRepository = WishlistCollectionRepository()
) : ViewModel() {

    private val _allListings =
        MutableStateFlow<List<Listing>>(emptyList())

    private val _searchQuery =
        MutableStateFlow("")

    val searchQuery: StateFlow<String> =
        _searchQuery.asStateFlow()

    private val _maxPrice =
        MutableStateFlow<Double?>(null)

    private val _minGuests =
        MutableStateFlow<Int?>(null)

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _toast =
        MutableStateFlow<String?>(null)

    val toast: StateFlow<String?> =
        _toast.asStateFlow()

    private val _wishlistIds =
        MutableStateFlow<Set<String>>(emptySet())

    val wishlistIds: StateFlow<Set<String>> =
        _wishlistIds.asStateFlow()

    val displayListings: StateFlow<List<Listing>> = combine(
        _allListings,
        _searchQuery,
        _maxPrice,
        _minGuests
    ) { listings, query, maxPrice, minGuests ->

        listings.filter { listing ->

            val q = query.trim()

            val matchesQuery =
                q.isEmpty() ||
                        listing.title.contains(
                            q,
                            ignoreCase = true
                        ) ||
                        listing.location.contains(
                            q,
                            ignoreCase = true
                        )

            val matchesPrice =
                maxPrice == null ||
                        listing.pricePerNight <= maxPrice

            val matchesGuests =
                minGuests == null ||
                        listing.maxGuests >= minGuests

            matchesQuery && matchesPrice && matchesGuests
        }

    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    init {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                listingRepository.observeListings()
                    .collect { listings ->
                        _allListings.value = listings
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _toast.value = "Failed to load listings: ${e.message}"
                _isLoading.value = false
            }
        }

        // Observe wishlist state derived from collections (source of truth).
        // This ensures that deleting a collection and removing its listings
        // immediately refreshes the heart icon state in ExploreFragment.
        val userId = AuthManager.currentUserId()
        if (userId != null) {
            viewModelScope.launch {
                try {
                    collectionRepository.observeCollections(userId).collect { collections ->
                        // A listing is wishlisted iff it exists in at least one collection
                        val savedIds = collections
                            .flatMap { it.listingIds }
                            .toSet()
                        _wishlistIds.value = savedIds
                    }
                } catch (e: Exception) {
                    // Silently fail - wishlist is optional
                }
            }
        }
    }

    fun setSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun setMaxPrice(value: Double?) {
        _maxPrice.value = value
    }

    fun setMinGuests(value: Int?) {
        _minGuests.value = value
    }

    fun consumeToast() {
        _toast.value = null
    }

    fun toggleWishlist(listingId: String, collectionId: String? = null) {
        val userId = AuthManager.currentUserId()
        if (userId == null) {
            _toast.value = "Please log in to save listings"
            return
        }

        viewModelScope.launch {
            wishlistRepository.toggleWishlist(userId, listingId, collectionId)
                .onSuccess { isAdded ->
                    _toast.value = if (isAdded) {
                        "Added to wishlist"
                    } else {
                        "Removed from wishlist"
                    }
                }
                .onFailure { error ->
                    _toast.value = "Failed to update wishlist: ${error.message}"
                }
        }
    }

    /**
     * Adds a listing to a specific collection.
     * Used when user selects a collection from the dialog.
     */
    fun addToCollection(listingId: String, collectionId: String) {
        val userId = AuthManager.currentUserId()
        if (userId == null) {
            _toast.value = "Please log in to save listings"
            return
        }

        viewModelScope.launch {
            wishlistRepository.addToWishlist(userId, listingId, collectionId)
                .onSuccess {
                    _toast.value = "Added to collection"
                }
                .onFailure { error ->
                    _toast.value = "Failed to add: ${error.message}"
                }
        }
    }
}