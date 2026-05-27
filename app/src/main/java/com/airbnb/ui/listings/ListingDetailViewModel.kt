package com.airbnb.ui.listings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Listing
import com.airbnb.data.repository.ListingRepository
import com.airbnb.data.session.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ListingDetailViewModel(
    private val listingId: String,
    private val listingRepository: ListingRepository = ListingRepository()
) : ViewModel() {

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _isOwnListing = MutableStateFlow(false)
    val isOwnListing: StateFlow<Boolean> = _isOwnListing.asStateFlow()

    init {
        loadListing()
    }

    private fun loadListing() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                listingRepository.observeListing(listingId).collect { listing ->
                    _listing.value = listing
                    _isLoading.value = false

                    if (listing == null) {
                        _error.value = "Listing not found"
                    } else {
                        // Check if current user is the host
                        val currentUser = UserSessionManager.currentUser.value
                        _isOwnListing.value = currentUser?.id == listing.hostId
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load listing: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun showToast(message: String) {
        _toast.value = message
    }

    fun consumeToast() {
        _toast.value = null
    }

    class Factory(private val listingId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ListingDetailViewModel(listingId) as T
        }
    }
}
