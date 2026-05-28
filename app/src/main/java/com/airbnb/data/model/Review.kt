package com.airbnb.data.model

import com.google.firebase.Timestamp

/**
 * Review data model representing a traveler's review of a completed stay.
 * 
 * Reviews can only be submitted after a reservation is completed and checked out.
 * Each review includes an overall rating, category-specific ratings, and an optional comment.
 * 
 * @property id Unique review identifier
 * @property reservationId Associated reservation ID
 * @property listingId Listing being reviewed
 * @property hostId Host receiving the review
 * @property reviewerId Traveler who submitted the review
 * @property reviewerName Name of the reviewer
 * @property reviewerAvatarUrl Optional avatar URL of the reviewer
 * @property rating Overall rating (1-5 stars)
 * @property cleanlinessRating Cleanliness category rating (1-5 stars)
 * @property communicationRating Communication category rating (1-5 stars)
 * @property checkInRating Check-in experience rating (1-5 stars)
 * @property accuracyRating Listing accuracy rating (1-5 stars)
 * @property locationRating Location rating (1-5 stars)
 * @property valueRating Value for money rating (1-5 stars)
 * @property comment Optional review comment/text
 * @property createdAt Timestamp when review was created
 * @property updatedAt Timestamp when review was last updated
 */
data class Review(
    val id: String = "",
    val reservationId: String = "",
    val listingId: String = "",
    val hostId: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val reviewerAvatarUrl: String? = null,
    val rating: Int = 5,
    val cleanlinessRating: Int = 5,
    val communicationRating: Int = 5,
    val checkInRating: Int = 5,
    val accuracyRating: Int = 5,
    val locationRating: Int = 5,
    val valueRating: Int = 5,
    val comment: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    /**
     * Calculates the average of all category ratings.
     * 
     * @return Average rating across all categories
     */
    fun calculateAverageRating(): Double {
        val ratings = listOf(
            cleanlinessRating,
            communicationRating,
            checkInRating,
            accuracyRating,
            locationRating,
            valueRating
        )
        return ratings.average()
    }
    
    /**
     * Returns true if the review has a comment.
     * 
     * @return True if comment is not empty
     */
    fun hasComment(): Boolean = comment.isNotBlank()
    
    /**
     * Returns a formatted rating string.
     * 
     * Example: "4.8"
     * 
     * @return Formatted rating string
     */
    fun formattedRating(): String = String.format("%.1f", rating.toDouble())
    
    /**
     * Returns a formatted average rating string.
     * 
     * Example: "4.7"
     * 
     * @return Formatted average rating string
     */
    fun formattedAverageRating(): String = String.format("%.1f", calculateAverageRating())
    
    /**
     * Validates the review data.
     * 
     * @return True if all required fields are valid
     */
    fun isValid(): Boolean {
        return reservationId.isNotBlank() &&
                listingId.isNotBlank() &&
                hostId.isNotBlank() &&
                reviewerId.isNotBlank() &&
                reviewerName.isNotBlank() &&
                rating in 1..5 &&
                cleanlinessRating in 1..5 &&
                communicationRating in 1..5 &&
                checkInRating in 1..5 &&
                accuracyRating in 1..5 &&
                locationRating in 1..5 &&
                valueRating in 1..5
    }
}
