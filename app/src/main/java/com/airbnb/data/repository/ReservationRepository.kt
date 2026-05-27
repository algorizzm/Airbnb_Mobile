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
     * Observes reservations for a specific listing in real-time.
     */
    fun observeReservationsForListing(listingId: String): Flow<List<Reservation>> = callbackFlow {
        val reg = reservationsCol
            .whereEqualTo("listingId", listingId)
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
        awaitClose { reg.remove() }
    }

    /**
     * Observes reservations for a specific guest (user) in real-time.
     */
    fun observeReservationsForGuest(guestId: String): Flow<List<Reservation>> = callbackFlow {
        val reg = reservationsCol
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
        awaitClose { reg.remove() }
    }

    /**
     * Observes reservations for a specific host in real-time.
     */
    fun observeReservationsForHost(hostId: String): Flow<List<Reservation>> = callbackFlow {
        val reg = reservationsCol
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
        awaitClose { reg.remove() }
    }

    /**
     * Observes active reservations (pending or confirmed) for a guest.
     */
    fun observeActiveReservationsForGuest(guestId: String): Flow<List<Reservation>> = callbackFlow {
        val reg = reservationsCol
            .whereEqualTo("guestId", guestId)
            .whereIn("status", listOf("pending", "confirmed"))
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
        awaitClose { reg.remove() }
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
     * Checks if a guest already has an active reservation for a specific listing.
     */
    suspend fun findActiveReservationForGuestOnListing(
        listingId: String,
        guestId: String
    ): Reservation? {
        val snap = runCatching {
            reservationsCol
                .whereEqualTo("listingId", listingId)
                .whereEqualTo("guestId", guestId)
                .get()
                .await()
        }.getOrNull() ?: return null
        
        return snap.documents
            .mapNotNull { it.toObject(Reservation::class.java)?.copy(id = it.id) }
            .firstOrNull { it.isActive() }
    }

    /**
     * Creates a new reservation.
     */
    suspend fun createReservation(
        listingId: String,
        listingTitle: String,
        listingImageUrl: String,
        guestId: String,
        guestName: String,
        hostId: String,
        hostName: String,
        checkInDate: Timestamp,
        checkOutDate: Timestamp,
        numberOfGuests: Int,
        totalPrice: Double
    ): Result<String> = runCatching {
        // Check if guest already has an active reservation for this listing
        if (findActiveReservationForGuestOnListing(listingId, guestId) != null) {
            error("You already have an active reservation for this listing.")
        }

        val ref = reservationsCol.document()
        ref.set(
            mapOf(
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
                "status" to "pending",
                "paymentStatus" to "unpaid",
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
        ).await()
        ref.id
    }

    /**
     * Updates the status of a reservation.
     */
    suspend fun updateReservationStatus(reservationId: String, status: String): Result<Unit> = runCatching {
        reservationsCol.document(reservationId).update(
            mapOf(
                "status" to status,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    /**
     * Updates the payment status of a reservation.
     */
    suspend fun updatePaymentStatus(reservationId: String, paymentStatus: String): Result<Unit> = runCatching {
        reservationsCol.document(reservationId).update(
            mapOf(
                "paymentStatus" to paymentStatus,
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
     * Confirms a reservation (host accepts).
     */
    suspend fun confirmReservation(reservationId: String): Result<Unit> =
        updateReservationStatus(reservationId, "confirmed")

    /**
     * Completes a reservation (after checkout).
     */
    suspend fun completeReservation(reservationId: String): Result<Unit> =
        updateReservationStatus(reservationId, "completed")

    /**
     * Gets the count of confirmed reservations for a specific listing.
     */
    suspend fun countConfirmedForListing(listingId: String): Int = runCatching {
        val snap = reservationsCol
            .whereEqualTo("listingId", listingId)
            .whereEqualTo("status", "confirmed")
            .get()
            .await()
        snap.size()
    }.getOrDefault(0)

    /**
     * Gets all confirmed reservations for a listing (one-time fetch).
     */
    suspend fun getConfirmedReservationsForListing(listingId: String): Result<List<Reservation>> = runCatching {
        val snap = reservationsCol
            .whereEqualTo("listingId", listingId)
            .whereEqualTo("status", "confirmed")
            .get()
            .await()
        snap.documents.mapNotNull { doc ->
            doc.toObject(Reservation::class.java)?.copy(id = doc.id)
        }
    }

    /**
     * Completes all confirmed reservations for a listing (batch operation).
     */
    suspend fun completeConfirmedReservationsForListing(listingId: String): Result<Unit> = runCatching {
        val snap = reservationsCol
            .whereEqualTo("listingId", listingId)
            .whereEqualTo("status", "confirmed")
            .get()
            .await()
        
        val batch = db.batch()
        snap.documents.forEach { doc ->
            batch.update(
                doc.reference,
                mapOf(
                    "status" to "completed",
                    "updatedAt" to Timestamp.now()
                )
            )
        }
        batch.commit().await()
    }
}
