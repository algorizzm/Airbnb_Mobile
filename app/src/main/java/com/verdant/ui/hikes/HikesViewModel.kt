package com.verdant.ui.hikes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.data.repository.HikeRepository
import com.verdant.data.session.UserSessionManager
import com.verdant.utils.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HikesViewModel(
    private val hikeRepository: HikeRepository = HikeRepository()
) : ViewModel() {

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _hasActiveHike = MutableStateFlow(false)
    val hasActiveHike: StateFlow<Boolean> =
        _hasActiveHike.asStateFlow()

    val isGuide: StateFlow<Boolean> = UserSessionManager.currentUser
        .map { user ->
            user?.role == UserRole.GUIDE
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            false
        )

    init {
        observeCurrentHike()
    }

    private fun observeCurrentHike() {

        viewModelScope.launch {

            hikeRepository.observeHikes().collect { hikes ->

                val currentUserId =
                    UserSessionManager.currentUser.value?.id

                val currentUser =
                    UserSessionManager.currentUser.value

                if (currentUserId == null || currentUser == null) {

                    _hasActiveHike.value = false
                    return@collect
                }

                val hasActive = when (currentUser.role) {

                    UserRole.GUIDE -> {

                        hikes.any { hike ->
                            hike.guideId == currentUserId
                        }
                    }

                    UserRole.HIKER -> {

                        // TODO:
                        // Replace with joined-hike logic later
                        false
                    }

                    else -> false
                }

                _hasActiveHike.value = hasActive
            }
        }
    }

    fun consumeToast() {
        _toast.value = null
    }
}