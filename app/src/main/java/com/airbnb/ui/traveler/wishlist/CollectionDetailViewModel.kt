package com.airbnb.ui.traveler.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Listing
import com.airbnb.data.model.WishlistCollection
import com.airbnb.data.repository.ListingRepository
import com.airbnb.data.repository.WishlistCollectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for collection detail screen.
 * Loads and displays listings within a specific collection.
 */
class CollectionDetailViewModel(
    private val collectionRepository: WishlistCollectionRepository = WishlistCollectionRepository(),
    private val listingRepository: ListingRepository = ListingRepository()
) : ViewModel() {

    private val _collection = MutableStateFlow<WishlistCollection?>(null)
    val collection: StateFlow<WishlistCollection?> = _collection.asStateFlow()

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun loadCollection(collectionId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get collection
                collectionRepository.getCollection(collectionId)
                    .onSuccess { collection ->
                        _collection.value = collection
                        collection?.let { loadListings(it.listingIds) }
                    }
                    .onFailure { error ->
                        _toast.value = "Failed to load collection: ${error.message}"
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _toast.value = "Failed to load collection: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadListings(listingIds: List<String>) {
        if (listingIds.isEmpty()) {
            _listings.value = emptyList()
            _isLoading.value = false
            return
        }

        val fetchedListings = mutableListOf<Listing>()
        listingIds.forEach { listingId ->
            listingRepository.getListing(listingId)
                .onSuccess { listing ->
                    fetchedListings.add(listing)
                }
                .onFailure {
                    // Silently skip listings that fail to load
                }
        }
        _listings.value = fetchedListings
        _isLoading.value = false
    }

    fun removeFromCollection(collectionId: String, listingId: String) {
        viewModelScope.launch {
            collectionRepository.removeListingFromCollection(collectionId, listingId)
                .onSuccess {
                    _toast.value = "Removed from collection"
                    // Reload collection to update UI
                    loadCollection(collectionId)
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
