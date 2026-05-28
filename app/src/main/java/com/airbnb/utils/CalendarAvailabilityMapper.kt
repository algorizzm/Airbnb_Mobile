package com.airbnb.utils

import com.airbnb.data.model.AvailabilityRange
import com.airbnb.data.model.AvailabilityStatus
import com.airbnb.data.model.BlockedDate
import com.airbnb.data.model.Reservation
import com.airbnb.data.model.ReservationStatus
import com.google.firebase.Timestamp
import java.util.*

/**
 * Maps reservations and blocked dates into merged availability ranges for calendar UI.
 * 
 * This utility combines multiple data sources (reservations, blocked dates, listing rules)
 * into a unified availability view that can be easily consumed by the UI layer.
 */
object CalendarAvailabilityMapper {
    
    /**
     * Generates availability ranges for a listing within a date range.
     * 
     * @param rangeStart Start of the date range to analyze
     * @param rangeEnd End of the date range to analyze
     * @param reservations All reservations for the listing
     * @param blockedDates All blocked date ranges for the listing
     * @return List of availability ranges with status and reason
     */
    fun generateAvailabilityRanges(
        rangeStart: Date,
        rangeEnd: Date,
        reservations: List<Reservation>,
        blockedDates: List<BlockedDate>
    ): List<AvailabilityRange> {
        val ranges = mutableListOf<AvailabilityRange>()
        
        // Add active reservations as RESERVED ranges
        val activeReservations = reservations.filter { reservation ->
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
            
            // Only include if it overlaps with the requested range
            if (DateNormalizationUtil.doRangesOverlap(checkIn, checkOut, rangeStart, rangeEnd)) {
                ranges.add(
                    AvailabilityRange(
                        startDate = Timestamp(checkIn),
                        endDate = Timestamp(checkOut),
                        status = AvailabilityStatus.RESERVED,
                        reason = "Reserved by ${reservation.guestName}"
                    )
                )
            }
        }
        
        // Add blocked dates as BLOCKED ranges
        for (blockedDate in blockedDates) {
            val blockStart = blockedDate.startDate?.toDate() ?: continue
            val blockEnd = blockedDate.endDate?.toDate() ?: continue
            
            // Only include if it overlaps with the requested range
            if (DateNormalizationUtil.doRangesOverlap(blockStart, blockEnd, rangeStart, rangeEnd)) {
                ranges.add(
                    AvailabilityRange(
                        startDate = Timestamp(blockStart),
                        endDate = Timestamp(blockEnd),
                        status = AvailabilityStatus.BLOCKED,
                        reason = blockedDate.reason.ifBlank { "Blocked by host" }
                    )
                )
            }
        }
        
        // Add past dates as UNAVAILABLE
        val today = DateNormalizationUtil.normalizeToMidnight(Date())
        if (DateNormalizationUtil.isDateBefore(rangeStart, today)) {
            val pastEnd = if (DateNormalizationUtil.isDateBefore(rangeEnd, today)) {
                rangeEnd
            } else {
                // Yesterday
                val calendar = Calendar.getInstance()
                calendar.time = today
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.time
            }
            
            ranges.add(
                AvailabilityRange(
                    startDate = Timestamp(rangeStart),
                    endDate = Timestamp(pastEnd),
                    status = AvailabilityStatus.UNAVAILABLE,
                    reason = "Past dates"
                )
            )
        }
        
        // Sort ranges by start date
        return ranges.sortedBy { it.startDate.toDate() }
    }
    
    /**
     * Gets a simple list of unavailable dates for calendar UI.
     * This is a convenience method that returns dates that should be disabled.
     */
    fun getDisabledDates(
        rangeStart: Date,
        rangeEnd: Date,
        reservations: List<Reservation>,
        blockedDates: List<BlockedDate>
    ): List<Date> {
        return ReservationConflictValidator.getUnavailableDates(
            rangeStart,
            rangeEnd,
            reservations,
            blockedDates
        )
    }
    
    /**
     * Checks if a listing is available for a specific date range.
     * Returns true only if the entire range is available.
     */
    fun isRangeAvailable(
        checkIn: Date,
        checkOut: Date,
        reservations: List<Reservation>,
        blockedDates: List<BlockedDate>
    ): Boolean {
        val result = ReservationConflictValidator.validateReservation(
            checkInDate = Timestamp(checkIn),
            checkOutDate = Timestamp(checkOut),
            existingReservations = reservations,
            blockedDates = blockedDates
        )
        
        return result is ReservationConflictValidator.ValidationResult.Valid
    }
    
    /**
     * Gets the next available check-in date for a listing.
     * Useful for suggesting alternative dates to users.
     */
    fun getNextAvailableDate(
        startSearchDate: Date,
        reservations: List<Reservation>,
        blockedDates: List<BlockedDate>,
        maxDaysToSearch: Int = 90
    ): Date? {
        val calendar = Calendar.getInstance()
        calendar.time = DateNormalizationUtil.normalizeToMidnight(startSearchDate)
        
        repeat(maxDaysToSearch) {
            val currentDate = calendar.time
            
            if (ReservationConflictValidator.isDateAvailable(currentDate, reservations, blockedDates)) {
                return currentDate
            }
            
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return null // No available date found within search range
    }
}
