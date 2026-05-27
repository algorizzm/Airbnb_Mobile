package com.airbnb.data.model

import com.google.firebase.Timestamp

data class Listing(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val pricePerNight: Double = 0.0,
    val hostId: String = "",
    val hostName: String = "",
    val imageUrl: String = "",
    val galleryImageUrls: List<String> = emptyList(),
    val amenities: List<String> = emptyList(),
    val maxGuests: Int = 1,
    val bedrooms: Int = 1,
    val bathrooms: Int = 1,
    val propertyType: String = "", // e.g., "Apartment", "House", "Villa"
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    /** Returns the cover image URL for the listing. */
    fun coverImageUrl(): String = imageUrl.trim()

    /** Returns a formatted price string. */
    fun formattedPrice(): String = "₱${pricePerNight.toInt()}/night"

    /** Returns a summary of guest capacity. */
    fun guestSummary(): String = when {
        maxGuests == 1 -> "1 guest"
        else -> "$maxGuests guests"
    }

    /** Returns a summary of bedrooms and bathrooms. */
    fun roomSummary(): String = buildString {
        if (bedrooms > 0) {
            append(if (bedrooms == 1) "1 bedroom" else "$bedrooms bedrooms")
        }
        if (bathrooms > 0) {
            if (isNotEmpty()) append(" · ")
            append(if (bathrooms == 1) "1 bathroom" else "$bathrooms bathrooms")
        }
    }
}
