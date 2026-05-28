package com.airbnb.ui.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Listing
import com.airbnb.data.model.Reservation
import com.airbnb.data.repository.ListingRepository
import com.airbnb.data.repository.ReservationRepository
import com.airbnb.data.session.UserSessionManager
import com.airbnb.utils.ReservationDateValidator
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class CreateReservationViewModel(
    private val listingId: String,
    private val listingRepository: ListingRepository = ListingRepository(),
    private val reservationRepository: ReservationRepository = ReservationRepository()
) : ViewModel() {

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing.asStateFlow()

    private val _checkInDate = MutableStateFlow<Date?>(null)
    val checkInDate: StateFlow<Date?> = _checkInDate.asStateFlow()

    private val _checkOutDate = MutableStateFlow<Date?>(null)
    val checkOutDate: StateFlow<Date?> = _checkOutDate.asStateFlow()

    private val _numberOfGuests = MutableStateFlow(1)
    val numberOfGuests: StateFlow<Int> = _numberOfGuests.asStateFlow()

    private val _totalPrice = MutableStateFlow(0.0)
    val totalPrice: StateFlow<Double> = _totalPrice.asStateFlow()

    private val _numberOfNights = MutableStateFlow(0)
    val numberOfNights: StateFlow<Int> = _numberOfNights.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _reservationCreated = MutableStateFlow(false)
    val reservationCreated: StateFlow<Boolean> = _reservationCreated.asStateFlow()

    init {
        loadListing()
    }

    private fun loadListing() {
        viewModelScope.launch {
            try {
                val result = listingRepository.getListing(listingId)
                result.onSuccess { listing ->
                    _listing.value = listing
                }.onFailure { e ->
                    _error.value = "Failed to load listing: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load listing: ${e.message}"
            }
        }
    }

    fun setCheckInDate(date: Date) {
        if (!ReservationDateValidator.isCheckInValid(date)) {
            _toast.value = "Check-in date cannot be in the past"
            return
        }
        _checkInDate.value = ReservationDateValidator.startOfDay(date)
        val selectedCheckOut = _checkOutDate.value
        if (selectedCheckOut != null && !ReservationDateValidator.isDateRangeValid(_checkInDate.value!!, selectedCheckOut)) {
            _checkOutDate.value = null
            _toast.value = "Please select a new check-out date"
        }
        calculatePricing()
    }

    fun setCheckOutDate(date: Date) {
        val selectedCheckIn = _checkInDate.value
        if (selectedCheckIn == null) {
            _toast.value = "Please select check-in date first"
            return
        }
        val normalizedCheckOut = ReservationDateValidator.startOfDay(date)
        if (!ReservationDateValidator.isDateRangeValid(selectedCheckIn, normalizedCheckOut)) {
            _toast.value = "Check-out must be at least 1 day after check-in"
            return
        }
        _checkOutDate.value = normalizedCheckOut
        calculatePricing()
    }

    fun setNumberOfGuests(guests: Int) {
        val listing = _listing.value ?: return
        if (guests in 1..listing.maxGuests) {
            _numberOfGuests.value = guests
        } else {
            _toast.value = "Maximum ${listing.maxGuests} guests allowed"
        }
    }

    private fun calculatePricing() {
        val checkIn = _checkInDate.value ?: return
        val checkOut = _checkOutDate.value ?: return
        val listing = _listing.value ?: return

        val nights = calculateNights(checkIn, checkOut)
        if (nights <= 0) {
            _numberOfNights.value = 0
            _totalPrice.value = 0.0
            return
        }

        _numberOfNights.value = nights
        _totalPrice.value = listing.pricePerNight * nights
    }

    private fun calculateNights(checkIn: Date, checkOut: Date): Int {
        return ReservationDateValidator.calculateNights(checkIn, checkOut)
    }

    fun createReservation() {
        viewModelScope.launch {
            val listing = _listing.value
            val checkIn = _checkInDate.value
            val checkOut = _checkOutDate.value
            val user = UserSessionManager.currentUser.value

            // Validation
            if (listing == null) {
                _toast.value = "Listing not found"
                return@launch
            }

            if (user == null) {
                _toast.value = "Please log in to make a reservation"
                return@launch
            }

            // Prevent host from booking their own listing
            if (user.id == listing.hostId) {
                _toast.value = "You cannot book your own listing"
                _isLoading.value = false
                return@launch
            }

            if (checkIn == null || checkOut == null) {
                _toast.value = "Please select check-in and check-out dates"
                return@launch
            }

            if (!ReservationDateValidator.isDateRangeValid(checkIn, checkOut)) {
                _toast.value = "Check-out must be after check-in"
                return@launch
            }

            if (_numberOfNights.value <= 0) {
                _toast.value = "Invalid date range"
                return@launch
            }

            _isLoading.value = true
            _error.value = null

            try {
                // Check if user already has an active reservation for this listing
                val hasActive = reservationRepository.hasActiveReservation(user.id, listing.id)
                if (hasActive) {
                    _toast.value = "You already have an active reservation for this property"
                    _isLoading.value = false
                    return@launch
                }

                val normalizedCheckIn = ReservationDateValidator.startOfDay(checkIn)
                val normalizedCheckOut = ReservationDateValidator.startOfDay(checkOut)

                if (!ReservationDateValidator.isDateRangeValid(normalizedCheckIn, normalizedCheckOut)) {
                    _toast.value = "Invalid reservation dates"
                    _isLoading.value = false
                    return@launch
                }

                val reservation = Reservation(
                    listingId = listing.id,
                    listingTitle = listing.title,
                    listingImageUrl = listing.imageUrl,
                    guestId = user.id,
                    guestName = user.name,
                    guestAvatarUrl = user.profileImage.ifBlank { null },
                    hostId = listing.hostId,
                    hostName = listing.hostName,
                    hostAvatarUrl = listing.hostProfileImage.ifBlank { null },
                    checkInDate = Timestamp(normalizedCheckIn),
                    checkOutDate = Timestamp(normalizedCheckOut),
                    numberOfGuests = _numberOfGuests.value,
                    totalPrice = _totalPrice.value
                )

                val result = reservationRepository.createReservation(reservation)
                result.onSuccess {
                    _toast.value = "Reservation created successfully!"
                    _reservationCreated.value = true
                }.onFailure { e ->
                    _toast.value = "Failed to create reservation: ${e.message}"
                }
            } catch (e: Exception) {
                _toast.value = "Failed to create reservation: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun consumeToast() {
        _toast.value = null
    }

    class Factory(private val listingId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateReservationViewModel(listingId) as T
        }
    }
}
