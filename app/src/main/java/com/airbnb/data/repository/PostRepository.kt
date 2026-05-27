package com.airbnb.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.airbnb.data.model.CompletionPost
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PostRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val postsCol get() = db.collection("posts")

    /**
     * Real-time flow of all completion posts, newest first.
     */
    fun observePosts(): Flow<List<CompletionPost>> = callbackFlow {
        val reg = postsCol
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CompletionPost::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /**
     * Creates a completion post after a hike ends.
     * Called from the ViewModel — never from a Fragment.
     */
    suspend fun createCompletionPost(
        hikeId: String,
        hikeTitle: String,
        guideId: String,
        guideName: String,
        location: String,
        distanceKm: Double,
        elevationM: Double,
        participantCount: Int,
        coverImageUrl: String
    ): Result<String> = runCatching {
        val ref = postsCol.document()
        ref.set(
            mapOf(
                "hikeId" to hikeId,
                "hikeTitle" to hikeTitle,
                "guideId" to guideId,
                "guideName" to guideName,
                "location" to location,
                "distanceKm" to distanceKm,
                "elevationM" to elevationM,
                "participantCount" to participantCount,
                "coverImageUrl" to coverImageUrl,
                "createdAt" to Timestamp.now()
            )
        ).await()
        ref.id
    }
}
