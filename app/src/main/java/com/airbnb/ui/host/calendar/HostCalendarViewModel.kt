package com.airbnb.ui.host.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.core.auth.AuthManager
import com.airbnb.data.model.BlockedDate
import com.airbnb.data.model.Listing
import com.airbnb.data.model.Reservation
import com.airbnb.data.repository.CalendarRepository
import com.airbnb.data.repository.ListingRepository
import com.airbnb.data.repository.ReservationRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for Host Calendar screen.
 * Manages blocked dates, reservations, and availability for host's listings.
 */
class HostCalendarViewModel(
    private val authManager: AuthManager = AuthManager,
    private val listingRepository: ListingRepository = ListingRepository(),
    private val reservationRepository: ReservationRepository = ReservationRepository(),
    private val calendarRepository: CalendarRepository = CalendarRepository()
) : ViewModel() {
    
    private val _hostListings = MutableStateFlow<List<Listing>>(emptyList())
    val hostListings: StateFlow<List<Listing>> = _hostListings.asStateFlow()
    
    private val _selectedListing = MutableStateFlow<Listing?>(null)
    val selectedListing: StateFlow<Listing?> = _selectedListing.asStateFlow()
    
    private val _reservations = MutableStateFlow<List<Reservation>>(emptyList())
    val reservations: StateFlow<List<Reservation>> = _reservations.asStateFlow()
    
    private val _blockedDates = MutableStateFlow<List<BlockedDate>>(emptyList())
    val blockedDates: StateFlow<List<BlockedDate>> = _blockedDates.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    init {
        loadHostListings()
    }
    
    /**
     * Loads all listings for the current host.
     */
    private fun loadHostListings() {
        val hostId = authManager.currentUserId() ?: return
        
        viewModelScope.launch {
            listingRepository.observeListingsForHost(hostId).collect { listings ->
                _hostListings.value = listings
                
                // Auto-select first listing if none selected
                if (_selectedListing.value == null && listings.isNotEmpty()) {
                    selectListing(listings.first())
                }
            }
        }
    }
    
    /**
     * Selects a listing to view its calendar.
     */
    fun selectListing(listing: Listing) {
        _selectedListing.value = listing
        loadCalendarData(listing.id)
    }
    
    /**
     * Loads reservations and blocked dates for the selected listing.
     */
    private fun loadCalendarData(listingId: String) {
        viewModelScope.launch {
            // Observe reservations
            launch {
                reservationRepository.observeReservationsForListing(listingId).collect { reservations ->
                    _reservations.value = reservations
                }
            }
            
            // Observe blocked dates
            launch {
                calendarRepository.observeBlockedDatesForListing(listingId).collect { blockedDates ->
                    _blockedDates.value = blockedDates
                }
            }
        }
    }
    
    /**
     * Blocks a date range for the selected listing.
     */
    fun blockDateRange(startDate: Date, endDate: Date, reason: String) {
        val listing = _selectedListing.value ?: return
        val hostId = authManager.currentUserId() ?: return
        
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            val result = calendarRepository.createBlockedDate(
                listingId = listing.id,
                hostId = hostId,
                startDate = Timestamp(startDate),
                endDate = Timestamp(endDate),
                reason = reason
            )
            
            _isLoading.value = false
            
            result.onSuccess {
                _successMessage.value = "Dates blocked successfully"
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to block dates"
            }
        }
    }
    
    /**
     * Unblocks a date range.
     */
    fun unblockDateRange(blockedDateId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            val result = calendarRepository.removeBlockedDate(blockedDateId)
            
            _isLoading.value = false
            
            result.onSuccess {
                _successMessage.value = "Dates unblocked successfully"
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to unblock dates"
            }
        }
    }
    
    /**
     * Clears error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Clears success message.
     */
    fun clearSuccess() {
        _successMessage.value = null
    }
}
