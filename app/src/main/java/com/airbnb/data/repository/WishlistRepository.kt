package com.airbnb.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.airbnb.data.model.Wishlist
import com.airbnb.utils.PublicCodeGenerator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WishlistRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val collectionRepository: WishlistCollectionRepository = WishlistCollectionRepository()
) {

    private val wishlistsCol get() = db.collection("wishlists")

    /**
     * Observes a user's wishlist in real-time.
     */
    fun observeWishlist(userId: String): Flow<Wishlist> = callbackFlow {
        val registration = wishlistsCol.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val wishlist = if (snapshot?.exists() == true) {
                    snapshot.toObject(Wishlist::class.java)?.copy(userId = userId)
                        ?: Wishlist(userId = userId)
                } else {
                    Wishlist(userId = userId)
                }
                
                trySend(wishlist)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Gets a user's wishlist (one-time fetch).
     */
    suspend fun getWishlist(userId: String): Result<Wishlist> = runCatching {
        val doc = wishlistsCol.document(userId).get().await()
        
        if (doc.exists()) {
            doc.toObject(Wishlist::class.java)?.copy(userId = userId)
                ?: Wishlist(userId = userId)
        } else {
            Wishlist(userId = userId)
        }
    }

    /**
     * Checks if a listing is in the user's wishlist.
     * Uses collection system (source of truth) rather than legacy document.
     */
    suspend fun isListingInWishlist(userId: String, listingId: String): Boolean {
        return collectionRepository.isListingInAnyCollection(userId, listingId)
    }

    /**
     * Adds a listing to the user's wishlist.
     * If collectionId is provided, adds to that collection.
     * Otherwise, adds to default collection (creating it if needed).
     * Also maintains legacy wishlist document for backward compatibility.
     */
    suspend fun addToWishlist(
        userId: String,
        listingId: String,
        collectionId: String? = null
    ): Result<Unit> = runCatching {
        // Add to collection (new system)
        val targetCollectionId = collectionId ?: run {
            val defaultCollection = collectionRepository.getOrCreateDefaultCollection(userId).getOrThrow()
            defaultCollection.id
        }
        collectionRepository.addListingToCollection(targetCollectionId, listingId).getOrThrow()

        // Also maintain legacy wishlist document for backward compatibility
        val docRef = wishlistsCol.document(userId)
        val doc = docRef.get().await()
        
        if (doc.exists()) {
            // Update existing wishlist - add listing ID to array
            docRef.update(
                mapOf(
                    "listingIds" to FieldValue.arrayUnion(listingId),
                    "updatedAt" to Timestamp.now()
                )
            ).await()
        } else {
            // Create new wishlist document
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "listingIds" to listOf(listingId),
                    "createdAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now(),
                    "wishlistCode" to PublicCodeGenerator.generateWishlistCode()
                ),
                SetOptions.merge()
            ).await()
        }
    }

    /**
     * Removes a listing from the user's wishlist.
     * Removes from all collections and legacy wishlist document.
     */
    suspend fun removeFromWishlist(userId: String, listingId: String): Result<Unit> = runCatching {
        // Remove from all collections (new system)
        collectionRepository.removeListingFromAllCollections(userId, listingId).getOrThrow()

        // Also remove from legacy wishlist document
        val docRef = wishlistsCol.document(userId)
        
        docRef.update(
            mapOf(
                "listingIds" to FieldValue.arrayRemove(listingId),
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    /**
     * Toggles a listing in the wishlist (add if not present, remove if present).
     * If adding and collectionId is provided, adds to that collection.
     * Otherwise, adds to default collection.
     */
    suspend fun toggleWishlist(
        userId: String,
        listingId: String,
        collectionId: String? = null
    ): Result<Boolean> = runCatching {
        val isInWishlist = isListingInWishlist(userId, listingId)
        
        if (isInWishlist) {
            removeFromWishlist(userId, listingId).getOrThrow()
            false // Removed
        } else {
            addToWishlist(userId, listingId, collectionId).getOrThrow()
            true // Added
        }
    }

    /**
     * Clears all listings from the user's wishlist.
     */
    suspend fun clearWishlist(userId: String): Result<Unit> = runCatching {
        wishlistsCol.document(userId).update(
            mapOf(
                "listingIds" to emptyList<String>(),
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    /**
     * Gets the count of listings in the user's wishlist.
     */
    suspend fun getWishlistCount(userId: String): Int = runCatching {
        val wishlist = getWishlist(userId).getOrNull()
        wishlist?.size() ?: 0
    }.getOrDefault(0)

    /**
     * Checks if multiple listings are in the wishlist (batch check).
     */
    suspend fun getWishlistStatus(userId: String, listingIds: List<String>): Map<String, Boolean> = runCatching {
        val wishlist = getWishlist(userId).getOrNull() ?: return@runCatching emptyMap()
        listingIds.associateWith { listingId ->
            wishlist.containsListing(listingId)
        }
    }.getOrDefault(emptyMap())

    /**
     * Removes a listing from all wishlists (used when a listing is deleted).
     */
    suspend fun removeListingFromAllWishlists(listingId: String): Result<Unit> = runCatching {
        val snapshot = wishlistsCol
            .whereArrayContains("listingIds", listingId)
            .get()
            .await()
        
        val batch = db.batch()
        snapshot.documents.forEach { doc ->
            batch.update(
                doc.reference,
                mapOf(
                    "listingIds" to FieldValue.arrayRemove(listingId),
                    "updatedAt" to Timestamp.now()
                )
            )
        }
        batch.commit().await()
    }
}
