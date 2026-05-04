package com.hikora.data.session

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hikora.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Central session: current Firebase user profile from Firestore (includes [User.role]).
 */
object UserSessionManager {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseUser ->
        val uid = firebaseUser?.uid
        if (uid != null) {
            scope.launch {
                loadUser(uid)
            }
        }
    }

    fun start() {
        auth.addAuthStateListener(authListener)
    }

    fun refresh() {
        val uid = auth.currentUser?.uid ?: return
        scope.launch { loadUser(uid) }
    }

    private suspend fun loadUser(uid: String) {
        runCatching {
            val snap = db.collection("users").document(uid).get().await()
            val user = snap.toObject(User::class.java)?.copy(id = uid)
            _currentUser.value = user
        }.onFailure {
            _currentUser.value = null
        }
    }
}
