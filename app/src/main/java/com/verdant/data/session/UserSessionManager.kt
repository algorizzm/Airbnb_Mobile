package com.verdant.data.session

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.verdant.core.auth.AuthState
import com.verdant.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object UserSessionManager {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // =========================================================
    // AUTH STATE
    // =========================================================

    private val _authState = MutableStateFlow<AuthState>(AuthState.Guest)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser: StateFlow<User?> = authState
        .map { state -> (state as? AuthState.Authenticated)?.user }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    // =========================================================
    // GUEST PROMPT SESSION STATE
    // =========================================================

    private val _guestPromptDismissed = MutableStateFlow(false)
    val guestPromptDismissed: StateFlow<Boolean> =
        _guestPromptDismissed.asStateFlow()

    fun dismissGuestPrompt() {
        _guestPromptDismissed.value = true
    }

    fun resetGuestPrompt() {
        _guestPromptDismissed.value = false
    }

    // =========================================================
    // FIREBASE AUTH LISTENER
    // =========================================================

    private val authListener = FirebaseAuth.AuthStateListener { firebaseUser ->

        val uid = firebaseUser?.uid

        if (uid != null) {

            scope.launch {

                _authState.value = AuthState.Loading(uid)

                loadUser(uid)
            }

        } else {

            _authState.value = AuthState.Guest

            // Reset popup every fresh guest session
            resetGuestPrompt()
        }
    }

    fun start() {
        auth.addAuthStateListener(authListener)
    }

    fun refresh() {

        val uid = auth.currentUser?.uid ?: return

        scope.launch {

            _authState.value = AuthState.Loading(uid)

            loadUser(uid)
        }
    }

    private suspend fun loadUser(uid: String) {

        runCatching {

            val snap = db.collection("users")
                .document(uid)
                .get()
                .await()

            val user = snap.toObject(User::class.java)
                ?.copy(id = uid)

            _authState.value =
                AuthState.Authenticated(
                    uid = uid,
                    user = user
                )

        }.onFailure {

            // Firebase is authenticated but profile failed to load
            _authState.value =
                AuthState.Authenticated(
                    uid = uid,
                    user = null
                )
        }
    }
}