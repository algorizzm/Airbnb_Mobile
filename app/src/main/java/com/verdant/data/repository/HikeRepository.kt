package com.verdant.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
        ref.set(
            mapOf(
                "title" to data.title,
                "description" to data.description,
                "location" to data.location,
                "difficulty" to data.difficulty,
                "distanceKm" to data.distanceKm,
                "price" to data.price,
                "guideId" to data.guideId,
                "guideName" to data.guideName,
                "maxParticipants" to data.maxParticipants,
                "status" to data.status,
                "createdAt" to data.createdAt!!
            )
        ).await()
        ref.id
    }

    suspend fun updateHike(hike: Hike): Result<Unit> = runCatching {
        if (hike.id.isBlank()) error("Missing hike id")
        hikesCol.document(hike.id).set(
            mapOf(
                "title" to hike.title,
                "description" to hike.description,
                "location" to hike.location,
                "difficulty" to hike.difficulty,
                "distanceKm" to hike.distanceKm,
                "price" to hike.price,
                "guideId" to hike.guideId,
                "guideName" to hike.guideName,
                "maxParticipants" to hike.maxParticipants,
                "status" to hike.status
            ),
            com.google.firebase.firestore.SetOptions.merge()
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
