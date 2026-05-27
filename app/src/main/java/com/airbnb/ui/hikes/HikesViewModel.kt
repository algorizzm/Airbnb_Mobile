package com.airbnb.ui.hikes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.data.repository.BookingRepository
import com.airbnb.data.repository.HikeRepository
import com.airbnb.data.session.UserSessionManager
import com.airbnb.utils.HikeStatus
import com.airbnb.utils.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.airbnb.data.model.Hike
import com.airbnb.data.repository.UserRepository

data class TrackUiState(
    val activeHike: Hike? = null,
    val approvedCount: Int = 0,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class HikesViewModel(
    private val hikeRepository: HikeRepository = HikeRepository(),
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    val isGuide: StateFlow<Boolean> = UserSessionManager.currentUser
        .map { user -> user?.role == UserRole.GUIDE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val uiState: StateFlow<TrackUiState> = UserSessionManager.currentUser
        .flatMapLatest { user ->
            if (user == null) return@flatMapLatest flowOf(TrackUiState(isLoading = false))

            when (user.role) {
                UserRole.GUIDE -> {
                    hikeRepository.observeOngoingHikeForGuide(user.id)
                        .flatMapLatest { hike ->
                            if (hike == null) {
                                flowOf(TrackUiState(isLoading = false))
                            } else {
                                bookingRepository.observeBookingsForHike(hike.id).map { bookings ->
                                    val count = bookings.count { it.status == com.airbnb.utils.BookingStatus.APPROVED }
                                    TrackUiState(activeHike = hike, approvedCount = count, isLoading = false)
                                }
                            }
                        }
                }

                UserRole.HIKER -> {
                    combine(
                        bookingRepository.observeApprovedBookingsForUser(user.id),
                        hikeRepository.observeHikes()
                    ) { approvedBookings, allHikes ->
                        val ongoingHike = allHikes.firstOrNull { it.status.equals(HikeStatus.ONGOING, ignoreCase = true) && approvedBookings.any { b -> b.hikeId == it.id } }
                        ongoingHike
                    }.flatMapLatest { hike ->
                        if (hike == null) {
                            flowOf(TrackUiState(isLoading = false))
                        } else {
                            bookingRepository.observeBookingsForHike(hike.id).map { bookings ->
                                val count = bookings.count { it.status == com.airbnb.utils.BookingStatus.APPROVED }
                                TrackUiState(activeHike = hike, approvedCount = count, isLoading = false)
                            }
                        }
                    }
                }

                else -> flowOf(TrackUiState(isLoading = false))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackUiState())

    val activeHikeId: StateFlow<String?> = uiState.map { it.activeHike?.id }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val hasActiveHike: StateFlow<Boolean> = uiState.map { it.activeHike != null }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun consumeToast() {
        _toast.value = null
    }

    fun endActiveHike() {
        val state = uiState.value
        val hikeId = state.activeHike?.id ?: return
        
        viewModelScope.launch {
            val approvedBookings = bookingRepository
                .getApprovedBookingsForHike(hikeId)
                .getOrElse { emptyList() }

            hikeRepository.completeHike(hikeId).onSuccess {
                bookingRepository.completeApprovedBookingsForHike(hikeId).onSuccess {
                    val distance = state.activeHike.effectiveDistanceKm()
                    approvedBookings.forEach { booking ->
                        userRepository.updateUserStats(
                            userId = booking.userId,
                            totalHikesAdd = 1,
                            totalDistanceAdd = distance,
                            summitsAdd = 1
                        )
                    }
                    _toast.value = "Hike completed."
                }.onFailure {
                    _toast.value = "Hike completed but could not finalise bookings: ${it.message}"
                }
            }.onFailure {
                _toast.value = it.message ?: "Could not complete hike."
            }
        }
    }
}