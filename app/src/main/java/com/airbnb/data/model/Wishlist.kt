package com.airbnb.data.model

import com.google.firebase.Timestamp

data class Wishlist(
    val userId: String = "",
    val listingIds: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val wishlistCode: String? = null
) {
    /** Returns true if the wishlist contains the specified listing. */
    fun containsListing(listingId: String): Boolean = listingIds.contains(listingId)

    /** Returns the number of listings in the wishlist. */
    fun size(): Int = listingIds.size

    /** Returns true if the wishlist is empty. */
    fun isEmpty(): Boolean = listingIds.isEmpty()

    /** Returns a new Wishlist with the listing added. */
    fun addListing(listingId: String): Wishlist {
        if (containsListing(listingId)) return this
        return copy(
            listingIds = listingIds + listingId,
            updatedAt = Timestamp.now()
        )
    }

    /** Returns a new Wishlist with the listing removed. */
    fun removeListing(listingId: String): Wishlist {
        if (!containsListing(listingId)) return this
        return copy(
            listingIds = listingIds - listingId,
            updatedAt = Timestamp.now()
        )
    }

    /** Returns a summary of the wishlist size. */
    fun sizeSummary(): String = when (size()) {
        0 -> "No saved listings"
        1 -> "1 saved listing"
        else -> "${size()} saved listings"
    }
}
