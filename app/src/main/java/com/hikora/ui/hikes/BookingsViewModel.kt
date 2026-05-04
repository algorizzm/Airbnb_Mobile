package com.hikora.ui.hikes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hikora.data.model.Booking
import com.hikora.data.repository.BookingRepository
import com.hikora.data.repository.HikeRepository
import com.hikora.data.session.UserSessionManager
import com.hikora.utils.BookingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UserBookingRow(
    val booking: Booking,
    val hikeTitle: String
)

data class MyBookingsUiState(
    val rows: List<UserBookingRow> = emptyList(),
    val message: String? = null
)

class BookingsViewModel(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val hikeRepository: HikeRepository = HikeRepository()
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MyBookingsUiState> = UserSessionManager.currentUser
        .flatMapLatest { user ->
            if (user == null || user.id.isBlank()) {
                flowOf(MyBookingsUiState())
            } else {
                combine(
                    bookingRepository.observeBookingsForUser(user.id),
                    hikeRepository.observeHikes(),
                    _message
                ) { bookings, hikes, msg ->
                    val rows = bookings.map { b ->
                        val title = hikes.firstOrNull { it.id == b.hikeId }?.title ?: "Hike"
                        UserBookingRow(b, title)
                    }
                    MyBookingsUiState(rows = rows, message = msg)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyBookingsUiState())

    fun cancelBooking(booking: Booking) {
        viewModelScope.launch {
            val uid = UserSessionManager.currentUser.value?.id ?: return@launch
            if (booking.userId != uid) {
                _message.value = "Not allowed."
                return@launch
            }
            if (booking.status != BookingStatus.PENDING && booking.status != BookingStatus.APPROVED) {
                _message.value = "This booking cannot be cancelled."
                return@launch
            }
            bookingRepository.cancelBooking(booking.id).onSuccess {
                _message.value = "Booking cancelled."
            }.onFailure {
                _message.value = it.message ?: "Could not cancel."
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}
