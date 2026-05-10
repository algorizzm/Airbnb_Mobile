package com.verdant.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.verdant.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val usersCol get() = db.collection("users")

    fun getCurrentUser(onResult: (User?) -> Unit) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onResult(null)
            return
        }

        usersCol.document(uid)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)?.copy(id = uid)
                onResult(user)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val registration = usersCol.document(userId)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val user = snapshot
                    ?.toObject(User::class.java)
                    ?.copy(id = userId)

                trySend(user)
            }

        awaitClose { registration.remove() }
    }

    suspend fun getUser(userId: String): Result<User> = runCatching {
        val doc = usersCol.document(userId).get().await()

        doc.toObject(User::class.java)
            ?.copy(id = userId)
            ?: error("User not found")
    }

    fun clearUserSession() {
        auth.signOut()
    }

    fun updateUserBio(
        newBio: String,
        onResult: (Boolean) -> Unit
    ) {

        val uid = auth.currentUser?.uid

        if (uid == null) {
            onResult(false)
            return
        }

        usersCol.document(uid)
            .update("bio", newBio)
            .addOnSuccessListener {
                onResult(true)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    suspend fun updateBio(
        uid: String,
        bio: String
    ): Result<Unit> = runCatching {
        usersCol.document(uid)
            .update("bio", bio)
            .await()
    }

    suspend fun updateAvatar(
        uid: String,
        url: String
    ): Result<Unit> = runCatching {
        usersCol.document(uid)
            .update("profileImage", url)
            .await()
    }

    suspend fun updateBanner(
        uid: String,
        url: String
    ): Result<Unit> = runCatching {
        usersCol.document(uid)
            .update("bannerImage", url)
            .await()
    }

    suspend fun updateUserStats(
        userId: String,
        totalHikesAdd: Int = 0,
        totalDistanceAdd: Double = 0.0,
        summitsAdd: Int = 0
    ): Result<Unit> = runCatching {

        val user = getUser(userId).getOrNull()
            ?: error("User not found")

        usersCol.document(userId)
            .update(
                mapOf(
                    "totalHikes" to (user.totalHikes + totalHikesAdd),
                    "totalDistance" to (user.totalDistance + totalDistanceAdd),
                    "totalSummits" to (user.totalSummits + summitsAdd)
                )
            )
            .await()
    }
}