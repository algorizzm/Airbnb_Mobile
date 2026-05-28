package com.airbnb.data.repository

import android.util.Log
import com.airbnb.data.model.Review
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing reviews in Firestore.
 * 
 * Responsibilities:
 * - Create and submit reviews
 * - Observe reviews for listings and hosts
 * - Calculate and update listing rating aggregations
 * - Validate review eligibility
 * - Handle review-related business logic
 * 
 * Collection structure:
 * - reviews/{reviewId}
 * 
 * Indexes required:
 * - listingId (ascending), createdAt (descending)
 * - hostId (ascending), createdAt (descending)
 * - reviewerId (ascending), createdAt (descending)
 * - reservationId (ascending)
 */
class ReviewRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val reviewsCollection = firestore.collection("reviews")
    private val listingsCollection = firestore.collection("listings")
    private val reservationsCollection = firestore.collection("reservations")
    
    companion object {
        private const val TAG = "ReviewRepository"
    }
    
    /**
     * Creates and submits a new review.
     * 
     * This method:
     * 1. Validates the review data
     * 2. Creates the review document in Firestore
     * 3. Updates the reservation's reviewSubmitted flag
     * 4. Recalculates and updates the listing's rating metrics
     * 
     * @param review Review to submit
     * @return Result with review ID on success, error on failure
     */
    suspend fun submitReview(review: Review): Result<String> {
        return try {
            // Validate review
            if (!review.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid review data"))
            }
            
            // Generate review ID
            val reviewId = reviewsCollection.document().id
            val reviewWithId = review.copy(
                id = reviewId,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            
            // Create review document
            reviewsCollection.document(reviewId).set(reviewWithId).await()
            Log.d(TAG, "Review created: $reviewId")
            
            // Update reservation's reviewSubmitted flag
            reservationsCollection.document(review.reservationId)
                .update("reviewSubmitted", true)
                .await()
            Log.d(TAG, "Reservation updated: ${review.reservationId}")
            
            // Update listing rating metrics
            updateListingRatingMetrics(review.listingId)
            
            Result.success(reviewId)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting review", e)
            Result.failure(e)
        }
    }
    
    /**
     * Observes reviews for a specific listing.
     * 
     * @param listingId Listing ID
     * @return Flow of review lists, ordered by creation date (newest first)
     */
    fun observeListingReviews(listingId: String): Flow<List<Review>> = callbackFlow {
        val listener = reviewsCollection
            .whereEqualTo("listingId", listingId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing listing reviews", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Review::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing review: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(reviews)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Observes reviews for a specific host.
     * 
     * @param hostId Host ID
     * @return Flow of review lists, ordered by creation date (newest first)
     */
    fun observeHostReviews(hostId: String): Flow<List<Review>> = callbackFlow {
        val listener = reviewsCollection
            .whereEqualTo("hostId", hostId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing host reviews", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Review::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing review: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(reviews)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Observes reviews submitted by a specific reviewer.
     * 
     * @param reviewerId Reviewer ID
     * @return Flow of review lists, ordered by creation date (newest first)
     */
    fun observeReviewerReviews(reviewerId: String): Flow<List<Review>> = callbackFlow {
        val listener = reviewsCollection
            .whereEqualTo("reviewerId", reviewerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing reviewer reviews", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Review::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing review: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(reviews)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Gets a review for a specific reservation.
     * 
     * @param reservationId Reservation ID
     * @return Review if found, null otherwise
     */
    suspend fun getReviewByReservation(reservationId: String): Review? {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("reservationId", reservationId)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.toObject(Review::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting review by reservation", e)
            null
        }
    }
    
    /**
     * Recalculates and updates a listing's rating metrics.
     * 
     * This method:
     * 1. Fetches all reviews for the listing
     * 2. Calculates the average rating
     * 3. Counts the total number of reviews
     * 4. Updates the listing document
     * 
     * @param listingId Listing ID
     */
    private suspend fun updateListingRatingMetrics(listingId: String) {
        try {
            // Fetch all reviews for the listing
            val snapshot = reviewsCollection
                .whereEqualTo("listingId", listingId)
                .get()
                .await()
            
            val reviews = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Review::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing review: ${doc.id}", e)
                    null
                }
            }
            
            // Calculate metrics
            val reviewCount = reviews.size
            val averageRating = if (reviewCount > 0) {
                reviews.map { it.rating.toDouble() }.average()
            } else {
                0.0
            }
            
            // Update listing document
            listingsCollection.document(listingId)
                .update(
                    mapOf(
                        "averageRating" to averageRating,
                        "reviewCount" to reviewCount,
                        "rating" to averageRating // Keep legacy rating field in sync
                    )
                )
                .await()
            
            Log.d(TAG, "Updated listing $listingId: averageRating=$averageRating, reviewCount=$reviewCount")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating listing rating metrics", e)
        }
    }
    
    /**
     * Calculates the average rating for a host across all their listings.
     * 
     * @param hostId Host ID
     * @return Average rating, or 0.0 if no reviews
     */
    suspend fun calculateHostAverageRating(hostId: String): Double {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("hostId", hostId)
                .get()
                .await()
            
            val reviews = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Review::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing review: ${doc.id}", e)
                    null
                }
            }
            
            if (reviews.isEmpty()) {
                0.0
            } else {
                reviews.map { it.rating.toDouble() }.average()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating host average rating", e)
            0.0
        }
    }
    
    /**
     * Gets the total number of reviews for a host.
     * 
     * @param hostId Host ID
     * @return Total review count
     */
    suspend fun getHostReviewCount(hostId: String): Int {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("hostId", hostId)
                .get()
                .await()
            
            snapshot.size()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting host review count", e)
            0
        }
    }
}
