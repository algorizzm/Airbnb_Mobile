package com.airbnb.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.airbnb.data.model.Reservation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReservationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val reservationsCol get() = db.collection("reservations")

    /**
     * Observes all reservations for a specific guest in real-time.
     */
    fun observeReservationsForGuest(guestId: String): Flow<List<Reservation>> = callbackFlow {
        val registration = reservationsCol
            .whereEqualTo("guestId", guestId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Reservation::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Observes all reservations for a specific host in real-time.
     */
    fun observeReservationsForHost(hostId: String): Flow<List<Reservation>> = callbackFlow {
        val registration = reservationsCol
            .whereEqualTo("hostId", hostId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Reservation::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Observes all reservations for a specific listing in real-time.
     */
    fun observeReservationsForListing(listingId: String): Flow<List<Reservation>> = callbackFlow {
        val registration = reservationsCol
            .whereEqualTo("listingId", listingId)
            .orderBy("checkInDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Reservation::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Gets a single reservation by ID (one-time fetch).
     */
    suspend fun getReservation(reservationId: String): Result<Reservation> = runCatching {
        val doc = reservationsCol.document(reservationId).get().await()
        doc.toObject(Reservation::class.java)?.copy(id = doc.id)
            ?: error("Reservation not found")
    }

    /**
     * Creates a new reservation in Firestore.
     */
    suspend fun createReservation(reservation: Reservation): Result<String> = runCatching {
        val data = reservation.copy(
            id = "",
            status = "pending",
            paymentStatus = "unpaid",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        val ref = reservationsCol.document()
        ref.set(data.toFirestoreMap()).await()
        ref.id
    }

    /**
     * Updates an existing reservation's status.
     */
    suspend fun updateReservationStatus(
        reservationId: String,
        status: String
    ): Result<Unit> = runCatching {
        reservationsCol.document(reservationId).update(
            mapOf(
                "status" to status,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    /**
     * Cancels a reservation.
     */
    suspend fun cancelReservation(reservationId: String): Result<Unit> =
        updateReservationStatus(reservationId, "cancelled")

    /**
     * Confirms a reservation.
     */
    suspend fun confirmReservation(reservationId: String): Result<Unit> =
        updateReservationStatus(reservationId, "confirmed")

    /**
     * Completes a reservation.
     */
    suspend fun completeReservation(reservationId: String): Result<Unit> =
        updateReservationStatus(reservationId, "completed")

    /**
     * Deletes a reservation.
     */
    suspend fun deleteReservation(reservationId: String): Result<Unit> = runCatching {
        reservationsCol.document(reservationId).delete().await()
    }

    /**
     * Checks if a guest has an active reservation for a specific listing.
     */
    suspend fun hasActiveReservation(guestId: String, listingId: String): Boolean = runCatching {
        val snapshot = reservationsCol
            .whereEqualTo("guestId", guestId)
            .whereEqualTo("listingId", listingId)
            .whereIn("status", listOf("pending", "confirmed"))
            .get()
            .await()
        snapshot.documents.isNotEmpty()
    }.getOrDefault(false)

    /**
     * Gets the count of active reservations for a listing.
     */
    suspend fun getActiveReservationCount(listingId: String): Int = runCatching {
        val snapshot = reservationsCol
            .whereEqualTo("listingId", listingId)
            .whereIn("status", listOf("pending", "confirmed"))
            .get()
            .await()
        snapshot.size()
    }.getOrDefault(0)
}

/**
 * Converts a Reservation to a Firestore-compatible map.
 */
private fun Reservation.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "listingId" to listingId,
        "listingTitle" to listingTitle,
        "listingImageUrl" to listingImageUrl,
        "guestId" to guestId,
        "guestName" to guestName,
        "hostId" to hostId,
        "hostName" to hostName,
        "checkInDate" to checkInDate,
        "checkOutDate" to checkOutDate,
        "numberOfGuests" to numberOfGuests,
        "totalPrice" to totalPrice,
        "status" to status,
        "paymentStatus" to paymentStatus,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    ).filterValues { it != null }
}
