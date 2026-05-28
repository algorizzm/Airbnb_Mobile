package com.airbnb.data.model

import com.google.firebase.Timestamp

/**
 * Represents a merged calendar state for a specific date range.
 * Used to communicate availability status to the UI layer.
 * 
 * This model combines information from:
 * - Active reservations
 * - Blocked dates
 * - Listing availability rules
 */
data class AvailabilityRange(
    val startDate: Timestamp,
    val endDate: Timestamp,
    val status: AvailabilityStatus,
    val reason: String = "" // Optional explanation (e.g., "Reserved by John", "Host blocked")
)

/**
 * Availability status for a date range.
 */
enum class AvailabilityStatus {
    /** Date range is available for booking */
    AVAILABLE,
    
    /** Date range is reserved by a guest */
    RESERVED,
    
    /** Date range is blocked by the host */
    BLOCKED,
    
    /** Date range is unavailable for other reasons (past dates, etc.) */
    UNAVAILABLE
}
