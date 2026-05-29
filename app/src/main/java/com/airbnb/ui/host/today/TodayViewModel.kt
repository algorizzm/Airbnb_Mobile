package com.airbnb.ui.host.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.core.auth.AuthManager
import com.airbnb.data.model.Reservation
import com.airbnb.data.model.ReservationStatus
import com.airbnb.data.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for the Today screen.
 * 
 * Responsibilities:
 * - Observe all host reservations
 * - Group reservations into sections based on date and status
 * - Expose clean UI state for section-based rendering
 * 
 * Sections:
 * - Today's Check-Ins: Reservations checking in today
 * - Today's Check-Outs: Reservations checking out today
 * - Active Stays: Currently checked-in guests
 * - Upcoming Reservations: Future reservations sorted by date
 */
class TodayViewModel(
    private val repository: ReservationRepository = ReservationRepository()
) : ViewModel() {

    // =========================================================
    // STATE
    // =========================================================

    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    // =========================================================
    // INITIALIZATION
    // =========================================================

    init {
        loadReservations()
    }

    // =========================================================
    // LOAD RESERVATIONS
    // =========================================================

    /**
     * Loads all reservations for the current host and groups them into sections.
     */
    fun loadReservations() {
        val hostId = AuthManager.currentUserId()
        if (hostId == null) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "User not authenticated"
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            repository.observeReservationsForHost(hostId)
                .catch { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load reservations"
                    )
                }
                .collect { reservations ->
                    val grouped = groupReservations(reservations)
                    _state.value = TodayUiState(
                        todaysCheckIns = grouped.todaysCheckIns,
                        todaysCheckOuts = grouped.todaysCheckOuts,
                        activeStays = grouped.activeStays,
                        upcomingReservations = grouped.upcomingReservations,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    // =========================================================
    // GROUPING LOGIC
    // =========================================================

    /**
     * Groups reservations into sections based on date and status.
     */
    private fun groupReservations(reservations: List<Reservation>): GroupedReservations {
        val today = getTodayMidnight()

        // Today's Check-Ins
        val todaysCheckIns = reservations.filter { reservation ->
            val checkInDate = reservation.checkInDate?.toDate() ?: return@filter false
            isSameDay(checkInDate, today) &&
                    reservation.status in listOf(ReservationStatus.UPCOMING, ReservationStatus.CONFIRMED) &&
                    !reservation.checkedIn
        }.sortedBy { it.checkInDate?.toDate() }

        // Today's Check-Outs
        val todaysCheckOuts = reservations.filter { reservation ->
            val checkOutDate = reservation.checkOutDate?.toDate() ?: return@filter false
            isSameDay(checkOutDate, today) &&
                    reservation.status == ReservationStatus.ACTIVE_STAY &&
                    reservation.checkedIn &&
                    !reservation.checkedOut
        }.sortedBy { it.checkOutDate?.toDate() }

        // Active Stays
        val activeStays = reservations.filter { reservation ->
            reservation.checkedIn &&
                    !reservation.checkedOut &&
                    reservation.status == ReservationStatus.ACTIVE_STAY
        }.sortedBy { it.checkOutDate?.toDate() }

        // Upcoming Reservations
        val upcomingReservations = reservations.filter { reservation ->
            val checkInDate = reservation.checkInDate?.toDate() ?: return@filter false
            checkInDate.after(today) &&
                    reservation.status in listOf(ReservationStatus.UPCOMING, ReservationStatus.CONFIRMED)
        }.sortedBy { it.checkInDate?.toDate() }

        return GroupedReservations(
            todaysCheckIns = todaysCheckIns,
            todaysCheckOuts = todaysCheckOuts,
            activeStays = activeStays,
            upcomingReservations = upcomingReservations
        )
    }

    // =========================================================
    // DATE UTILITIES
    // =========================================================

    /**
     * Returns today's date at midnight (00:00:00).
     */
    private fun getTodayMidnight(): Date {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    /**
     * Checks if two dates are on the same day (ignoring time).
     */
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply {
            time = date1
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val cal2 = Calendar.getInstance().apply {
            time = date2
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return cal1.time == cal2.time
    }

    // =========================================================
    // ERROR HANDLING
    // =========================================================

    /**
     * Clears the current error state.
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

// =========================================================
// STATE MODELS
// =========================================================

/**
 * UI state for the Today screen.
 */
data class TodayUiState(
    val todaysCheckIns: List<Reservation> = emptyList(),
    val todaysCheckOuts: List<Reservation> = emptyList(),
    val activeStays: List<Reservation> = emptyList(),
    val upcomingReservations: List<Reservation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * Returns true if all sections are empty.
     */
    fun isEmpty(): Boolean =
        todaysCheckIns.isEmpty() &&
                todaysCheckOuts.isEmpty() &&
                activeStays.isEmpty() &&
                upcomingReservations.isEmpty()
}

/**
 * Internal model for grouped reservations.
 */
private data class GroupedReservations(
    val todaysCheckIns: List<Reservation>,
    val todaysCheckOuts: List<Reservation>,
    val activeStays: List<Reservation>,
    val upcomingReservations: List<Reservation>
)
