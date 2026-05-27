package com.airbnb.ui.hosting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Reservation
import com.airbnb.data.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class HostReservationsState(
    val reservations: List<Reservation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class HostReservationsViewModel(
    private val reservationRepository: ReservationRepository = ReservationRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(HostReservationsState())
    val state: StateFlow<HostReservationsState> = _state.asStateFlow()

    /**
     * Loads reservations for a specific listing.
     */
    fun loadReservationsForListing(listingId: String) {
        _state.value = _state.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            reservationRepository.observeReservationsForListing(listingId)
                .catch { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load reservations"
                    )
                }
                .collect { reservations ->
                    _state.value = _state.value.copy(
                        reservations = reservations,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    /**
     * Cancels a reservation.
     */
    fun cancelReservation(reservationId: String) {
        viewModelScope.launch {
            reservationRepository.cancelReservation(reservationId)
                .onSuccess {
                    _state.value = _state.value.copy(
                        message = "Reservation cancelled"
                    )
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(
                        error = e.message ?: "Failed to cancel reservation"
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
