package com.airbnb.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.airbnb.data.model.Listing
import com.airbnb.utils.PublicCodeGenerator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ListingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val listingsCol get() = db.collection("listings")

    /**
     * Observes a single listing in real-time.
     */
    fun observeListing(listingId: String): Flow<Listing?> = callbackFlow {
        val registration = listingsCol.document(listingId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val listing = snapshot?.toObject(Listing::class.java)?.copy(id = snapshot.id)
            trySend(listing)
        }
        awaitClose { registration.remove() }
    }

    /**
     * Observes all listings in real-time, ordered by creation date (newest first).
     */
    fun observeListings(): Flow<List<Listing>> = callbackFlow {
        val registration = listingsCol
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Listing::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Observes listings for a specific host in real-time.
     */
    fun observeListingsForHost(hostId: String): Flow<List<Listing>> = callbackFlow {
        val registration = listingsCol
            .whereEqualTo("hostId", hostId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Listing::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Gets a single listing by ID (one-time fetch).
     */
    suspend fun getListing(listingId: String): Result<Listing> = runCatching {
        val doc = listingsCol.document(listingId).get().await()
        doc.toObject(Listing::class.java)?.copy(id = doc.id)
            ?: error("Listing not found")
    }

    /**
     * Creates a new listing in Firestore.
     */
    suspend fun createListing(listing: Listing): Result<String> = runCatching {
        val data = listing.copy(
            id = "",
            createdAt = listing.createdAt ?: Timestamp.now(),
            updatedAt = Timestamp.now(),
            listingCode = listing.listingCode ?: PublicCodeGenerator.generateListingCode()
        )
        val ref = listingsCol.document()
        ref.set(data.toFirestoreMap(includeCreatedAt = true)).await()
        ref.id
    }

    /**
     * Updates an existing listing in Firestore.
     */
    suspend fun updateListing(listing: Listing): Result<Unit> = runCatching {
        if (listing.id.isBlank()) error("Missing listing id")
        val data = listing.copy(updatedAt = Timestamp.now())
        listingsCol.document(listing.id).set(
            data.toFirestoreMap(includeCreatedAt = false),
            SetOptions.merge()
        ).await()
    }

    /**
     * Deletes a listing and all associated reservations.
     */
    suspend fun deleteListing(listingId: String): Result<Unit> = runCatching {
        // Delete associated reservations
        val reservationSnap = db.collection("reservations")
            .whereEqualTo("listingId", listingId)
            .get()
            .await()
        
        val batch = db.batch()
        reservationSnap.documents.forEach { doc -> batch.delete(doc.reference) }
        batch.delete(listingsCol.document(listingId))
        batch.commit().await()
    }

    /**
     * Searches listings by location (case-insensitive contains).
     */
    suspend fun searchListingsByLocation(location: String): Result<List<Listing>> = runCatching {
        val snapshot = listingsCol.get().await()
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(Listing::class.java)?.copy(id = doc.id)
        }.filter { listing ->
            listing.location.contains(location, ignoreCase = true)
        }
    }

    /**
     * Gets listings within a price range.
     */
    suspend fun getListingsByPriceRange(minPrice: Double, maxPrice: Double): Result<List<Listing>> = runCatching {
        val snapshot = listingsCol
            .whereGreaterThanOrEqualTo("pricePerNight", minPrice)
            .whereLessThanOrEqualTo("pricePerNight", maxPrice)
            .get()
            .await()
        
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(Listing::class.java)?.copy(id = doc.id)
        }
    }

    /**
     * Gets the count of listings for a specific host.
     */
    suspend fun getListingCountForHost(hostId: String): Int = runCatching {
        val snap = listingsCol
            .whereEqualTo("hostId", hostId)
            .get()
            .await()
        snap.size()
    }.getOrDefault(0)
}

/**
 * Converts a Listing to a Firestore-compatible map.
 */
private fun Listing.toFirestoreMap(includeCreatedAt: Boolean): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>(
        "title" to title,
        "description" to description,
        "location" to location,
        "pricePerNight" to pricePerNight,
        "hostId" to hostId,
        "hostName" to hostName,
        "hostProfileImage" to hostProfileImage,
        "imageUrl" to imageUrl,
        "galleryImageUrls" to galleryImageUrls,
        "amenities" to amenities,
        "maxGuests" to maxGuests,
        "bedrooms" to bedrooms,
        "bathrooms" to bathrooms,
        "propertyType" to propertyType,
        "updatedAt" to updatedAt,
        "listingCode" to listingCode
    )
    if (includeCreatedAt) {
        map["createdAt"] = createdAt
    }
    return map.filterValues { it != null }
}
