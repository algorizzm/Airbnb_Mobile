package com.verdant.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.verdant.data.model.Hike
import com.verdant.data.repository.BookingRepository
import com.verdant.data.repository.HikeRepository
import com.verdant.data.repository.UserRepository
import com.verdant.data.session.UserSessionManager
import com.verdant.core.auth.AuthState
import com.verdant.utils.HikeStatus
import com.verdant.utils.Permissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HikeManagementUiState(
    val hike: Hike? = null,
    val message: String? = null,
    val canManage: Boolean = false,
    val canStart: Boolean = false,
    val canEnd: Boolean = false,
    val loading: Boolean = false
)

class HikeManagementViewModel(
    private val hikeId: String,
    private val hikeRepository: HikeRepository = HikeRepository(),
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HikeManagementUiState> = combine(
        hikeRepository.observeHike(hikeId),
        UserSessionManager.authState,
        _message
    ) { hike, authState, msg ->
        val user = (authState as? AuthState.Authenticated)?.user
        val canManage = Permissions.canManageHike(user?.role, hike?.guideId, user?.id)
        val canStart = canManage && hike?.status == HikeStatus.OPEN
        val canEnd = canManage && hike?.status == HikeStatus.ONGOING
        HikeManagementUiState(
            hike = hike,
            message = msg,
            canManage = canManage,
            canStart = canStart,
            canEnd = canEnd,
            loading = false
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HikeManagementUiState()
    )

    fun startHike() {
        viewModelScope.launch {
            val user = when (val state = UserSessionManager.authState.value) {
                AuthState.Guest -> {
                    _message.value = "Please log in to manage hikes."
                    return@launch
                }
                is AuthState.Loading -> {
                    _message.value = "Loading your account…"
                    return@launch
                }
                is AuthState.Authenticated -> {
                    state.user ?: run {
                        _message.value = "Finishing account setup…"
                        UserSessionManager.refresh()
                        return@launch
                    }
                }
            }
            val hike = hikeRepository.getHike(hikeId).getOrNull() ?: return@launch
            if (!Permissions.canStartHike(user.role, hike.guideId, user.id)) {
                _message.value = "Not allowed."
                return@launch
            }
            if (hike.status != HikeStatus.OPEN) {
                _message.value = "This hike is not open."
                return@launch
            }
            hikeRepository.startHike(hikeId).onSuccess {
                _message.value = "Hike started."
            }.onFailure {
                _message.value = it.message ?: "Could not start hike."
            }
        }
    }

    fun endHike() {
        viewModelScope.launch {
            val user = when (val state = UserSessionManager.authState.value) {
                AuthState.Guest -> {
                    _message.value = "Please log in to manage hikes."
                    return@launch
                }
                is AuthState.Loading -> {
                    _message.value = "Loading your account…"
                    return@launch
                }
                is AuthState.Authenticated -> {
                    state.user ?: run {
                        _message.value = "Finishing account setup…"
                        UserSessionManager.refresh()
                        return@launch
                    }
                }
            }
            val hike = hikeRepository.getHike(hikeId).getOrNull() ?: return@launch
            if (!Permissions.canEndHike(user.role, hike.guideId, user.id)) {
                _message.value = "Not allowed."
                return@launch
            }
            if (hike.status != HikeStatus.ONGOING) {
                _message.value = "This hike is not ongoing."
                return@launch
            }
            hikeRepository.completeHike(hikeId).onSuccess {
                bookingRepository.completeApprovedBookingsForHike(hikeId).onSuccess {
                    updateUserStatsForCompletedHike(hikeId, hike.effectiveDistanceKm())
                }.onFailure {
                    _message.value = "Hike completed but could not complete bookings: ${it.message}"
                }
            }.onFailure {
                _message.value = it.message ?: "Could not complete hike."
            }
        }
    }

    private suspend fun updateUserStatsForCompletedHike(hikeId: String, distance: Double) {
        bookingRepository.getApprovedBookingsForHike(hikeId).onSuccess { bookings ->
            bookings.forEach { booking ->
                userRepository.updateUserStats(
                    userId = booking.userId,
                    totalHikesAdd = 1,
                    totalDistanceAdd = distance,
                    summitsAdd = 1
                ).onFailure {
                    _message.value = "Could not update user stats: ${it.message}"
                }
            }
            _message.value = "Hike completed and all stats updated."
        }.onFailure {
            _message.value = "Could not get completed bookings: ${it.message}"
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    class Factory(
        private val hikeId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HikeManagementViewModel::class.java)) {
                return HikeManagementViewModel(hikeId) as T
            }
            error("Unknown ViewModel class")
        }
    }
}
