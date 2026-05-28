package com.airbnb.data.model

import com.google.firebase.Timestamp

/**
 * Represents a wishlist collection/category.
 * Users can organize saved listings into multiple collections.
 */
data class WishlistCollection(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val listingIds: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val isDefault: Boolean = false
) {
    /** Returns true if the collection contains the specified listing. */
    fun containsListing(listingId: String): Boolean = listingIds.contains(listingId)

    /** Returns the number of listings in the collection. */
    fun size(): Int = listingIds.size

    /** Returns true if the collection is empty. */
    fun isEmpty(): Boolean = listingIds.isEmpty()

    /** Returns a new collection with the listing added. */
    fun addListing(listingId: String): WishlistCollection {
        if (containsListing(listingId)) return this
        return copy(
            listingIds = listingIds + listingId,
            updatedAt = Timestamp.now()
        )
    }

    /** Returns a new collection with the listing removed. */
    fun removeListing(listingId: String): WishlistCollection {
        if (!containsListing(listingId)) return this
        return copy(
            listingIds = listingIds - listingId,
            updatedAt = Timestamp.now()
        )
    }

    /** Returns a summary of the collection size. */
    fun sizeSummary(): String = when (size()) {
        0 -> "No saved listings"
        1 -> "1 saved listing"
        else -> "${size()} saved listings"
    }

    /** Returns the first listing ID for cover image, or null if empty. */
    fun coverListingId(): String? = listingIds.firstOrNull()
}
