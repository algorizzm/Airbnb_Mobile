package com.airbnb.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.airbnb.data.model.User
import com.airbnb.utils.PublicCodeGenerator

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun loginWithGoogle(
        idToken: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    if (firebaseUser != null) {
                        val uid = firebaseUser.uid
                        val docRef = FirebaseFirestore.getInstance().collection("users").document(uid)
                        docRef.get().addOnCompleteListener { docTask ->
                            if (docTask.isSuccessful) {
                                val document = docTask.result
                                if (document != null && !document.exists()) {
                                    val userCode = PublicCodeGenerator.generateUserCode()
                                    val displayName = firebaseUser.displayName.orEmpty()
                                    val profileImageUrl = firebaseUser.photoUrl?.toString().orEmpty()

                                    val userMap = hashMapOf(
                                        "id" to uid,
                                        "userId" to uid,
                                        "userCode" to userCode,
                                        "name" to displayName,
                                        "displayName" to displayName,
                                        "email" to firebaseUser.email.orEmpty(),
                                        "profileImage" to profileImageUrl,
                                        "profileImageUrl" to profileImageUrl,
                                        "role" to "traveler",
                                        "createdAt" to System.currentTimeMillis(),
                                        "hostingEnabled" to false,
                                        "hostModeEnabled" to false,
                                        "location" to "Cebu, Philippines"
                                    )
                                    docRef.set(userMap)
                                        .addOnSuccessListener {
                                            onSuccess(firebaseUser)
                                        }
                                        .addOnFailureListener { e ->
                                            onFailure(e)
                                        }
                                } else {
                                    onSuccess(firebaseUser)
                                }
                            } else {
                                onSuccess(firebaseUser)
                            }
                        }
                    } else {
                        onFailure(Exception("Firebase user is null"))
                    }
                } else {
                    onFailure(task.exception ?: Exception("Google sign-in failed"))
                }
            }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure(task.exception ?: Exception("Login failed"))
                }
            }
    }

    fun signup(
        name: String,
        email: String,
        password: String,
        role: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val user = result.user

                if (user == null) {
                    onFailure(Exception("User creation failed: user is null"))
                    return@addOnSuccessListener
                }

                val uid = user.uid

                val userData = User(
                    id = uid,
                    name = name,
                    email = email,
                    role = role.lowercase(),
                    userCode = PublicCodeGenerator.generateUserCode()
                )

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(userData)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onFailure(e)
                    }

            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getCurrentUser() = auth.currentUser

    fun logout() {
        auth.signOut()
    }
}