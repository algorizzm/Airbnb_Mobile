package com.verdant.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.verdant.data.model.Booking
import com.verdant.utils.BookingStatus
import com.verdant.utils.PaymentStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class BookingRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val bookingsCol get() = db.collection("bookings")

    fun observeBookingsForHike(hikeId: String): Flow<List<Booking>> = callbackFlow {
        val reg = bookingsCol
            .whereEqualTo("hikeId", hikeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }.orEmpty()
                    .sortedByDescending { it.createdAt?.seconds ?: 0 }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeBookingsForUser(userId: String): Flow<List<Booking>> = callbackFlow {
        val reg = bookingsCol
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }.orEmpty()
                    .sortedByDescending { it.createdAt?.seconds ?: 0 }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun findActiveBookingForUserOnHike(hikeId: String, userId: String): Booking? {
        val snap = runCatching {
            bookingsCol
                .whereEqualTo("hikeId", hikeId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
        }.getOrNull() ?: return null
        return snap.documents
            .mapNotNull { it.toObject(Booking::class.java)?.copy(id = it.id) }
            .firstOrNull {
                it.status == BookingStatus.PENDING || it.status == BookingStatus.APPROVED
            }
    }

    suspend fun countApprovedForHike(hikeId: String): Int = runCatching {
        val snap = bookingsCol
            .whereEqualTo("hikeId", hikeId)
            .whereEqualTo("status", BookingStatus.APPROVED)
            .get()
            .await()
        snap.size()
    }.getOrDefault(0)

    suspend fun createPendingBooking(
        hikeId: String,
        userId: String,
        userName: String,
        guideId: String
    ): Result<String> = runCatching {
        if (findActiveBookingForUserOnHike(hikeId, userId) != null) {
            error("You already have an application for this hike.")
        }
        val ref = bookingsCol.document()
        ref.set(
            mapOf(
                "hikeId" to hikeId,
                "userId" to userId,
                "userName" to userName,
                "guideId" to guideId,
                "status" to BookingStatus.PENDING,
                "paymentStatus" to PaymentStatus.UNPAID,
                "createdAt" to Timestamp.now()
            )
        ).await()
        ref.id
    }

    suspend fun updateBookingStatus(bookingId: String, status: String): Result<Unit> = runCatching {
        bookingsCol.document(bookingId).update("status", status).await()
    }

    suspend fun cancelBooking(bookingId: String): Result<Unit> =
        updateBookingStatus(bookingId, BookingStatus.CANCELLED)

    suspend fun completeApprovedBookingsForHike(hikeId: String): Result<Unit> = runCatching {
        val snap = bookingsCol
            .whereEqualTo("hikeId", hikeId)
            .whereEqualTo("status", BookingStatus.APPROVED)
            .get()
            .await()
        val batch = com.google.firebase.firestore.FirebaseFirestore.getInstance().batch()
        snap.documents.forEach { doc ->
            batch.update(doc.reference, "status", BookingStatus.COMPLETED)
        }
        batch.commit().await()
    }

    fun observeBookingsForGuide(guideId: String): Flow<List<Booking>> = callbackFlow {
        val reg = bookingsCol
            .whereEqualTo("guideId", guideId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }.orEmpty()
                    .sortedByDescending { it.createdAt?.seconds ?: 0 }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun getApprovedBookingsForHike(hikeId: String): Result<List<Booking>> = runCatching {
        val snap = bookingsCol
            .whereEqualTo("hikeId", hikeId)
            .whereEqualTo("status", BookingStatus.APPROVED)
            .get()
            .await()
        snap.documents.mapNotNull { doc ->
            doc.toObject(Booking::class.java)?.copy(id = doc.id)
        }
    }

    /**
     * Real-time flow of the user's APPROVED bookings.
     * The Track ViewModel cross-joins this with hike status to determine
     * whether the hiker is currently in an ONGOING hike.
     */
    fun observeApprovedBookingsForUser(userId: String): Flow<List<Booking>> = callbackFlow {
        val reg = bookingsCol
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", BookingStatus.APPROVED)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }
}
