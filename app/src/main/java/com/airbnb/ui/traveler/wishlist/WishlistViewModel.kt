package com.airbnb.ui.traveler.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Listing
import com.airbnb.data.model.WishlistCollection
import com.airbnb.data.repository.ListingRepository
import com.airbnb.data.repository.WishlistRepository
import com.airbnb.data.repository.WishlistCollectionRepository
import com.airbnb.core.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WishlistViewModel(
    private val wishlistRepository: WishlistRepository = WishlistRepository(),
    private val collectionRepository: WishlistCollectionRepository = WishlistCollectionRepository(),
    private val listingRepository: ListingRepository = ListingRepository()
) : ViewModel() {

    private val _collections = MutableStateFlow<List<WishlistCollection>>(emptyList())
    val collections: StateFlow<List<WishlistCollection>> = _collections.asStateFlow()

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    init {
        loadCollections()
    }

    private fun loadCollections() {
        val userId = AuthManager.currentUserId() ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                collectionRepository.observeCollections(userId).collect { collections ->
                    _collections.value = collections
                    _isLoading.value = false

                    // Auto-create default collection if none exist
                    if (collections.isEmpty()) {
                        createDefaultCollection()
                    }
                }
            } catch (e: Exception) {
                _toast.value = "Failed to load collections: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun createDefaultCollection() {
        val userId = AuthManager.currentUserId() ?: return

        viewModelScope.launch {
            collectionRepository.getOrCreateDefaultCollection(userId)
                .onFailure { error ->
                    _toast.value = "Failed to create default collection: ${error.message}"
                }
        }
    }

    fun renameCollection(collectionId: String, newName: String) {
        viewModelScope.launch {
            collectionRepository.renameCollection(collectionId, newName)
                .onSuccess {
                    _toast.value = "Collection renamed"
                }
                .onFailure { error ->
                    _toast.value = "Failed to rename: ${error.message}"
                }
        }
    }

    fun deleteCollection(collection: WishlistCollection) {

        // HARD BLOCK
        if (collection.isDefault) {
            _toast.value = "Favorites collection cannot be deleted"
            return
        }

        val userId = AuthManager.currentUserId() ?: return

        viewModelScope.launch {

            collectionRepository.deleteCollection(
                collection.id,
                userId,
                moveToDefault = true
            )
                .onSuccess {
                    _toast.value = "Collection deleted"
                }
                .onFailure { error ->
                    _toast.value = "Failed to delete: ${error.message}"
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
