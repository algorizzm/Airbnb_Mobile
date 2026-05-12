package com.verdant.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.verdant.data.model.Hike
import com.verdant.utils.HikeStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class HikeRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val hikesCol get() = db.collection("hikes")

    fun observeHike(hikeId: String): Flow<Hike?> = callbackFlow {
        val registration = hikesCol.document(hikeId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val hike = snapshot?.toObject(Hike::class.java)?.copy(id = snapshot.id)
            trySend(hike)
        }
        awaitClose { registration.remove() }
    }

    fun observeHikes(): Flow<List<Hike>> = callbackFlow {
        val registration = hikesCol
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Hike::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun getHike(hikeId: String): Result<Hike> = runCatching {
        val doc = hikesCol.document(hikeId).get().await()
        doc.toObject(Hike::class.java)?.copy(id = doc.id)
            ?: error("Hike not found")
    }

    suspend fun createHike(hike: Hike): Result<String> = runCatching {
        val data = hike.copy(
            id = "",
            createdAt = hike.createdAt ?: Timestamp.now(),
            status = hike.status.ifBlank { HikeStatus.OPEN }
        )
        val ref = hikesCol.document()
        ref.set(data.toFirestoreMap(includeCreatedAt = true)).await()
        ref.id
    }

    suspend fun updateHike(hike: Hike): Result<Unit> = runCatching {
        if (hike.id.isBlank()) error("Missing hike id")
        hikesCol.document(hike.id).set(
            hike.toFirestoreMap(includeCreatedAt = false),
            SetOptions.merge()
        ).await()
    }

    suspend fun deleteHike(hikeId: String): Result<Unit> = runCatching {
        val bookingSnap = db.collection("bookings").whereEqualTo("hikeId", hikeId).get().await()
        val batch = db.batch()
        bookingSnap.documents.forEach { doc -> batch.delete(doc.reference) }
        batch.delete(hikesCol.document(hikeId))
        batch.commit().await()
    }

    suspend fun updateHikeStatus(hikeId: String, status: String): Result<Unit> = runCatching {
        hikesCol.document(hikeId).update("status", status).await()
    }

    fun observeHikesForGuide(guideId: String): Flow<List<Hike>> = callbackFlow {
        val registration = hikesCol
            .whereEqualTo("guideId", guideId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Hike::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun startHike(hikeId: String): Result<Unit> =
        updateHikeStatus(hikeId, HikeStatus.ONGOING)

    suspend fun completeHike(hikeId: String): Result<Unit> =
        updateHikeStatus(hikeId, HikeStatus.COMPLETED)
}

private fun Hike.toFirestoreMap(includeCreatedAt: Boolean): Map<String, Any?> {
    val meetup = meetupPoint.ifBlank { location }
    val map = mutableMapOf<String, Any?>(
        "title" to title,
        "description" to description,
        "location" to meetup,
        "meetupPoint" to meetupPoint.ifBlank { null },
        "destination" to destination.ifBlank { null },
        "difficulty" to difficulty,
        "distanceKm" to distanceKm,
        "estimatedDistanceKm" to estimatedDistanceKm,
        "elevationM" to elevationM,
        "price" to price,
        "durationHours" to durationHours,
        "guideId" to guideId,
        "guideName" to guideName,
        "maxParticipants" to maxParticipants,
        "status" to status,
        "imageUrl" to imageUrl,
        "galleryImageUrls" to galleryImageUrls,
        "startDateTime" to startDateTime,
        "endDateTime" to endDateTime,
        "inclusions" to inclusions,
        "requirements" to requirements,
        "tags" to tags,
        "paymentMethods" to paymentMethods,
        "pricingNotes" to pricingNotes
    )
    if (includeCreatedAt) {
        map["createdAt"] = createdAt
    }
    return map.filterValues { it != null }
}
