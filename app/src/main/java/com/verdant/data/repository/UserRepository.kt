package com.verdant.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.verdant.data.model.User

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getCurrentUser(onResult: (User?) -> Unit) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onResult(null)
            return
        }

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                onResult(user)
            }
            .addOnFailureListener {
                onResult(null)
            }
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

        db.collection("users")
            .document(uid)
            .update("bio", newBio)
            .addOnSuccessListener {

                onResult(true)
            }
            .addOnFailureListener {

                onResult(false)
            }
    }
}