package com.airbnb.ui.hikes.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Hike
import com.airbnb.data.repository.HikeRepository
import com.airbnb.data.session.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EventsFragmentViewModel(
    private val hikeRepository: HikeRepository = HikeRepository()
) : ViewModel() {

    private val _events =
        MutableStateFlow<List<Hike>>(emptyList())

    val events: StateFlow<List<Hike>> =
        _events.asStateFlow()

    private val _toast =
        MutableStateFlow<String?>(null)

    val toast: StateFlow<String?> =
        _toast.asStateFlow()

    init {
        observeEvents()
    }

    private fun observeEvents() {

        viewModelScope.launch {

            hikeRepository.observeHikes().collect { hikes ->

                val currentUserId =
                    UserSessionManager.currentUser.value?.id

                if (currentUserId == null) {

                    _events.value = emptyList()
                    return@collect
                }

                _events.value = hikes.filter { hike ->

                    hike.guideId == currentUserId
                }
            }
        }
    }

    fun consumeToast() {
        _toast.value = null
    }
}