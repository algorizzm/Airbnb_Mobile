package com.airbnb.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.core.auth.AuthManager
import com.airbnb.core.auth.AuthState
import com.airbnb.data.model.AppNotification
import com.airbnb.data.model.User
import com.airbnb.data.model.Reservation
import com.airbnb.data.remote.StorageService
import com.airbnb.data.repository.NotificationRepository
import com.airbnb.data.repository.ReservationRepository
import com.airbnb.data.repository.UserRepository
import com.airbnb.data.session.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecentTrip(
    val reservation: Reservation,
    val listingTitle: String,
    val listingImageUrl: String,
    val listingLocation: String
)

data class ProfileUiState(
    val user: User? = null,
    val isGuest: Boolean = false,
    val avatarUploading: Boolean = false,
    val bannerUploading: Boolean = false,
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val recentTrips: List<RecentTrip> = emptyList(),
    val message: String? = null
)

class ProfileViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val storageService: StorageService = StorageService(),
    private val notifRepo: NotificationRepository = NotificationRepository(),
    private val reservationRepo: ReservationRepository = ReservationRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    // Legacy accessor
    val isGuest get() = _state.value.isGuest

    // Prevent duplicate collectors
    private var notificationsStarted = false
    private var tripsStarted = false

    init {
        observeSession()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Session / User
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeSession() {

        viewModelScope.launch {

            UserSessionManager.currentUser.collect { user ->

                _state.value = _state.value.copy(
                    user = user,
                    isGuest = user == null
                )

                if (user != null) {

                    if (!notificationsStarted) {
                        notificationsStarted = true

                        observeNotifications()
                        observeUnreadCount()
                    }

                    if (!tripsStarted) {
                        tripsStarted = true

                        observeRecentTrips()
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recent trips (reservations)
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeRecentTrips() {
        val uid = currentUid() ?: return
        
        viewModelScope.launch {
            // Show recent completed reservations for guests
            reservationRepo.observeReservationsForGuest(uid).collect { reservations ->
                val recentTrips = reservations
                    .filter { it.status == "completed" }
                    .take(3)
                    .map { reservation ->
                        RecentTrip(
                            reservation = reservation,
                            listingTitle = reservation.listingTitle,
                            listingImageUrl = reservation.listingImageUrl,
                            listingLocation = "" // Location not stored in reservation, could be enhanced
                        )
                    }
                _state.value = _state.value.copy(recentTrips = recentTrips)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notifications
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeNotifications() {

        val uid = currentUid() ?: return

        viewModelScope.launch {

            notifRepo.observeNotifications(uid)
                .collect { list ->

                    _state.value = _state.value.copy(
                        notifications = list
                    )
                }
        }
    }

    private fun observeUnreadCount() {

        val uid = currentUid() ?: return

        viewModelScope.launch {

            notifRepo.unreadCount(uid)
                .collect { count ->

                    _state.value = _state.value.copy(
                        unreadCount = count
                    )
                }
        }
    }

    fun markAllNotificationsRead() {

        val uid = currentUid() ?: return

        viewModelScope.launch {
            notifRepo.markAllRead(uid)
        }
    }

    fun markNotificationRead(notificationId: String) {

        val uid = currentUid() ?: return

        viewModelScope.launch {
            notifRepo.markRead(uid, notificationId)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Avatar upload
    // ─────────────────────────────────────────────────────────────────────────

    fun uploadAvatar(uri: Uri) {

        val uid = currentUid() ?: return

        viewModelScope.launch {

            _state.value = _state.value.copy(
                avatarUploading = true
            )

            storageService.uploadAvatar(uid, uri)
                .onSuccess { url ->

                    userRepo.updateAvatar(uid, url)

                    _state.value = _state.value.copy(
                        avatarUploading = false
                    )
                }
                .onFailure {

                    _state.value = _state.value.copy(
                        avatarUploading = false,
                        message = "Avatar upload failed: ${it.message}"
                    )
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Banner upload
    // ─────────────────────────────────────────────────────────────────────────

    fun uploadBanner(uri: Uri) {

        val uid = currentUid() ?: return

        viewModelScope.launch {

            _state.value = _state.value.copy(
                bannerUploading = true
            )

            storageService.uploadBanner(uid, uri)
                .onSuccess { url ->

                    userRepo.updateBanner(uid, url)

                    _state.value = _state.value.copy(
                        bannerUploading = false
                    )
                }
                .onFailure {

                    _state.value = _state.value.copy(
                        bannerUploading = false,
                        message = "Banner upload failed: ${it.message}"
                    )
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bio edit
    // ─────────────────────────────────────────────────────────────────────────

    fun updateBio(bio: String) {
        val uid = currentUid() ?: return
        viewModelScope.launch {
            userRepo.updateBio(uid, bio)
                .onSuccess {
                    // Optimistically update local state immediately
                    val updated = _state.value.user?.copy(bio = bio)
                    _state.value = _state.value.copy(user = updated)
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        message = "Failed to save bio: ${it.message}"
                    )
                }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Misc
    // ─────────────────────────────────────────────────────────────────────────

    fun consumeMessage() {

        _state.value = _state.value.copy(
            message = null
        )
    }

    private fun currentUid(): String? =
        (AuthManager.stateSnapshot() as? AuthState.Authenticated)?.uid
}