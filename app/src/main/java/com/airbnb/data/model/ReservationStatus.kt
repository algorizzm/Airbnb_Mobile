package com.airbnb.data.model

/**
 * Constants for reservation status values.
 * Use these constants throughout the app to ensure consistency.
 * 
 * Lifecycle Flow:
 * PENDING -> CONFIRMED -> UPCOMING -> ACTIVE_STAY -> COMPLETED
 * PENDING -> REJECTED
 * PENDING/CONFIRMED/UPCOMING -> CANCELLED
 */
object ReservationStatus {
    const val PENDING = "pending"
    const val CONFIRMED = "confirmed"
    const val UPCOMING = "upcoming"
    const val ACTIVE_STAY = "active_stay"
    const val REJECTED = "rejected"
    const val CANCELLED = "cancelled"
    const val COMPLETED = "completed"
}
