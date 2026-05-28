package com.airbnb.ui.traveler.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Listing
import com.airbnb.data.repository.ListingRepository
import com.airbnb.data.repository.WishlistRepository
import com.airbnb.core.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WishlistViewModel(
    private val wishlistRepository: WishlistRepository = WishlistRepository(),
    private val listingRepository: ListingRepository = ListingRepository()
) : ViewModel() {

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    init {
        loadWishlist()
    }

    private fun loadWishlist() {
        val userId = AuthManager.currentUserId() ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                wishlistRepository.observeWishlist(userId).collect { wishlist ->
                    if (wishlist.listingIds.isEmpty()) {
                        _listings.value = emptyList()
                        _isLoading.value = false
                    } else {
                        // Fetch all listings for the wishlist IDs
                        val fetchedListings = mutableListOf<Listing>()
                        wishlist.listingIds.forEach { listingId ->
                            listingRepository.getListing(listingId)
                                .onSuccess { listing ->
                                    fetchedListings.add(listing)
                                }
                                .onFailure { error ->
                                    // Silently skip listings that fail to load
                                    // (e.g., deleted listings)
                                }
                        }
                        _listings.value = fetchedListings
                        _isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                _toast.value = "Failed to load wishlist: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun removeFromWishlist(listingId: String) {
        val userId = AuthManager.currentUserId() ?: return

        viewModelScope.launch {
            wishlistRepository.removeFromWishlist(userId, listingId)
                .onSuccess {
                    _toast.value = "Removed from wishlist"
                }
                .onFailure { error ->
                    _toast.value = "Failed to remove: ${error.message}"
                }
        }
    }

    fun consumeToast() {
        _toast.value = null
    }
}
