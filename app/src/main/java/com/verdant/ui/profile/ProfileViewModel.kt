package com.verdant.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.core.auth.AuthManager
import com.verdant.core.auth.AuthState
import com.verdant.data.model.AppNotification
import com.verdant.data.model.User
import com.verdant.data.remote.StorageService
import com.verdant.data.repository.BookingRepository
import com.verdant.data.repository.HikeRepository
import com.verdant.data.repository.NotificationRepository
import com.verdant.data.repository.UserRepository
import com.verdant.data.session.UserSessionManager
import com.verdant.ui.hikes.UserBookingRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ProfileUiState(
    val user: User? = null,
    val isGuest: Boolean = false,
    val avatarUploading: Boolean = false,
    val bannerUploading: Boolean = false,
    val notifications: List<AppNotification> = emptyList(),
    val unreadCount: Int = 0,
    val recentHikes: List<UserBookingRow> = emptyList(),
    val message: String? = null
)

class ProfileViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val storageService: StorageService = StorageService(),
    private val notifRepo: NotificationRepository = NotificationRepository(),
    private val bookingRepo: BookingRepository = BookingRepository(),
    private val hikeRepo: HikeRepository = HikeRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    // Legacy accessor
    val isGuest get() = _state.value.isGuest

    // Prevent duplicate collectors
    private var notificationsStarted = false
    private var hikesStarted = false

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

                    if (!hikesStarted) {
                        hikesStarted = true

                        observeRecentHikes()
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recent hikes
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeRecentHikes() {

        val uid = currentUid() ?: return

        viewModelScope.launch {

            combine(
                bookingRepo.observeBookingsForUser(uid),
                hikeRepo.observeHikes()
            ) { bookings, hikes ->

                bookings.take(3).map { booking ->

                    val hike = hikes.firstOrNull {
                        it.id == booking.hikeId
                    }

                    UserBookingRow(
                        booking = booking,
                        hikeTitle = hike?.title ?: "Hike",
                        hikeImageUrl = hike?.coverImageUrl() ?: "",
                        hikeLocation = hike?.summaryLocation()?.ifBlank { hike?.location } ?: ""
                    )
                }
            }.collect { rows ->

                _state.value = _state.value.copy(
                    recentHikes = rows
                )
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