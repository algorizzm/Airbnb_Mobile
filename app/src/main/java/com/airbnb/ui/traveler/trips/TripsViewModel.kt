package com.airbnb.ui.traveler.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.core.auth.AuthManager
import com.airbnb.data.model.TripItem
import com.airbnb.data.repository.ListingRepository
import com.airbnb.data.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TripsViewModel(
    private val reservationRepository: ReservationRepository = ReservationRepository(),
    private val listingRepository: ListingRepository = ListingRepository()
) : ViewModel() {

    private val _upcomingTrips = MutableStateFlow<List<TripItem>>(emptyList())
    val upcomingTrips: StateFlow<List<TripItem>> = _upcomingTrips.asStateFlow()

    private val _pastTrips = MutableStateFlow<List<TripItem>>(emptyList())
    val pastTrips: StateFlow<List<TripItem>> = _pastTrips.asStateFlow()

    private val _cancelledTrips = MutableStateFlow<List<TripItem>>(emptyList())
    val cancelledTrips: StateFlow<List<TripItem>> = _cancelledTrips.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    init {
        loadTrips()
    }

    private fun loadTrips() {
        val userId = AuthManager.currentUserId()
        if (userId == null) {
            _toast.value = "Please log in to view your trips"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                reservationRepository.observeReservationsForGuest(userId)
                    .catch { e ->
                        _toast.value = "Failed to load trips: ${e.message}"
                        _isLoading.value = false
                    }
                    .collect { reservations ->
                        // Fetch listing details for each reservation
                        val tripItems = reservations.mapNotNull { reservation ->
                            val listing = try {
                                listingRepository.getListing(reservation.listingId).getOrNull()
                            } catch (e: Exception) {
                                null
                            }
                            TripItem(reservation, listing)
                        }

                        // Group trips by status
                        _upcomingTrips.value = tripItems.filter { it.isUpcoming() }
                        _pastTrips.value = tripItems.filter { it.isCompleted() }
                        _cancelledTrips.value = tripItems.filter { it.isCancelled() }

                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _toast.value = "Failed to load trips: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun cancelReservation(reservationId: String) {
        viewModelScope.launch {
            reservationRepository.cancelReservation(reservationId)
                .onSuccess {
                    _toast.value = "Reservation cancelled successfully"
                }
                .onFailure { error ->
                    _toast.value = "Failed to cancel reservation: ${error.message}"
                }
        }
    }

    fun consumeToast() {
        _toast.value = null
    }
}
