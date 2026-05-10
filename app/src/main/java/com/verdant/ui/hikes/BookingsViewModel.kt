package com.verdant.ui.hikes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.data.model.Booking
import com.verdant.data.repository.BookingRepository
import com.verdant.data.repository.HikeRepository
import com.verdant.data.session.UserSessionManager
import com.verdant.core.auth.AuthState
import com.verdant.utils.BookingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UserBookingRow(
    val booking: Booking,
    val hikeTitle: String,
    val hikeImageUrl: String = "",
    val hikeLocation: String = ""
)

data class MyBookingsUiState(
    val rows: List<UserBookingRow> = emptyList(),
    val loading: Boolean = false,
    val authState: AuthState = AuthState.Guest,
    val message: String? = null
)

class BookingsViewModel(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val hikeRepository: HikeRepository = HikeRepository()
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MyBookingsUiState> = UserSessionManager.authState
        .flatMapLatest { authState ->
            when (authState) {
                AuthState.Guest -> _message.map { msg ->
                    MyBookingsUiState(
                        rows = emptyList(),
                        loading = false,
                        authState = authState,
                        message = msg
                    )
                }

                is AuthState.Loading -> _message.map { msg ->
                    MyBookingsUiState(
                        rows = emptyList(),
                        loading = true,
                        authState = authState,
                        message = msg
                    )
                }

                is AuthState.Authenticated -> {
                    val user = authState.user
                    if (user == null || user.id.isBlank()) {
                        _message.map { msg ->
                            MyBookingsUiState(
                                rows = emptyList(),
                                loading = true,
                                authState = authState,
                                message = msg
                            )
                        }
                    } else {
                        combine(
                            bookingRepository.observeBookingsForUser(user.id),
                            hikeRepository.observeHikes(),
                            _message
                        ) { bookings, hikes, msg ->
                            val rows = bookings.map { b ->
                                val hike = hikes.firstOrNull { it.id == b.hikeId }
                                UserBookingRow(
                                    booking = b,
                                    hikeTitle = hike?.title ?: "Hike",
                                    hikeImageUrl = hike?.imageUrl ?: "",
                                    hikeLocation = hike?.location ?: ""
                                )
                            }
                            MyBookingsUiState(
                                rows = rows,
                                loading = false,
                                authState = authState,
                                message = msg
                            )
                        }
                    }
                }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MyBookingsUiState()
        )

    fun cancelBooking(booking: Booking) {
        viewModelScope.launch {
            val uid = when (val state = UserSessionManager.authState.value) {
                AuthState.Guest -> {
                    _message.value = "Please log in to manage bookings."
                    return@launch
                }
                is AuthState.Loading -> {
                    _message.value = "Loading your account…"
                    return@launch
                }
                is AuthState.Authenticated -> {
                    state.user?.id ?: run {
                        _message.value = "Finishing account setup…"
                        UserSessionManager.refresh()
                        return@launch
                    }
                }
            }
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
