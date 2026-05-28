package com.airbnb.data.model

import com.airbnb.utils.formatting.DateFormatter
import com.google.firebase.Timestamp

/**
 * Represents a date range that has been manually blocked by a host.
 * Blocked dates prevent new reservations from being created during that period.
 * 
 * This is distinct from reservations - blocked dates are host-initiated unavailability
 * (e.g., maintenance, personal use, seasonal closure).
 */
data class BlockedDate(
    val id: String = "",
    val listingId: String = "",
    val hostId: String = "",
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val reason: String = "", // e.g., "Host unavailable", "Maintenance", "Personal use"
    val createdAt: Timestamp? = null
) {
    /**
     * Returns true if this blocked date range overlaps with the given date range.
     */
    fun overlaps(checkIn: Timestamp, checkOut: Timestamp): Boolean {
        if (startDate == null || endDate == null) return false
        
        val blockStart = startDate.toDate()
        val blockEnd = endDate.toDate()
        val reserveStart = checkIn.toDate()
        val reserveEnd = checkOut.toDate()
        
        // Check if ranges overlap
        // Ranges overlap if: start1 < end2 AND start2 < end1
        return blockStart.before(reserveEnd) && reserveStart.before(blockEnd)
    }
    
    /**
     * Returns a user-friendly summary of the blocked period.
     */
    /** Returns a formatted date range summary using centralized formatter. */
    fun summary(): String {
        if (startDate == null || endDate == null) return "Invalid date range"
        return DateFormatter.formatReservationRange(startDate, endDate)
    }
}
