package com.airbnb.data.model

import com.airbnb.utils.ReservationLifecycleManager
import com.airbnb.utils.formatting.DateFormatter

/**
 * UI model that combines a Reservation with its associated Listing.
 * Used for displaying trip information in the Trips screen.
 */
data class TripItem(
    val reservation: Reservation,
    val listing: Listing?
) {
    /** Returns true if this trip is upcoming or active. */
    fun isUpcoming(): Boolean = reservation.status.equals(ReservationStatus.UPCOMING, ignoreCase = true) ||
                                 reservation.status.equals(ReservationStatus.CONFIRMED, ignoreCase = true)

    /** Returns true if this trip is currently active (guest is staying). */
    fun isActiveStay(): Boolean = reservation.status.equals(ReservationStatus.ACTIVE_STAY, ignoreCase = true)

    /** Returns true if this trip is completed. */
    fun isCompleted(): Boolean = reservation.status.equals(ReservationStatus.COMPLETED, ignoreCase = true)

    /** Returns true if this trip is cancelled or rejected. */
    fun isCancelled(): Boolean = reservation.status.equals(ReservationStatus.CANCELLED, ignoreCase = true) ||
                                  reservation.status.equals(ReservationStatus.REJECTED, ignoreCase = true)

    /** Returns true if this trip is pending host approval. */
    fun isPending(): Boolean = reservation.status.equals(ReservationStatus.PENDING, ignoreCase = true)

    /** Returns the listing image URL or empty string if listing is null. */
    fun imageUrl(): String = listing?.imageUrl ?: reservation.listingImageUrl

    /** Returns the listing title or reservation title if listing is null. */
    fun title(): String = listing?.title ?: reservation.listingTitle

    /** Returns the listing location or empty string if listing is null. */
    fun location(): String = listing?.location ?: ""

    /** Returns the host name. */
    fun hostName(): String = reservation.hostName

    /** Returns the host avatar URL. */
    fun hostAvatarUrl(): String? = reservation.hostAvatarUrl

    /** Returns the reservation code. */
    fun reservationCode(): String = reservation.reservationCode ?: "N/A"

    /** Returns a countdown or status message for the trip. */
    fun countdownMessage(): String = ReservationLifecycleManager.getReservationCountdown(reservation)

    /** Returns the number of nights for this trip using centralized formatter. */
    fun numberOfNights(): Int {
        val checkIn = reservation.checkInDate?.toDate()
        val checkOut = reservation.checkOutDate?.toDate()
        
        if (checkIn != null && checkOut != null) {
            return DateFormatter.calculateNights(checkIn, checkOut)
        }
        
        return 0
    }

    /** Returns a formatted night summary using centralized formatter. */
    fun nightsSummary(): String {
        return DateFormatter.formatNights(reservation.checkInDate, reservation.checkOutDate)
    }
}
