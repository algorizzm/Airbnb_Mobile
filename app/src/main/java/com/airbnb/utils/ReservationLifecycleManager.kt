package com.airbnb.utils

import com.airbnb.data.model.Reservation
import com.airbnb.data.model.ReservationStatus
import com.airbnb.data.repository.ReservationRepository
import java.util.Calendar
import java.util.Date

/**
 * Manages automatic reservation lifecycle state transitions.
 * 
 * Lifecycle Flow:
 * - CONFIRMED -> UPCOMING (when check-in is within 30 days)
 * - UPCOMING -> ACTIVE_STAY (when current date >= check-in date)
 * - ACTIVE_STAY -> COMPLETED (when current date > check-out date)
 * 
 * This utility should be called:
 * - When TripsFragment loads
 * - Optionally on app startup
 * - Before rendering trip lists
 */
object ReservationLifecycleManager {

    private val repository = ReservationRepository()

    /**
     * Determines the correct lifecycle status for a reservation based on current date.
     * Does not modify the reservation, only returns the appropriate status.
     */
    fun determineLifecycleStatus(reservation: Reservation): String {
        // Don't transition terminal or cancelled states
        if (reservation.status in listOf(
                ReservationStatus.CANCELLED,
                ReservationStatus.REJECTED,
                ReservationStatus.COMPLETED,
                ReservationStatus.PENDING // Host hasn't approved yet
            )
        ) {
            return reservation.status
        }

        val checkInDate = reservation.checkInDate?.toDate() ?: return reservation.status
        val checkOutDate = reservation.checkOutDate?.toDate() ?: return reservation.status
        val now = Date()

        return when {
            // If checkout has passed, mark as completed
            now.after(checkOutDate) -> ReservationStatus.COMPLETED

            // If check-in has passed but checkout hasn't, it's an active stay
            now.after(checkInDate) || isSameDay(now, checkInDate) -> ReservationStatus.ACTIVE_STAY

            // If check-in is within 30 days, mark as upcoming
            isWithinDays(now, checkInDate, 30) -> ReservationStatus.UPCOMING

            // Otherwise keep as confirmed
            else -> ReservationStatus.CONFIRMED
        }
    }

    /**
     * Syncs a single reservation's lifecycle status if needed.
     * Returns true if the status was updated.
     */
    suspend fun syncReservationLifecycle(reservation: Reservation): Boolean {
        val newStatus = determineLifecycleStatus(reservation)
        
        if (newStatus != reservation.status) {
            repository.updateReservationStatus(reservation.id, newStatus)
            return true
        }
        
        return false
    }

    /**
     * Syncs lifecycle status for a list of reservations.
     * Returns the count of reservations that were updated.
     */
    suspend fun syncReservationsLifecycle(reservations: List<Reservation>): Int {
        var updatedCount = 0
        
        reservations.forEach { reservation ->
            if (syncReservationLifecycle(reservation)) {
                updatedCount++
            }
        }
        
        return updatedCount
    }

    /**
     * Checks if a date is within a specified number of days from now.
     */
    private fun isWithinDays(from: Date, to: Date, days: Int): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = from
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return to.before(calendar.time) || isSameDay(to, calendar.time)
    }

    /**
     * Checks if two dates are on the same day.
     */
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Returns a human-readable countdown or status message for a reservation.
     */
    fun getReservationCountdown(reservation: Reservation): String {
        val checkInDate = reservation.checkInDate?.toDate()
        val checkOutDate = reservation.checkOutDate?.toDate()
        val now = Date()

        return when (reservation.status) {
            ReservationStatus.ACTIVE_STAY -> {
                if (checkOutDate != null) {
                    val daysRemaining = daysBetween(now, checkOutDate)
                    when {
                        daysRemaining == 0 -> "Checkout today"
                        daysRemaining == 1 -> "1 day remaining"
                        daysRemaining > 1 -> "$daysRemaining days remaining"
                        else -> "Active stay"
                    }
                } else {
                    "Active stay"
                }
            }
            ReservationStatus.UPCOMING -> {
                if (checkInDate != null) {
                    val daysUntil = daysBetween(now, checkInDate)
                    when {
                        daysUntil == 0 -> "Check-in today"
                        daysUntil == 1 -> "Check-in tomorrow"
                        daysUntil > 1 -> "Check-in in $daysUntil days"
                        else -> "Upcoming"
                    }
                } else {
                    "Upcoming"
                }
            }
            ReservationStatus.CONFIRMED -> "Confirmed"
            ReservationStatus.PENDING -> "Awaiting host approval"
            ReservationStatus.COMPLETED -> "Trip completed"
            ReservationStatus.CANCELLED -> "Cancelled"
            ReservationStatus.REJECTED -> "Rejected by host"
            else -> reservation.statusLabel()
        }
    }

    /**
     * Calculates the number of days between two dates.
     */
    private fun daysBetween(from: Date, to: Date): Int {
        val diff = to.time - from.time
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }
}
