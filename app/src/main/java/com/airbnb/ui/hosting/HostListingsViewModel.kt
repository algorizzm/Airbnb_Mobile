package com.airbnb.ui.hosting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Listing
import com.airbnb.data.repository.ListingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class HostListingsState(
    val listings: List<Listing> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class HostListingsViewModel(
    private val listingRepository: ListingRepository = ListingRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(HostListingsState())
    val state: StateFlow<HostListingsState> = _state.asStateFlow()

    init {
        loadHostListings()
    }

    /**
     * Loads listings for the current host user.
     */
    private fun loadHostListings() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        _state.value = _state.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            listingRepository.observeListingsForHost(currentUserId)
                .catch { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load listings"
                    )
                }
                .collect { listings ->
                    _state.value = _state.value.copy(
                        listings = listings,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    /**
     * Deletes a listing (soft delete by setting status to inactive).
     */
    fun deleteListing(listingId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            listingRepository.deleteListing(listingId)
                .onSuccess {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        message = "Listing deleted successfully"
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to delete listing"
                    )
                }
        }
    }

    /**
     * Consumes the message after it's been shown.
     */
    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    /**
     * Clears the error after it's been shown.
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
