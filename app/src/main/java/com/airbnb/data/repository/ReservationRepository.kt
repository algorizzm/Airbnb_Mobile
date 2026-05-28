package com.airbnb.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.airbnb.data.model.BlockedDate
import com.airbnb.data.model.Reservation
import com.airbnb.data.model.ReservationStatus
import com.airbnb.utils.PublicCodeGenerator
import com.airbnb.utils.ReservationDateValidator
import com.airbnb.utils.ReservationConflictValidator
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
     * 
     * IMPORTANT: This method performs conflict validation before creating the reservation.
     * It checks for:
     * - Overlapping reservations
     * - Blocked dates
     * - Invalid date ranges
     * - Past dates
     * 
     * This is the final validation layer before Firestore write.
     */
    suspend fun createReservation(reservation: Reservation): Result<String> = runCatching {
        val checkInDate = reservation.checkInDate?.toDate()
            ?: error("Check-in date is required")
        val checkOutDate = reservation.checkOutDate?.toDate()
            ?: error("Check-out date is required")

        if (!ReservationDateValidator.isDateRangeValid(checkInDate, checkOutDate)) {
            error("Invalid reservation date range")
        }

        // CRITICAL: Perform final conflict validation before write
        // This protects against race conditions and stale UI state
        val existingReservations = getReservationsForListing(reservation.listingId)
        val blockedDates = getBlockedDatesForListing(reservation.listingId)
        
        val validationResult = ReservationConflictValidator.validateReservation(
            checkInDate = reservation.checkInDate!!,
            checkOutDate = reservation.checkOutDate!!,
            existingReservations = existingReservations,
            blockedDates = blockedDates
        )
        
        if (validationResult is ReservationConflictValidator.ValidationResult.Invalid) {
            error(validationResult.reason)
        }

        val data = reservation.copy(
            id = "",
            status = ReservationStatus.PENDING,
            paymentStatus = "unpaid",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now(),
            reservationCode = reservation.reservationCode ?: PublicCodeGenerator.generateReservationCode()
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
        updateReservationStatus(reservationId, ReservationStatus.CANCELLED)

    /**
     * Confirms a reservation.
     */
    suspend fun confirmReservation(reservationId: String): Result<Unit> =
        updateReservationStatus(reservationId, ReservationStatus.CONFIRMED)

    /**
     * Rejects a reservation.
     */
    suspend fun rejectReservation(reservationId: String): Result<Unit> =
        updateReservationStatus(reservationId, ReservationStatus.REJECTED)

    /**
     * Completes a reservation.
     */
    suspend fun completeReservation(reservationId: String): Result<Unit> =
        updateReservationStatus(reservationId, ReservationStatus.COMPLETED)

    /**
     * Checks in a traveler to their reservation.
     * 
     * Rules:
     * - Reservation must be CONFIRMED or UPCOMING
     * - Current date must be >= check-in date
     * - checkedIn must be false
     * 
     * Effects:
     * - Sets checkedIn = true
     * - Sets status = ACTIVE_STAY
     * - Updates updatedAt timestamp
     */
    suspend fun checkInReservation(reservationId: String): Result<Unit> = runCatching {
        val reservation = getReservation(reservationId).getOrThrow()
        
        // Validate check-in eligibility
        if (reservation.status !in listOf(ReservationStatus.CONFIRMED, ReservationStatus.UPCOMING)) {
            error("Cannot check in: reservation status is ${reservation.status}")
        }
        
        if (reservation.checkedIn) {
            error("Already checked in")
        }
        
        val checkInDate = reservation.checkInDate?.toDate()
            ?: error("Check-in date not found")
        
        val now = java.util.Date()
        if (now.before(checkInDate)) {
            error("Cannot check in before check-in date")
        }
        
        // Perform check-in
        reservationsCol.document(reservationId).update(
            mapOf(
                "checkedIn" to true,
                "status" to ReservationStatus.ACTIVE_STAY,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    /**
     * Checks out a traveler from their reservation.
     * 
     * Rules:
     * - checkedIn must be true
     * - checkedOut must be false
     * - status must be ACTIVE_STAY
     * 
     * Effects:
     * - Sets checkedOut = true
     * - Sets status = COMPLETED
     * - Updates updatedAt timestamp
     */
    suspend fun checkOutReservation(reservationId: String): Result<Unit> = runCatching {
        val reservation = getReservation(reservationId).getOrThrow()
        
        // Validate check-out eligibility
        if (!reservation.checkedIn) {
            error("Cannot check out: not checked in")
        }
        
        if (reservation.checkedOut) {
            error("Already checked out")
        }
        
        if (reservation.status != ReservationStatus.ACTIVE_STAY) {
            error("Cannot check out: reservation status is ${reservation.status}")
        }
        
        // Perform check-out
        reservationsCol.document(reservationId).update(
            mapOf(
                "checkedOut" to true,
                "status" to ReservationStatus.COMPLETED,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

    /**
     * Performs early check-out before the scheduled check-out date.
     * 
     * Rules:
     * - checkedIn must be true
     * - checkedOut must be false
     * - current date must be < check-out date
     * 
     * Effects:
     * - Sets checkedOut = true
     * - Sets status = COMPLETED
     * - Updates updatedAt timestamp
     * 
     * Note: This sprint does NOT handle refunds or pricing adjustments.
     * Early checkout is lifecycle-only for now.
     */
    suspend fun earlyCheckOutReservation(reservationId: String): Result<Unit> = runCatching {
        val reservation = getReservation(reservationId).getOrThrow()
        
        // Validate early check-out eligibility
        if (!reservation.checkedIn) {
            error("Cannot check out: not checked in")
        }
        
        if (reservation.checkedOut) {
            error("Already checked out")
        }
        
        val checkOutDate = reservation.checkOutDate?.toDate()
            ?: error("Check-out date not found")
        
        val now = java.util.Date()
        if (!now.before(checkOutDate)) {
            error("Cannot perform early check-out: check-out date has passed")
        }
        
        // Perform early check-out (same as regular check-out for now)
        reservationsCol.document(reservationId).update(
            mapOf(
                "checkedOut" to true,
                "status" to ReservationStatus.COMPLETED,
                "updatedAt" to Timestamp.now()
            )
        ).await()
    }

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
            .whereIn("status", listOf(ReservationStatus.PENDING, ReservationStatus.CONFIRMED))
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
            .whereIn("status", listOf(ReservationStatus.PENDING, ReservationStatus.CONFIRMED))
            .get()
            .await()
        snapshot.size()
    }.getOrDefault(0)
    
    /**
     * Gets all reservations for a listing (for conflict validation).
     * Internal helper method used by conflict validator.
     */
    private suspend fun getReservationsForListing(listingId: String): List<Reservation> {
        val snapshot = reservationsCol
            .whereEqualTo("listingId", listingId)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Reservation::class.java)?.copy(id = doc.id)
        }
    }
    
    /**
     * Gets all blocked dates for a listing (for conflict validation).
     * Internal helper method used by conflict validator.
     */
    private suspend fun getBlockedDatesForListing(listingId: String): List<BlockedDate> {
        val snapshot = db.collection("blocked_dates")
            .whereEqualTo("listingId", listingId)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(BlockedDate::class.java)?.copy(id = doc.id)
        }
    }
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
        "guestAvatarUrl" to guestAvatarUrl,
        "hostId" to hostId,
        "hostName" to hostName,
        "hostAvatarUrl" to hostAvatarUrl,
        "checkInDate" to checkInDate,
        "checkOutDate" to checkOutDate,
        "numberOfGuests" to numberOfGuests,
        "totalPrice" to totalPrice,
        "status" to status,
        "paymentStatus" to paymentStatus,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "reservationCode" to reservationCode,
        "checkedIn" to checkedIn,
        "checkedOut" to checkedOut,
        "reviewSubmitted" to reviewSubmitted
    ).filterValues { it != null }
}
