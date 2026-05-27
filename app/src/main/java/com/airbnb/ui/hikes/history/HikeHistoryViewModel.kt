package com.airbnb.ui.hikes.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Booking
import com.airbnb.data.repository.BookingRepository
import com.airbnb.data.repository.HikeRepository
import com.airbnb.data.session.UserSessionManager
import com.airbnb.core.auth.AuthState
import com.airbnb.utils.BookingStatus
import com.airbnb.utils.HikeStatus
import com.airbnb.utils.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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

@OptIn(ExperimentalCoroutinesApi::class)
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
                    } else if (user.role == UserRole.GUIDE) {
                        // Guide history: completed hikes where they were the guide
                        combine(
                            hikeRepository.observeHikesForGuide(user.id),
                            _message
                        ) { hikes, msg ->
                            val completedHikes = hikes.filter { it.status == HikeStatus.COMPLETED }
                            val rows = completedHikes.map { hike ->
                                // Create a synthetic "booking" row from the hike itself
                                // so we can reuse the same adapter/UI without a new layout
                                val syntheticBooking = Booking(
                                    id = hike.id,
                                    hikeId = hike.id,
                                    userId = user.id,
                                    userName = user.name,
                                    guideId = user.id,
                                    status = BookingStatus.COMPLETED
                                )
                                UserBookingRow(
                                    booking = syntheticBooking,
                                    hikeTitle = hike.title,
                                    hikeImageUrl = hike.coverImageUrl(),
                                    hikeLocation = hike.summaryLocation().ifBlank { hike.location }
                                )
                            }
                            MyBookingsUiState(
                                rows = rows,
                                loading = false,
                                authState = authState,
                                message = msg
                            )
                        }
                    } else {
                        // Hiker history: only COMPLETED bookings
                        combine(
                            bookingRepository.observeBookingsForUser(user.id),
                            hikeRepository.observeHikes(),
                            _message
                        ) { bookings, hikes, msg ->
                            val completedBookings = bookings.filter { b ->
                                b.status == BookingStatus.COMPLETED
                            }
                            val rows = completedBookings.map { b ->
                                val hike = hikes.firstOrNull { it.id == b.hikeId }
                                UserBookingRow(
                                    booking = b,
                                    hikeTitle = hike?.title ?: "Hike",
                                    hikeImageUrl = hike?.coverImageUrl() ?: "",
                                    hikeLocation = hike?.summaryLocation()?.ifBlank { hike?.location } ?: ""
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
