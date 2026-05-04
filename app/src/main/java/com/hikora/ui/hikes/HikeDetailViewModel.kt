package com.hikora.ui.hikes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hikora.data.model.Booking
import com.hikora.data.model.Hike
import com.hikora.data.model.User
import com.hikora.data.repository.BookingRepository
import com.hikora.data.repository.HikeRepository
import com.hikora.data.session.UserSessionManager
import com.hikora.utils.BookingStatus
import com.hikora.utils.HikeStatus
import com.hikora.utils.HikingRbac
import com.hikora.utils.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HikeDetailUiState(
    val hike: Hike? = null,
    val loading: Boolean = true,
    val myBooking: Booking? = null,
    val message: String? = null,
    val canApply: Boolean = false,
    val showGuideActions: Boolean = false,
    val showApply: Boolean = false,
    val showCancelApplication: Boolean = false,
    val showLeaveHike: Boolean = false,
    val shouldPop: Boolean = false
)

class HikeDetailViewModel(
    private val hikeId: String,
    private val hikeRepository: HikeRepository = HikeRepository(),
    private val bookingRepository: BookingRepository = BookingRepository()
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    private val _shouldPop = MutableStateFlow(false)

    private val uid: String?
        get() = UserSessionManager.currentUser.value?.id?.ifBlank { null }

    val uiState: StateFlow<HikeDetailUiState> = combine(
        hikeRepository.observeHike(hikeId),
        UserSessionManager.currentUser
    ) { hike, user -> Pair(hike, user) }
        .flatMapLatest { (hike, user) ->
            val bookingsFlow = user?.id?.takeIf { it.isNotBlank() }?.let { id ->
                bookingRepository.observeBookingsForUser(id)
            } ?: flowOf(emptyList())

            combine(bookingsFlow, _message, _shouldPop) { bookings, msg, pop ->
                buildUiState(hike, user, bookings, msg, pop)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HikeDetailUiState(loading = true)
        )

    init {
        viewModelScope.launch {
            if (hikeRepository.getHike(hikeId).isFailure) {
                _message.value = "Could not load hike."
            }
        }
    }

    private fun buildUiState(
        hike: Hike?,
        user: User?,
        myBookings: List<Booking>,
        message: String?,
        shouldPop: Boolean
    ): HikeDetailUiState {
        val role = user?.role ?: UserRole.CLIENT
        val userId = user?.id
        val myBooking = myBookings.firstOrNull { b ->
            b.hikeId == hikeId &&
                (b.status == BookingStatus.PENDING || b.status == BookingStatus.APPROVED)
        }
        val showGuideActions = HikingRbac.canManageHike(role, hike?.guideId, userId)
        val canApplyLogic = hike != null &&
            hike.status == HikeStatus.OPEN &&
            HikingRbac.canApplyAsClient(role, hike.guideId, userId)
        val showApply = canApplyLogic && myBooking == null
        val showCancelApplication = myBooking?.status == BookingStatus.PENDING
        val showLeaveHike = myBooking?.status == BookingStatus.APPROVED

        return HikeDetailUiState(
            hike = hike,
            loading = false,
            myBooking = myBooking,
            message = message,
            canApply = canApplyLogic,
            showGuideActions = showGuideActions,
            showApply = showApply,
            showCancelApplication = showCancelApplication,
            showLeaveHike = showLeaveHike,
            shouldPop = shouldPop
        )
    }

    fun applyToHike() {
        viewModelScope.launch {
            val hike = hikeRepository.getHike(hikeId).getOrNull() ?: return@launch
            val user = UserSessionManager.currentUser.value ?: return@launch
            if (!HikingRbac.canApplyAsClient(user.role, hike.guideId, user.id)) {
                _message.value = "Only clients can apply to hikes."
                return@launch
            }
            if (hike.status != HikeStatus.OPEN) {
                _message.value = "This hike is not open for applications."
                return@launch
            }
            bookingRepository.createPendingBooking(
                hikeId = hikeId,
                userId = user.id,
                userName = user.name.ifBlank { user.email }
            ).onSuccess {
                _message.value = "Application sent."
            }.onFailure {
                _message.value = it.message ?: "Could not apply."
            }
        }
    }

    fun deleteHike() {
        viewModelScope.launch {
            val user = UserSessionManager.currentUser.value ?: return@launch
            val hike = hikeRepository.getHike(hikeId).getOrNull() ?: return@launch
            if (!HikingRbac.canManageHike(user.role, hike.guideId, user.id)) {
                _message.value = "Not allowed."
                return@launch
            }
            hikeRepository.deleteHike(hikeId).onSuccess {
                _shouldPop.value = true
            }.onFailure {
                _message.value = it.message ?: "Could not delete hike."
            }
        }
    }

    fun cancelMyBooking() {
        viewModelScope.launch {
            val booking = bookingRepository.findActiveBookingForUserOnHike(
                hikeId,
                uid ?: return@launch
            ) ?: return@launch
            bookingRepository.cancelBooking(booking.id).onSuccess {
                _message.value = "Booking updated."
            }.onFailure {
                _message.value = it.message ?: "Could not update booking."
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
            if (modelClass.isAssignableFrom(HikeDetailViewModel::class.java)) {
                return HikeDetailViewModel(hikeId) as T
            }
            error("Unknown ViewModel class")
        }
    }
}
