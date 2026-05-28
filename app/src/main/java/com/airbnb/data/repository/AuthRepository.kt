package com.airbnb.data.repository

import android.content.Context
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.airbnb.data.model.User
import com.airbnb.utils.PublicCodeGenerator

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // =========================================================
    // EMAIL LINK AUTHENTICATION
    // =========================================================

    fun sendSignInLinkToEmail(
        email: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // TODO: Replace with your actual Dynamic Links domain from Firebase Console
        // Firebase Console → Dynamic Links → Copy your domain
        // Example: "https://yourproject.page.link/auth"
        val dynamicLinkDomain = "https://airbnb.page.link/auth"
        
        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl(dynamicLinkDomain)
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                context.packageName,
                true,
                null
            )
            .build()

        auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnSuccessListener {
                // Save email locally for auth completion
                context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("pending_email", email)
                    .apply()
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun isSignInWithEmailLink(link: String): Boolean {
        return auth.isSignInWithEmailLink(link)
    }

    fun signInWithEmailLink(
        email: String,
        link: String,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        auth.signInWithEmailLink(email, link)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    if (firebaseUser != null) {
                        ensureUserDocument(firebaseUser, onSuccess, onFailure)
                    } else {
                        onFailure(Exception("Firebase user is null"))
                    }
                } else {
                    onFailure(task.exception ?: Exception("Email link sign-in failed"))
                }
            }
    }

    fun getPendingEmail(context: Context): String? {
        return context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .getString("pending_email", null)
    }

    fun clearPendingEmail(context: Context) {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("pending_email")
            .apply()
    }

    // =========================================================
    // GOOGLE AUTHENTICATION
    // =========================================================

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
                        ensureUserDocument(firebaseUser, onSuccess, onFailure)
                    } else {
                        onFailure(Exception("Firebase user is null"))
                    }
                } else {
                    onFailure(task.exception ?: Exception("Google sign-in failed"))
                }
            }
    }

    // =========================================================
    // USER DOCUMENT AUTO-CREATION
    // =========================================================

    private fun ensureUserDocument(
        firebaseUser: FirebaseUser,
        onSuccess: (FirebaseUser) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
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
                        "location" to ""
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