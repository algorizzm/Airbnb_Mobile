package com.airbnb.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.airbnb.data.model.WishlistCollection
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing wishlist collections.
 * Handles CRUD operations for collection management.
 */
class WishlistCollectionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val collectionsCol get() = db.collection("wishlist_collections")

    companion object {
        const val DEFAULT_COLLECTION_NAME = "Favorites"
    }

    /**
     * Observes all collections for a user in real-time.
     */
    fun observeCollections(userId: String): Flow<List<WishlistCollection>> = callbackFlow {
        val registration = collectionsCol
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val collections = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(WishlistCollection::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(collections)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Gets all collections for a user (one-time fetch).
     */
    suspend fun getCollections(userId: String): Result<List<WishlistCollection>> = runCatching {
        val snapshot = collectionsCol
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            doc.toObject(WishlistCollection::class.java)?.copy(id = doc.id)
        }
    }

    /**
     * Gets a single collection by ID.
     */
    suspend fun getCollection(collectionId: String): Result<WishlistCollection?> = runCatching {
        val doc = collectionsCol.document(collectionId).get().await()
        if (doc.exists()) {
            doc.toObject(WishlistCollection::class.java)?.copy(id = doc.id)
        } else {
            null
        }
    }

    /**
     * Gets the default collection for a user, creating it if it doesn't exist.
     */
    suspend fun getOrCreateDefaultCollection(userId: String): Result<WishlistCollection> = runCatching {
        // Try to find existing default collection
        val snapshot = collectionsCol
            .whereEqualTo("userId", userId)
            .whereEqualTo("isDefault", true)
            .limit(1)
            .get()
            .await()

        if (!snapshot.isEmpty) {
            val doc = snapshot.documents.first()
            doc.toObject(WishlistCollection::class.java)?.copy(id = doc.id)
                ?: throw IllegalStateException("Failed to deserialize default collection")
        } else {
            // Create default collection
            createCollection(userId, DEFAULT_COLLECTION_NAME, isDefault = true).getOrThrow()
        }
    }

    /**
     * Creates a new collection.
     */
    suspend fun createCollection(
        userId: String,
        name: String,
        isDefault: Boolean = false
    ): Result<WishlistCollection> = runCatching {
        val docRef = collectionsCol.document()
        val now = Timestamp.now()

        val collection = WishlistCollection(
            id = docRef.id,
            userId = userId,
            name = name,
            listingIds = emptyList(),
            createdAt = now,
            updatedAt = now,
            isDefault = isDefault
        )

        docRef.set(
            mapOf(
                "userId" to userId,
                "name" to name,
                "listingIds" to emptyList<String>(),
                "createdAt" to now,
                "updatedAt" to now,
                "isDefault" to isDefault
            )
        ).await()

        collection
    }

    /**
     * Renames a collection.
     */
    suspend fun renameCollection(collectionId: String, newName: String): Result<Unit> = runCatching {
        collectionsCol.document(collectionId).update(
            mapOf(
                "name" to newName,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    /**
     * Deletes a collection.
     * If moveToDefault is true, moves all listings to the default collection.
     * If moveToDefault is false, listings that are orphaned (not in any other collection)
     * will also be removed from the legacy wishlist document to prevent stale state.
     */
    suspend fun deleteCollection(
        collectionId: String,
        userId: String,
        moveToDefault: Boolean = true
    ): Result<Unit> = runCatching {
        val collection = getCollection(collectionId).getOrNull()
            ?: throw IllegalArgumentException("Collection not found")

        // Prevent deleting default collection if it's the only one
        if (collection.isDefault) {
            val allCollections = getCollections(userId).getOrNull() ?: emptyList()
            if (allCollections.size == 1) {
                throw IllegalStateException("Cannot delete the only collection")
            }
        }

        // Move listings to default collection if requested
        if (moveToDefault && collection.listingIds.isNotEmpty()) {
            val defaultCollection = getOrCreateDefaultCollection(userId).getOrThrow()
            if (defaultCollection.id != collectionId) {
                // Add all listings to default collection
                collection.listingIds.forEach { listingId ->
                    addListingToCollection(defaultCollection.id, listingId).getOrThrow()
                }
            }
        } else if (!moveToDefault && collection.listingIds.isNotEmpty()) {
            // Not moving listings — clean up legacy wishlist document for truly orphaned listings.
            // A listing is orphaned if it no longer exists in any remaining collection.
            val remainingCollections = getCollections(userId).getOrNull() ?: emptyList()
            val stillWishlistedIds = remainingCollections
                .filter { it.id != collectionId }
                .flatMap { it.listingIds }
                .toSet()

            val orphanedIds = collection.listingIds.filter { it !in stillWishlistedIds }
            if (orphanedIds.isNotEmpty()) {
                val docRef = db.collection("wishlists").document(userId)
                val batch = db.batch()
                orphanedIds.forEach { listingId ->
                    batch.update(
                        docRef,
                        mapOf(
                            "listingIds" to FieldValue.arrayRemove(listingId),
                            "updatedAt" to com.google.firebase.Timestamp.now()
                        )
                    )
                }
                batch.commit().await()
            }
        }

        // Delete the collection
        collectionsCol.document(collectionId).delete().await()
    }

    /**
     * Adds a listing to a collection.
     */
    suspend fun addListingToCollection(collectionId: String, listingId: String): Result<Unit> = runCatching {
        collectionsCol.document(collectionId).update(
            mapOf(
                "listingIds" to FieldValue.arrayUnion(listingId),
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    /**
     * Removes a listing from a collection.
     */
    suspend fun removeListingFromCollection(collectionId: String, listingId: String): Result<Unit> = runCatching {
        collectionsCol.document(collectionId).update(
            mapOf(
                "listingIds" to FieldValue.arrayRemove(listingId),
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    /**
     * Removes a listing from all collections for a user.
     */
    suspend fun removeListingFromAllCollections(userId: String, listingId: String): Result<Unit> = runCatching {
        val snapshot = collectionsCol
            .whereEqualTo("userId", userId)
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

    /**
     * Gets all collections that contain a specific listing.
     */
    suspend fun getCollectionsContainingListing(userId: String, listingId: String): Result<List<WishlistCollection>> = runCatching {
        val snapshot = collectionsCol
            .whereEqualTo("userId", userId)
            .whereArrayContains("listingIds", listingId)
            .get()
            .await()

        snapshot.documents.mapNotNull { doc ->
            doc.toObject(WishlistCollection::class.java)?.copy(id = doc.id)
        }
    }

    /**
     * Checks if a listing is in any collection.
     */
    suspend fun isListingInAnyCollection(userId: String, listingId: String): Boolean = runCatching {
        val collections = getCollectionsContainingListing(userId, listingId).getOrNull()
        !collections.isNullOrEmpty()
    }.getOrDefault(false)

    /**
     * Moves a listing from one collection to another.
     */
    suspend fun moveListingBetweenCollections(
        fromCollectionId: String,
        toCollectionId: String,
        listingId: String
    ): Result<Unit> = runCatching {
        val batch = db.batch()

        // Remove from source collection
        batch.update(
            collectionsCol.document(fromCollectionId),
            mapOf(
                "listingIds" to FieldValue.arrayRemove(listingId),
                "updatedAt" to Timestamp.now()
            )
        )

        // Add to destination collection
        batch.update(
            collectionsCol.document(toCollectionId),
            mapOf(
                "listingIds" to FieldValue.arrayUnion(listingId),
                "updatedAt" to Timestamp.now()
            )
        )

        batch.commit().await()
    }

    /**
     * Gets the total count of listings across all collections for a user.
     * Note: This counts unique listings (no duplicates if a listing is in multiple collections).
     */
    suspend fun getTotalUniqueListingsCount(userId: String): Int = runCatching {
        val collections = getCollections(userId).getOrNull() ?: emptyList()
        collections.flatMap { it.listingIds }.toSet().size
    }.getOrDefault(0)
}
