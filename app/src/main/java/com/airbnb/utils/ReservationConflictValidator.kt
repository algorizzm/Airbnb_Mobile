package com.airbnb.utils

import com.airbnb.data.model.BlockedDate
import com.airbnb.data.model.Reservation
import com.airbnb.data.model.ReservationStatus
import com.google.firebase.Timestamp
import java.util.*

/**
 * Centralized validator for reservation conflicts and availability.
 * 
 * This is the single source of truth for reservation integrity.
 * All reservation writes MUST use this validator to prevent:
 * - Overlapping reservations
 * - Reservations on blocked dates
 * - Invalid date ranges
 * - Past date reservations
 * 
 * IMPORTANT: This validator should be called at the repository level
 * before any Firestore write operation.
 */
object ReservationConflictValidator {
    
    /**
     * Validation result for a reservation attempt.
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
    
    /**
     * Validates a reservation against existing reservations and blocked dates.
     * 
     * @param checkInDate The proposed check-in date
     * @param checkOutDate The proposed check-out date
     * @param existingReservations All active reservations for the listing
     * @param blockedDates All blocked date ranges for the listing
     * @param excludeReservationId Optional reservation ID to exclude (for updates)
     * @return ValidationResult indicating if the reservation is valid
     */
    fun validateReservation(
        checkInDate: Timestamp,
        checkOutDate: Timestamp,
        existingReservations: List<Reservation>,
        blockedDates: List<BlockedDate>,
        excludeReservationId: String? = null
    ): ValidationResult {
        val checkIn = checkInDate.toDate()
        val checkOut = checkOutDate.toDate()
        
        // Rule 1: Check-out must be after check-in
        if (!DateNormalizationUtil.isDateAfter(checkOut, checkIn)) {
            return ValidationResult.Invalid("Check-out date must be after check-in date")
        }
        
        // Rule 2: Cannot book dates in the past
        val today = DateNormalizationUtil.normalizeToMidnight(Date())
        if (DateNormalizationUtil.isDateBefore(checkIn, today)) {
            return ValidationResult.Invalid("Cannot book dates in the past")
        }
        
        // Rule 3: Check for overlapping reservations
        val activeReservations = existingReservations.filter { reservation ->
            // Only check active reservations (not cancelled, rejected, or completed)
            val isActive = reservation.status in listOf(
                ReservationStatus.PENDING,
                ReservationStatus.CONFIRMED,
                ReservationStatus.UPCOMING,
                ReservationStatus.ACTIVE_STAY
            )
            
            // Exclude the reservation being updated (if any)
            val shouldInclude = excludeReservationId == null || reservation.id != excludeReservationId
            
            isActive && shouldInclude
        }
        
        for (reservation in activeReservations) {
            val existingCheckIn = reservation.checkInDate?.toDate() ?: continue
            val existingCheckOut = reservation.checkOutDate?.toDate() ?: continue
            
            if (DateNormalizationUtil.doRangesOverlap(checkIn, checkOut, existingCheckIn, existingCheckOut)) {
                return ValidationResult.Invalid("These dates overlap with an existing reservation")
            }
        }
        
        // Rule 4: Check for blocked dates
        for (blockedDate in blockedDates) {
            val blockStart = blockedDate.startDate?.toDate() ?: continue
            val blockEnd = blockedDate.endDate?.toDate() ?: continue
            
            if (DateNormalizationUtil.doRangesOverlap(checkIn, checkOut, blockStart, blockEnd)) {
                val reason = if (blockedDate.reason.isNotBlank()) {
                    "This listing is unavailable: ${blockedDate.reason}"
                } else {
                    "This listing is blocked during your selected dates"
                }
                return ValidationResult.Invalid(reason)
            }
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Quick check if a specific date is available (not reserved or blocked).
     * Useful for calendar UI to disable specific dates.
     */
    fun isDateAvailable(
        date: Date,
        existingReservations: List<Reservation>,
        blockedDates: List<BlockedDate>
    ): Boolean {
        val normalizedDate = DateNormalizationUtil.normalizeToMidnight(date)
        
        // Check if date is in the past
        val today = DateNormalizationUtil.normalizeToMidnight(Date())
        if (DateNormalizationUtil.isDateBefore(normalizedDate, today)) {
            return false
        }
        
        // Check active reservations
        val activeReservations = existingReservations.filter { reservation ->
            reservation.status in listOf(
                ReservationStatus.PENDING,
                ReservationStatus.CONFIRMED,
                ReservationStatus.UPCOMING,
                ReservationStatus.ACTIVE_STAY
            )
        }
        
        for (reservation in activeReservations) {
            val checkIn = reservation.checkInDate?.toDate() ?: continue
            val checkOut = reservation.checkOutDate?.toDate() ?: continue
            
            if (DateNormalizationUtil.isDateInRange(normalizedDate, checkIn, checkOut)) {
                return false
            }
        }
        
        // Check blocked dates
        for (blockedDate in blockedDates) {
            val blockStart = blockedDate.startDate?.toDate() ?: continue
            val blockEnd = blockedDate.endDate?.toDate() ?: continue
            
            if (DateNormalizationUtil.isDateInRange(normalizedDate, blockStart, blockEnd)) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Gets all unavailable dates within a date range.
     * Returns a list of dates that should be disabled in the calendar UI.
     */
    fun getUnavailableDates(
        rangeStart: Date,
        rangeEnd: Date,
        existingReservations: List<Reservation>,
        blockedDates: List<BlockedDate>
    ): List<Date> {
        val unavailableDates = mutableListOf<Date>()
        val calendar = Calendar.getInstance()
        calendar.time = DateNormalizationUtil.normalizeToMidnight(rangeStart)
        
        val end = DateNormalizationUtil.normalizeToMidnight(rangeEnd)
        
        while (!calendar.time.after(end)) {
            val currentDate = calendar.time
            
            if (!isDateAvailable(currentDate, existingReservations, blockedDates)) {
                unavailableDates.add(Date(currentDate.time))
            }
            
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return unavailableDates
    }
    
    /**
     * Validates a blocked date range to ensure it doesn't conflict with existing reservations.
     * Hosts should not be able to block dates that already have confirmed reservations.
     */
    fun validateBlockedDateRange(
        startDate: Timestamp,
        endDate: Timestamp,
        existingReservations: List<Reservation>
    ): ValidationResult {
        val start = startDate.toDate()
        val end = endDate.toDate()
        
        // Rule 1: End date must be after start date
        if (!DateNormalizationUtil.isDateAfter(end, start)) {
            return ValidationResult.Invalid("End date must be after start date")
        }
        
        // Rule 2: Check for overlapping active reservations
        val activeReservations = existingReservations.filter { reservation ->
            reservation.status in listOf(
                ReservationStatus.PENDING,
                ReservationStatus.CONFIRMED,
                ReservationStatus.UPCOMING,
                ReservationStatus.ACTIVE_STAY
            )
        }
        
        for (reservation in activeReservations) {
            val checkIn = reservation.checkInDate?.toDate() ?: continue
            val checkOut = reservation.checkOutDate?.toDate() ?: continue
            
            if (DateNormalizationUtil.doRangesOverlap(start, end, checkIn, checkOut)) {
                return ValidationResult.Invalid(
                    "Cannot block dates that overlap with existing reservation (${reservation.reservationCode ?: reservation.id})"
                )
            }
        }
        
        return ValidationResult.Valid
    }
}
