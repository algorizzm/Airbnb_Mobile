package com.airbnb.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Listing
import com.airbnb.data.repository.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExploreViewModel(
    private val listingRepository: ListingRepository = ListingRepository()
) : ViewModel() {

    private val _allListings =
        MutableStateFlow<List<Listing>>(emptyList())

    private val _searchQuery =
        MutableStateFlow("")

    val searchQuery: StateFlow<String> =
        _searchQuery.asStateFlow()

    private val _maxPrice =
        MutableStateFlow<Double?>(null)

    private val _minGuests =
        MutableStateFlow<Int?>(null)

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _toast =
        MutableStateFlow<String?>(null)

    val toast: StateFlow<String?> =
        _toast.asStateFlow()

    val displayListings: StateFlow<List<Listing>> = combine(
        _allListings,
        _searchQuery,
        _maxPrice,
        _minGuests
    ) { listings, query, maxPrice, minGuests ->

        listings.filter { listing ->

            val q = query.trim()

            val matchesQuery =
                q.isEmpty() ||
                        listing.title.contains(
                            q,
                            ignoreCase = true
                        ) ||
                        listing.location.contains(
                            q,
                            ignoreCase = true
                        )

            val matchesPrice =
                maxPrice == null ||
                        listing.pricePerNight <= maxPrice

            val matchesGuests =
                minGuests == null ||
                        listing.maxGuests >= minGuests

            matchesQuery && matchesPrice && matchesGuests
        }

    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    init {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                listingRepository.observeListings()
                    .collect { listings ->
                        _allListings.value = listings
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _toast.value = "Failed to load listings: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun setMaxPrice(value: Double?) {
        _maxPrice.value = value
    }

    fun setMinGuests(value: Int?) {
        _minGuests.value = value
    }

    fun consumeToast() {
        _toast.value = null
    }
}