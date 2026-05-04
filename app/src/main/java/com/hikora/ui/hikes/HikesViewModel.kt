package com.hikora.ui.hikes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hikora.data.model.Hike
import com.hikora.data.repository.HikeRepository
import com.hikora.data.session.UserSessionManager
import com.hikora.utils.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HikesViewModel(
    private val hikeRepository: HikeRepository = HikeRepository()
) : ViewModel() {

    private val _allHikes = MutableStateFlow<List<Hike>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _difficultyFilter = MutableStateFlow<String?>(null)
    val difficultyFilter: StateFlow<String?> = _difficultyFilter.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    val displayHikes: StateFlow<List<Hike>> = combine(
        _allHikes,
        _searchQuery,
        _difficultyFilter
    ) { hikes, query, difficulty ->
        hikes.filter { hike ->
            val q = query.trim()
            val matchesQuery = q.isEmpty() ||
                hike.title.contains(q, ignoreCase = true) ||
                hike.location.contains(q, ignoreCase = true)
            val matchesDifficulty = difficulty.isNullOrBlank() ||
                hike.difficulty.equals(difficulty, ignoreCase = true)
            matchesQuery && matchesDifficulty
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isGuide: StateFlow<Boolean> = UserSessionManager.currentUser
        .map { it?.role == UserRole.GUIDE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch {
            hikeRepository.observeHikes().collect { list ->
                _allHikes.value = list
            }
        }
    }

    fun setSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun setDifficultyFilter(value: String?) {
        _difficultyFilter.value = value
    }

    fun consumeToast() {
        _toast.value = null
    }
}
