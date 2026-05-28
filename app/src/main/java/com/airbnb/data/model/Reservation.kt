package com.airbnb.data.model

import com.google.firebase.Timestamp

data class Reservation(
    val id: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val listingImageUrl: String = "",
    val guestId: String = "",
    val guestName: String = "",
    val guestAvatarUrl: String? = null,
    val hostId: String = "",
    val hostName: String = "",
    val hostAvatarUrl: String? = null,
    val checkInDate: Timestamp? = null,
    val checkOutDate: Timestamp? = null,
    val numberOfGuests: Int = 1,
    val totalPrice: Double = 0.0,
    val status: String = ReservationStatus.PENDING,
    val paymentStatus: String = "unpaid", // unpaid, paid, refunded
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val reservationCode: String? = null,
    val checkedIn: Boolean = false,
    val checkedOut: Boolean = false,
    val reviewSubmitted: Boolean = false
) {
    /** Returns true if the reservation is active (not cancelled or completed). */
    fun isActive(): Boolean = status.equals(ReservationStatus.PENDING, ignoreCase = true) || 
                              status.equals(ReservationStatus.CONFIRMED, ignoreCase = true) ||
                              status.equals(ReservationStatus.UPCOMING, ignoreCase = true) ||
                              status.equals(ReservationStatus.ACTIVE_STAY, ignoreCase = true)

    /** Returns true if the reservation can be cancelled. */
    fun isCancellable(): Boolean = status.equals(ReservationStatus.PENDING, ignoreCase = true) || 
                                    status.equals(ReservationStatus.CONFIRMED, ignoreCase = true) ||
                                    status.equals(ReservationStatus.UPCOMING, ignoreCase = true)

    /** Returns a formatted total price string. */
    fun formattedTotalPrice(): String = "₱${totalPrice.toInt()}"

    /** Returns a summary of the number of guests. */
    fun guestSummary(): String = when {
        numberOfGuests == 1 -> "1 guest"
        else -> "$numberOfGuests guests"
    }

    /** Returns a user-friendly status label. */
    fun statusLabel(): String = when (status.lowercase()) {
        "pending" -> "Pending"
        "confirmed" -> "Confirmed"
        "upcoming" -> "Upcoming"
        "active_stay" -> "Active Stay"
        "rejected" -> "Rejected"
        "cancelled" -> "Cancelled"
        "completed" -> "Completed"
        else -> status.replaceFirstChar { it.uppercase() }
    }
}
