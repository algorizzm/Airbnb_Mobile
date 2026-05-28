package com.airbnb.data.repository

import com.airbnb.data.model.BlockedDate
import com.airbnb.data.model.AvailabilityRange
import com.airbnb.data.model.Reservation
import com.airbnb.utils.CalendarAvailabilityMapper
import com.airbnb.utils.ReservationConflictValidator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Repository for managing calendar availability, blocked dates, and date conflicts.
 * 
 * This repository is the central authority for:
 * - Host calendar blocking
 * - Availability calculation
 * - Conflict detection
 */
class CalendarRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val reservationRepository: ReservationRepository = ReservationRepository(db)
) {
    
    private val blockedDatesCol get() = db.collection("blocked_dates")
    
    /**
     * Observes all blocked dates for a specific listing in real-time.
     */
    fun observeBlockedDatesForListing(listingId: String): Flow<List<BlockedDate>> = callbackFlow {
        val registration = blockedDatesCol
            .whereEqualTo("listingId", listingId)
            .orderBy("startDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(BlockedDate::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }
    
    /**
     * Observes all blocked dates for a specific host across all their listings.
     */
    fun observeBlockedDatesForHost(hostId: String): Flow<List<BlockedDate>> = callbackFlow {
        val registration = blockedDatesCol
            .whereEqualTo("hostId", hostId)
            .orderBy("startDate", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(BlockedDate::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }
    
    /**
     * Gets blocked dates for a listing (one-time fetch).
     */
    suspend fun getBlockedDatesForListing(listingId: String): Result<List<BlockedDate>> = runCatching {
        val snapshot = blockedDatesCol
            .whereEqualTo("listingId", listingId)
            .orderBy("startDate", Query.Direction.ASCENDING)
            .get()
            .await()
        
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(BlockedDate::class.java)?.copy(id = doc.id)
        }
    }
    
    /**
     * Creates a new blocked date range for a listing.
     * Validates that the range doesn't conflict with existing reservations.
     */
    suspend fun createBlockedDate(
        listingId: String,
        hostId: String,
        startDate: Timestamp,
        endDate: Timestamp,
        reason: String = ""
    ): Result<String> = runCatching {
        // Fetch existing reservations for validation
        val reservations = getActiveReservationsForListing(listingId)
        
        // Validate that blocked date doesn't conflict with existing reservations
        val validationResult = ReservationConflictValidator.validateBlockedDateRange(
            startDate = startDate,
            endDate = endDate,
            existingReservations = reservations
        )
        
        if (validationResult is ReservationConflictValidator.ValidationResult.Invalid) {
            error(validationResult.reason)
        }
        
        // Create the blocked date
        val blockedDate = BlockedDate(
            id = "",
            listingId = listingId,
            hostId = hostId,
            startDate = startDate,
            endDate = endDate,
            reason = reason,
            createdAt = Timestamp.now()
        )
        
        val ref = blockedDatesCol.document()
        ref.set(blockedDate.toFirestoreMap()).await()
        ref.id
    }
    
    /**
     * Removes a blocked date range.
     */
    suspend fun removeBlockedDate(blockedDateId: String): Result<Unit> = runCatching {
        blockedDatesCol.document(blockedDateId).delete().await()
    }
    
    /**
     * Gets availability ranges for a listing within a date range.
     * Combines reservations and blocked dates into a unified availability view.
     */
    suspend fun getAvailabilityRanges(
        listingId: String,
        rangeStart: Date,
        rangeEnd: Date
    ): Result<List<AvailabilityRange>> = runCatching {
        val reservations = getActiveReservationsForListing(listingId)
        val blockedDates = getBlockedDatesForListing(listingId).getOrThrow()
        
        CalendarAvailabilityMapper.generateAvailabilityRanges(
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            reservations = reservations,
            blockedDates = blockedDates
        )
    }
    
    /**
     * Gets a list of unavailable dates for calendar UI.
     */
    suspend fun getUnavailableDates(
        listingId: String,
        rangeStart: Date,
        rangeEnd: Date
    ): Result<List<Date>> = runCatching {
        val reservations = getActiveReservationsForListing(listingId)
        val blockedDates = getBlockedDatesForListing(listingId).getOrThrow()
        
        CalendarAvailabilityMapper.getDisabledDates(
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            reservations = reservations,
            blockedDates = blockedDates
        )
    }
    
    /**
     * Validates if a date range is available for booking.
     */
    suspend fun validateAvailability(
        listingId: String,
        checkInDate: Timestamp,
        checkOutDate: Timestamp
    ): Result<ReservationConflictValidator.ValidationResult> = runCatching {
        val reservations = getActiveReservationsForListing(listingId)
        val blockedDates = getBlockedDatesForListing(listingId).getOrThrow()
        
        ReservationConflictValidator.validateReservation(
            checkInDate = checkInDate,
            checkOutDate = checkOutDate,
            existingReservations = reservations,
            blockedDates = blockedDates
        )
    }
    
    /**
     * Helper method to get active reservations for a listing.
     */
    private suspend fun getActiveReservationsForListing(listingId: String): List<Reservation> {
        val snapshot = db.collection("reservations")
            .whereEqualTo("listingId", listingId)
            .get()
            .await()
        
        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Reservation::class.java)?.copy(id = doc.id)
        }
    }
}

/**
 * Converts a BlockedDate to a Firestore-compatible map.
 */
private fun BlockedDate.toFirestoreMap(): Map<String, Any?> {
    return mapOf(
        "listingId" to listingId,
        "hostId" to hostId,
        "startDate" to startDate,
        "endDate" to endDate,
        "reason" to reason,
        "createdAt" to createdAt
    ).filterValues { it != null }
}
