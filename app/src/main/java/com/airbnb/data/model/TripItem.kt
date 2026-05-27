package com.airbnb.data.model

/**
 * UI model that combines a Reservation with its associated Listing.
 * Used for displaying trip information in the Trips screen.
 */
data class TripItem(
    val reservation: Reservation,
    val listing: Listing?
) {
    /** Returns true if this trip is upcoming (active reservation). */
    fun isUpcoming(): Boolean = reservation.isActive()

    /** Returns true if this trip is completed. */
    fun isCompleted(): Boolean = reservation.status.equals(ReservationStatus.COMPLETED, ignoreCase = true)

    /** Returns true if this trip is cancelled or rejected. */
    fun isCancelled(): Boolean = reservation.status.equals(ReservationStatus.CANCELLED, ignoreCase = true) ||
                                  reservation.status.equals(ReservationStatus.REJECTED, ignoreCase = true)

    /** Returns the listing image URL or empty string if listing is null. */
    fun imageUrl(): String = listing?.imageUrl ?: reservation.listingImageUrl

    /** Returns the listing title or reservation title if listing is null. */
    fun title(): String = listing?.title ?: reservation.listingTitle

    /** Returns the listing location or empty string if listing is null. */
    fun location(): String = listing?.location ?: ""
}
