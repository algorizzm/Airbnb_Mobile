package com.airbnb.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object BackfillUtility {
    private const val TAG = "BackfillUtility"

    suspend fun runBackfill(db: FirebaseFirestore) {
        Log.d(TAG, "Starting Firestore collections backfill check...")
        
        try {
            backfillUsers(db)
            backfillListings(db)
            backfillReservations(db)
            backfillWishlists(db)
            Log.d(TAG, "Firestore collections backfill check completed successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error running Firestore backfill", e)
        }
    }

    private suspend fun backfillUsers(db: FirebaseFirestore) {
        val collection = db.collection("users")
        val snapshot = collection.get().await()
        var updatedCount = 0

        for (doc in snapshot.documents) {
            val userCode = doc.getString("userCode")
            if (userCode.isNullOrBlank()) {
                val newCode = PublicCodeGenerator.generateUserCode()
                collection.document(doc.id)
                    .update("userCode", newCode)
                    .await()
                updatedCount++
            }
        }
        if (updatedCount > 0) {
            Log.d(TAG, "Backfilled $updatedCount users with userCode.")
        } else {
            Log.d(TAG, "No users required backfilling.")
        }
    }

    private suspend fun backfillListings(db: FirebaseFirestore) {
        val collection = db.collection("listings")
        val snapshot = collection.get().await()
        var updatedCount = 0

        for (doc in snapshot.documents) {
            val listingCode = doc.getString("listingCode")
            if (listingCode.isNullOrBlank()) {
                val newCode = PublicCodeGenerator.generateListingCode()
                collection.document(doc.id)
                    .update("listingCode", newCode)
                    .await()
                updatedCount++
            }
        }
        if (updatedCount > 0) {
            Log.d(TAG, "Backfilled $updatedCount listings with listingCode.")
        } else {
            Log.d(TAG, "No listings required backfilling.")
        }
    }

    private suspend fun backfillReservations(db: FirebaseFirestore) {
        val collection = db.collection("reservations")
        val snapshot = collection.get().await()
        var updatedCount = 0

        for (doc in snapshot.documents) {
            val reservationCode = doc.getString("reservationCode")
            if (reservationCode.isNullOrBlank()) {
                val newCode = PublicCodeGenerator.generateReservationCode()
                collection.document(doc.id)
                    .update("reservationCode", newCode)
                    .await()
                updatedCount++
            }
        }
        if (updatedCount > 0) {
            Log.d(TAG, "Backfilled $updatedCount reservations with reservationCode.")
        } else {
            Log.d(TAG, "No reservations required backfilling.")
        }
    }

    private suspend fun backfillWishlists(db: FirebaseFirestore) {
        val collection = db.collection("wishlists")
        val snapshot = collection.get().await()
        var updatedCount = 0

        for (doc in snapshot.documents) {
            val wishlistCode = doc.getString("wishlistCode")
            if (wishlistCode.isNullOrBlank()) {
                val newCode = PublicCodeGenerator.generateWishlistCode()
                collection.document(doc.id)
                    .update("wishlistCode", newCode)
                    .await()
                updatedCount++
            }
        }
        if (updatedCount > 0) {
            Log.d(TAG, "Backfilled $updatedCount wishlists with wishlistCode.")
        } else {
            Log.d(TAG, "No wishlists required backfilling.")
        }
    }
}
