package com.verdant.ui.hikes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.data.model.Hike
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

    private val _myHikes = MutableStateFlow<List<Hike>>(emptyList())
    val myHikes: StateFlow<List<Hike>> = _myHikes.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

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
        observeUserHikes()
    }

    private fun observeUserHikes() {

        viewModelScope.launch {

            hikeRepository.observeHikes().collect { hikes ->

                val currentUserId =
                    UserSessionManager.currentUser.value?.id

                if (currentUserId == null) {
                    _myHikes.value = emptyList()
                    return@collect
                }

                // TEMPORARY LOGIC
                // Replace later with:
                // booked hikes
                // hosted hikes
                // active hikes

                _myHikes.value = hikes.filter { hike ->

                    hike.guideId == currentUserId
                }
            }
        }
    }

    fun consumeToast() {
        _toast.value = null
    }
}