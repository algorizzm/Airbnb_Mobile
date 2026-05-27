package com.airbnb.ui.hikes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Booking
import com.airbnb.data.model.Hike
import com.airbnb.data.repository.BookingRepository
import com.airbnb.data.repository.HikeRepository
import com.airbnb.data.session.UserSessionManager
import com.airbnb.core.auth.AuthState
import com.airbnb.utils.BookingStatus
import com.airbnb.utils.HikeStatus
import com.airbnb.utils.Permissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ApplicantsUiState(
    val hike: Hike? = null,
    val bookings: List<Booking> = emptyList(),
    val message: String? = null,
    val canManage: Boolean = false,
    val authState: AuthState = AuthState.Guest
)

class ApplicantsViewModel(
    private val hikeId: String,
    private val hikeRepository: HikeRepository = HikeRepository(),
    private val bookingRepository: BookingRepository = BookingRepository()
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ApplicantsUiState> = combine(
        hikeRepository.observeHike(hikeId),
        bookingRepository.observeBookingsForHike(hikeId),
        UserSessionManager.authState,
        _message
    ) { hike, bookings, authState, msg ->
        val user = (authState as? AuthState.Authenticated)?.user
        val canManage = Permissions.canReviewApplicants(user?.role, hike?.guideId, user?.id)
        ApplicantsUiState(
            hike = hike,
            bookings = bookings,
            message = msg,
            canManage = canManage,
            authState = authState
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ApplicantsUiState()
    )

    fun approve(bookingId: String) {
        viewModelScope.launch {
            val user = when (val state = UserSessionManager.authState.value) {
                AuthState.Guest -> {
                    _message.value = "Please log in to manage applicants."
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
            if (!Permissions.canReviewApplicants(user.role, hike.guideId, user.id)) {
                _message.value = "Not allowed."
                return@launch
            }
            val approved = bookingRepository.countApprovedForHike(hikeId)
            if (approved >= hike.maxParticipants) {
                _message.value = "Maximum participants reached."
                return@launch
            }
            bookingRepository.updateBookingStatus(bookingId, BookingStatus.APPROVED).onSuccess {
                val newCount = bookingRepository.countApprovedForHike(hikeId)
                val h = hikeRepository.getHike(hikeId).getOrNull()
                if (h != null && newCount >= h.maxParticipants &&
                    h.status.equals(HikeStatus.OPEN, ignoreCase = true)
                ) {
                    hikeRepository.updateHikeStatus(hikeId, HikeStatus.FULL)
                }
                _message.value = "Applicant approved."
            }.onFailure {
                _message.value = it.message ?: "Could not approve."
            }
        }
    }

    fun reject(bookingId: String) {
        viewModelScope.launch {
            val user = when (val state = UserSessionManager.authState.value) {
                AuthState.Guest -> {
                    _message.value = "Please log in to manage applicants."
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
            if (!Permissions.canReviewApplicants(user.role, hike.guideId, user.id)) {
                _message.value = "Not allowed."
                return@launch
            }
            bookingRepository.updateBookingStatus(bookingId, BookingStatus.REJECTED).onSuccess {
                _message.value = "Applicant rejected."
            }.onFailure {
                _message.value = it.message ?: "Could not reject."
            }
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
            if (modelClass.isAssignableFrom(ApplicantsViewModel::class.java)) {
                return ApplicantsViewModel(hikeId) as T
            }
            error("Unknown ViewModel class")
        }
    }
}
