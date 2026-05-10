package com.verdant.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.verdant.data.model.User
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getCurrentUser(onResult: (User?) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(null)
        db.collection("users").document(uid).get()
            .addOnSuccessListener { onResult(it.toObject(User::class.java)?.copy(id = uid)) }
            .addOnFailureListener { onResult(null) }
    }

    fun clearUserSession() = auth.signOut()

    fun updateUserName(newName: String, onResult: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return onResult(false)
        db.collection("users").document(uid)
            .update("name", newName)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

    suspend fun updateAvatar(uid: String, url: String): Result<Unit> = runCatching {
        db.collection("users").document(uid).update("profileImage", url).await()
    }

    suspend fun updateBanner(uid: String, url: String): Result<Unit> = runCatching {
        db.collection("users").document(uid).update("bannerImage", url).await()
    }

    suspend fun updateBio(uid: String, bio: String): Result<Unit> = runCatching {
        db.collection("users").document(uid).update("bio", bio).await()
    }
}
