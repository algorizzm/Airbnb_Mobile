package com.airbnb.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Booking
import com.airbnb.data.model.Hike
import com.airbnb.data.model.User
import com.airbnb.data.repository.BookingRepository
import com.airbnb.data.repository.HikeRepository
import com.airbnb.data.repository.UserRepository
import com.airbnb.data.session.UserSessionManager
import com.airbnb.core.auth.AuthState
import com.airbnb.utils.BookingStatus
import com.airbnb.utils.HikeStatus
import com.airbnb.utils.Permissions
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
    val approvedCount: Int = 0,
    val message: String? = null,
    val canApply: Boolean = false,
    val showGuideActions: Boolean = false,
    val showApply: Boolean = false,
    val showCancelApplication: Boolean = false,
    val showLeaveHike: Boolean = false,
    val showStartHike: Boolean = false,
    val showEndHike: Boolean = false,
    val shouldPop: Boolean = false
)

class HikeDetailViewModel(
    private val hikeId: String,
    private val hikeRepository: HikeRepository = HikeRepository(),
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    private val _shouldPop = MutableStateFlow(false)

    val uiState: StateFlow<HikeDetailUiState> = combine(
        hikeRepository.observeHike(hikeId),
        UserSessionManager.currentUser
    ) { hike, user -> Pair(hike, user) }
        .flatMapLatest { (hike, user) ->
            val bookingsFlow = user?.id?.takeIf { it.isNotBlank() }?.let { id ->
                bookingRepository.observeBookingsForUser(id)
            } ?: flowOf(emptyList())

            val allHikeBookingsFlow = bookingRepository.observeBookingsForHike(hikeId)

            combine(bookingsFlow, allHikeBookingsFlow, _message, _shouldPop) { myBookings, allBookings, msg, pop ->
                val approvedCount = allBookings.count { it.status == com.airbnb.utils.BookingStatus.APPROVED }
                buildUiState(hike, user, myBookings, approvedCount, msg, pop)
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
        approvedCount: Int,
        message: String?,
        shouldPop: Boolean
    ): HikeDetailUiState {
        val authState = UserSessionManager.authState.value
        val role = user?.role
        val userId = user?.id
        val myBooking = myBookings.firstOrNull { b ->
            b.hikeId == hikeId &&
                (b.status == BookingStatus.PENDING || b.status == BookingStatus.APPROVED)
        }
        val showGuideActions = Permissions.canManageHike(role, hike?.guideId, userId)
        val canApplyLogic = hike != null &&
            hike.status == HikeStatus.OPEN &&
                Permissions.canApplyAsHiker(role, hike.guideId, userId)
        val showApply = canApplyLogic && myBooking == null
        val showCancelApplication = myBooking?.status == BookingStatus.PENDING
        val showLeaveHike = myBooking?.status == BookingStatus.APPROVED
        val showStartHike = showGuideActions && hike?.status == HikeStatus.OPEN
        val showEndHike = showGuideActions && hike?.status == HikeStatus.ONGOING

        val authLoading = authState is AuthState.Loading ||
            (authState is AuthState.Authenticated && authState.user == null)

        return HikeDetailUiState(
            hike = hike,
            loading = authLoading,
            myBooking = myBooking,
            approvedCount = approvedCount,
            message = message,
            canApply = canApplyLogic,
            showGuideActions = showGuideActions,
            showApply = showApply,
            showCancelApplication = showCancelApplication,
            showLeaveHike = showLeaveHike,
            showStartHike = showStartHike,
            showEndHike = showEndHike,
            shouldPop = shouldPop
        )
    }

    fun applyToHike() {
        viewModelScope.launch {
            val hike = hikeRepository.getHike(hikeId).getOrNull() ?: return@launch
            val user = when (val state = UserSessionManager.authState.value) {
                AuthState.Guest -> {
                    _message.value = "Please log in to apply."
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
            if (!Permissions.canApplyAsHiker(user.role, hike.guideId, user.id)) {
                _message.value = "Only Hikers can apply to hikes."
                return@launch
            }
            if (hike.status != HikeStatus.OPEN) {
                _message.value = "This hike is not open for applications."
                return@launch
            }
            bookingRepository.createPendingBooking(
                hikeId = hikeId,
                userId = user.id,
                userName = user.name.ifBlank { user.email },
                guideId = hike.guideId
            ).onSuccess {
                _message.value = "Application sent."
            }.onFailure {
                _message.value = it.message ?: "Could not apply."
            }
        }
    }

    fun deleteHike() {
        viewModelScope.launch {
            val user = when (val state = UserSessionManager.authState.value) {
                AuthState.Guest -> {
                    _message.value = "Please log in to continue."
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
            if (!Permissions.canManageHike(user.role, hike.guideId, user.id)) {
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
            val user = when (val state = UserSessionManager.authState.value) {
                AuthState.Guest -> {
                    _message.value = "Please log in to continue."
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

            val booking = bookingRepository.findActiveBookingForUserOnHike(
                hikeId,
                user.id
            ) ?: run {
                _message.value = "No active booking found."
                return@launch
            }
            bookingRepository.cancelBooking(booking.id).onSuccess {
                _message.value = "Booking updated."
            }.onFailure {
                _message.value = it.message ?: "Could not update booking."
            }
        }
    }

    fun leaveHike() {
        viewModelScope.launch {
            val hike = hikeRepository.getHike(hikeId).getOrNull() ?: return@launch
            val user = when (val state = UserSessionManager.authState.value) {
                AuthState.Guest -> {
                    _message.value = "Please log in to continue."
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

            val booking = bookingRepository.findActiveBookingForUserOnHike(
                hikeId,
                user.id
            ) ?: run {
                _message.value = "No active booking found."
                return@launch
            }
            if (booking.status != BookingStatus.APPROVED) {
                _message.value = "Only approved bookings can be left."
                return@launch
            }
            if (hike.status == HikeStatus.COMPLETED) {
                _message.value = "This hike has already completed."
                return@launch
            }
            bookingRepository.cancelBooking(booking.id).onSuccess {
                _message.value = "You have left the hike."
            }.onFailure {
                _message.value = it.message ?: "Could not leave hike."
            }
        }
    }

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
            // Enforce: guide may only have ONE ongoing hike at a time
            if (hikeRepository.hasOngoingHike(user.id)) {
                _message.value = "You already have an ongoing hike. End it before starting another."
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
            // Fetch approved bookings BEFORE completing them (status will change after)
            val approvedBookings = bookingRepository
                .getApprovedBookingsForHike(hikeId)
                .getOrElse { emptyList() }

            hikeRepository.completeHike(hikeId).onSuccess {
                bookingRepository.completeApprovedBookingsForHike(hikeId).onSuccess {
                    updateUserStats(approvedBookings, hike.effectiveDistanceKm())
                }.onFailure {
                    _message.value = "Hike completed but could not finalise bookings: ${it.message}"
                }
            }.onFailure {
                _message.value = it.message ?: "Could not complete hike."
            }
        }
    }

    private suspend fun updateUserStats(approvedBookings: List<Booking>, distance: Double) {
        approvedBookings.forEach { booking ->
            userRepository.updateUserStats(
                userId = booking.userId,
                totalHikesAdd = 1,
                totalDistanceAdd = distance,
                summitsAdd = 1
            ).onFailure {
                _message.value = "Could not update stats for ${booking.userName}: ${it.message}"
            }
        }
        _message.value = "Hike completed. ${approvedBookings.size} participant(s) updated."
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
