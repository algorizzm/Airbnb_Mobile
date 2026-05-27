package com.airbnb.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Hike
import com.airbnb.data.repository.HikeRepository
import com.airbnb.utils.HikeStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val hikeRepository: HikeRepository = HikeRepository()
) : ViewModel() {

    private val _allHikes =
        MutableStateFlow<List<Hike>>(emptyList())

    private val _searchQuery =
        MutableStateFlow("")

    val searchQuery: StateFlow<String> =
        _searchQuery.asStateFlow()

    private val _difficultyFilter =
        MutableStateFlow<String?>(null)

    val difficultyFilter: StateFlow<String?> =
        _difficultyFilter.asStateFlow()

    private val _minDistance =
        MutableStateFlow<Double?>(null)

    private val _maxDistance =
        MutableStateFlow<Double?>(null)

    private val _maxPrice =
        MutableStateFlow<Double?>(null)

    private val _maxDuration =
        MutableStateFlow<Double?>(null)

    private val _toast =
        MutableStateFlow<String?>(null)

    val toast: StateFlow<String?> =
        _toast.asStateFlow()

    val displayHikes: StateFlow<List<Hike>> = combine(

        _allHikes,
        _searchQuery,
        _difficultyFilter,

        combine(
            _minDistance,
            _maxDistance,
            _maxPrice,
            _maxDuration
        ) { minD, maxD, maxP, maxDur ->

            FilterState(
                minD,
                maxD,
                maxP,
                maxDur
            )
        }

    ) { hikes, query, difficulty, filters ->

        hikes.filter { hike ->

            val discoverable =
                hike.status.equals(HikeStatus.OPEN, ignoreCase = true) ||
                    hike.status.equals(HikeStatus.FULL, ignoreCase = true)
            // Exclude ONGOING, COMPLETED, CANCELLED, DRAFT from Explore
            if (!discoverable) return@filter false

            val q = query.trim()
            val locLabel = hike.summaryLocation().ifBlank { hike.location }

            val matchesQuery =
                q.isEmpty() ||
                        hike.title.contains(
                            q,
                            ignoreCase = true
                        ) ||
                        locLabel.contains(
                            q,
                            ignoreCase = true
                        )

            val matchesDifficulty =
                difficulty.isNullOrBlank() ||
                        hike.difficulty.equals(
                            difficulty,
                            ignoreCase = true
                        )

            val effDist = hike.effectiveDistanceKm()

            val matchesMinDist =
                filters.minDistance == null ||
                        effDist >= filters.minDistance

            val matchesMaxDist =
                filters.maxDistance == null ||
                        effDist <= filters.maxDistance

            val matchesPrice =
                filters.maxPrice == null ||
                        hike.price <= filters.maxPrice

            val matchesDuration =
                filters.maxDuration == null ||
                        hike.durationHours <= filters.maxDuration

            matchesQuery &&
                    matchesDifficulty &&
                    matchesMinDist &&
                    matchesMaxDist &&
                    matchesPrice &&
                    matchesDuration
        }

    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    init {

        viewModelScope.launch {

            hikeRepository.observeHikes()
                .collect { hikes ->

                    _allHikes.value = hikes
                }
        }
    }

    fun setSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun setDifficultyFilter(value: String?) {
        _difficultyFilter.value = value
    }

    fun setMinDistance(value: Double?) {
        _minDistance.value = value
    }

    fun setMaxDistance(value: Double?) {
        _maxDistance.value = value
    }

    fun setMaxPrice(value: Double?) {
        _maxPrice.value = value
    }

    fun setMaxDuration(value: Double?) {
        _maxDuration.value = value
    }

    fun consumeToast() {
        _toast.value = null
    }

    private data class FilterState(
        val minDistance: Double?,
        val maxDistance: Double?,
        val maxPrice: Double?,
        val maxDuration: Double?
    )
}