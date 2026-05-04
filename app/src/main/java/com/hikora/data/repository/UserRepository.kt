package com.hikora.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hikora.data.model.User

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

    fun updateUserName(newName: String, onResult: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onResult(false)
            return
        }

        db.collection("users")
            .document(uid)
            .update("name", newName)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }
}