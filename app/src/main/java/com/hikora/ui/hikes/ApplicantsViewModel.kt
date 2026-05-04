package com.hikora.ui.hikes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hikora.data.model.Booking
import com.hikora.data.model.Hike
import com.hikora.data.repository.BookingRepository
import com.hikora.data.repository.HikeRepository
import com.hikora.data.session.UserSessionManager
import com.hikora.utils.BookingStatus
import com.hikora.utils.HikingRbac
import com.hikora.utils.UserRole
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
    val canManage: Boolean = false
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
        UserSessionManager.currentUser,
        _message
    ) { hike, bookings, user, msg ->
        val role = user?.role ?: UserRole.CLIENT
        val canManage = HikingRbac.canReviewApplicants(role, hike?.guideId, user?.id)
        ApplicantsUiState(
            hike = hike,
            bookings = bookings,
            message = msg,
            canManage = canManage
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ApplicantsUiState()
    )

    fun approve(bookingId: String) {
        viewModelScope.launch {
            val user = UserSessionManager.currentUser.value ?: return@launch
            val hike = hikeRepository.getHike(hikeId).getOrNull() ?: return@launch
            if (!HikingRbac.canReviewApplicants(user.role, hike.guideId, user.id)) {
                _message.value = "Not allowed."
                return@launch
            }
            val approved = bookingRepository.countApprovedForHike(hikeId)
            if (approved >= hike.maxParticipants) {
                _message.value = "Maximum participants reached."
                return@launch
            }
            bookingRepository.updateBookingStatus(bookingId, BookingStatus.APPROVED).onSuccess {
                _message.value = "Applicant approved."
            }.onFailure {
                _message.value = it.message ?: "Could not approve."
            }
        }
    }

    fun reject(bookingId: String) {
        viewModelScope.launch {
            val user = UserSessionManager.currentUser.value ?: return@launch
            val hike = hikeRepository.getHike(hikeId).getOrNull() ?: return@launch
            if (!HikingRbac.canReviewApplicants(user.role, hike.guideId, user.id)) {
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
