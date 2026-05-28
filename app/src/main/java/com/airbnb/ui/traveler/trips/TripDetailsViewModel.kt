package com.airbnb.ui.traveler.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.TripItem
import com.airbnb.data.repository.ListingRepository
import com.airbnb.data.repository.ReservationRepository
import com.airbnb.utils.ReservationLifecycleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TripDetailsViewModel(
    private val reservationRepository: ReservationRepository = ReservationRepository(),
    private val listingRepository: ListingRepository = ListingRepository()
) : ViewModel() {

    private val _tripItem = MutableStateFlow<TripItem?>(null)
    val tripItem: StateFlow<TripItem?> = _tripItem.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _navigationEvent = MutableStateFlow(false)
    val navigationEvent: StateFlow<Boolean> = _navigationEvent.asStateFlow()

    fun loadReservation(reservationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = reservationRepository.getReservation(reservationId)
                
                result.onSuccess { reservation ->
                    // Sync lifecycle status
                    val newStatus = ReservationLifecycleManager.determineLifecycleStatus(reservation)
                    val syncedReservation = if (newStatus != reservation.status) {
                        reservationRepository.updateReservationStatus(reservation.id, newStatus)
                        reservation.copy(status = newStatus)
                    } else {
                        reservation
                    }

                    // Fetch listing details
                    val listing = try {
                        listingRepository.getListing(syncedReservation.listingId).getOrNull()
                    } catch (e: Exception) {
                        null
                    }

                    _tripItem.value = TripItem(syncedReservation, listing)
                }.onFailure { error ->
                    _toast.value = "Failed to load reservation: ${error.message}"
                }
            } catch (e: Exception) {
                _toast.value = "Failed to load reservation: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelReservation(reservationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            reservationRepository.cancelReservation(reservationId)
                .onSuccess {
                    _toast.value = "Reservation cancelled successfully"
                    _navigationEvent.value = true
                }
                .onFailure { error ->
                    _toast.value = "Failed to cancel reservation: ${error.message}"
                }
            _isLoading.value = false
        }
    }

    fun checkInReservation(reservationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            reservationRepository.checkInReservation(reservationId)
                .onSuccess {
                    _toast.value = "Checked in successfully!"
                    // Reload reservation to reflect changes
                    loadReservation(reservationId)
                }
                .onFailure { error ->
                    _toast.value = "Check-in failed: ${error.message}"
                    _isLoading.value = false
                }
        }
    }

    fun checkOutReservation(reservationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            reservationRepository.checkOutReservation(reservationId)
                .onSuccess {
                    _toast.value = "Checked out successfully!"
                    // Reload reservation to reflect changes
                    loadReservation(reservationId)
                }
                .onFailure { error ->
                    _toast.value = "Check-out failed: ${error.message}"
                    _isLoading.value = false
                }
        }
    }

    fun earlyCheckOutReservation(reservationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            reservationRepository.earlyCheckOutReservation(reservationId)
                .onSuccess {
                    _toast.value = "Early check-out completed successfully!"
                    // Reload reservation to reflect changes
                    loadReservation(reservationId)
                }
                .onFailure { error ->
                    _toast.value = "Early check-out failed: ${error.message}"
                    _isLoading.value = false
                }
        }
    }

    fun consumeToast() {
        _toast.value = null
    }
}
