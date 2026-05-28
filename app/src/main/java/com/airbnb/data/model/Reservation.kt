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

    /** Returns true if the traveler can check in. */
    fun canCheckIn(): Boolean {
        android.util.Log.d("Reservation", "=== canCheckIn() Debug ===")
        android.util.Log.d("Reservation", "checkedIn: $checkedIn")
        
        if (checkedIn) {
            android.util.Log.d("Reservation", "FAIL: Already checked in")
            return false
        }
        
        android.util.Log.d("Reservation", "status: $status")
        if (status !in listOf(ReservationStatus.CONFIRMED, ReservationStatus.UPCOMING)) {
            android.util.Log.d("Reservation", "FAIL: Status not CONFIRMED or UPCOMING")
            return false
        }
        
        val checkInDate = checkInDate?.toDate()
        android.util.Log.d("Reservation", "checkInDate: $checkInDate")
        
        if (checkInDate == null) {
            android.util.Log.d("Reservation", "FAIL: checkInDate is null")
            return false
        }
        
        val now = java.util.Date()
        android.util.Log.d("Reservation", "now: $now")
        
        // Normalize dates to midnight for comparison (ignore time component)
        val checkInCal = java.util.Calendar.getInstance().apply {
            time = checkInDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        val nowCal = java.util.Calendar.getInstance().apply {
            time = now
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        android.util.Log.d("Reservation", "checkInCal (normalized): ${checkInCal.time}")
        android.util.Log.d("Reservation", "nowCal (normalized): ${nowCal.time}")
        
        val canCheckIn = !nowCal.time.before(checkInCal.time)
        android.util.Log.d("Reservation", "nowCal.time.before(checkInCal.time): ${nowCal.time.before(checkInCal.time)}")
        android.util.Log.d("Reservation", "Result: $canCheckIn")
        
        // Can check in on or after check-in date (comparing dates only, not time)
        return canCheckIn
    }

    /** Returns true if the traveler can check out. */
    fun canCheckOut(): Boolean {
        if (!checkedIn) return false
        if (checkedOut) return false
        return status == ReservationStatus.ACTIVE_STAY
    }

    /** Returns true if the traveler can check out early. */
    fun canEarlyCheckOut(): Boolean {
        if (!checkedIn) return false
        if (checkedOut) return false
        
        val checkOutDate = checkOutDate?.toDate() ?: return false
        val now = java.util.Date()
        
        // Normalize dates to midnight for comparison (ignore time component)
        val checkOutCal = java.util.Calendar.getInstance().apply {
            time = checkOutDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        val nowCal = java.util.Calendar.getInstance().apply {
            time = now
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        // Can early check out if current date is before scheduled check-out (comparing dates only)
        return nowCal.time.before(checkOutCal.time)
    }
}
