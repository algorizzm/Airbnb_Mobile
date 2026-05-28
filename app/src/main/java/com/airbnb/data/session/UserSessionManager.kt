package com.airbnb.data.session

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.airbnb.core.auth.AuthState
import com.airbnb.data.model.User
import com.airbnb.core.mode.AppModeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

object UserSessionManager {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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

    // Active Firestore real-time listener for the signed-in user document
    private var userDocListener: ListenerRegistration? = null

    private val authListener = FirebaseAuth.AuthStateListener { firebaseUser ->
        val uid = firebaseUser?.uid
        if (uid != null) {
            attachUserListener(uid)
        } else {
            detachUserListener()
            _authState.value = AuthState.Guest
            AppModeManager.resetToTraveler()
            resetGuestPrompt()
        }
    }

    fun start() {
        auth.addAuthStateListener(authListener)
    }

    /**
     * Force a re-attach of the Firestore listener (e.g. after a manual write).
     * Because we use a snapshot listener, this is rarely needed — Firestore
     * pushes updates automatically. Kept for compatibility.
     */
    fun refresh() {
        val uid = auth.currentUser?.uid ?: return
        // Re-attach triggers an immediate snapshot delivery with fresh data
        detachUserListener()
        attachUserListener(uid)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun attachUserListener(uid: String) {
        // Set loading only on first attach (not on re-attach from refresh)
        if (_authState.value !is AuthState.Authenticated) {
            _authState.value = AuthState.Loading(uid)
        }

        AppModeManager.init(auth.app.applicationContext)
        AppModeManager.restoreSavedMode()

        userDocListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Keep authenticated but mark user as unresolved
                    _authState.value = AuthState.Authenticated(uid = uid, user = null)
                    return@addSnapshotListener
                }
                val user =
                    snapshot
                        ?.toObject(User::class.java)
                        ?.copy(id = uid)

                _authState.value =
                    AuthState.Authenticated(
                        uid = uid,
                        user = user
                    )

                AppModeManager.restoreSavedMode()
            }
    }

    private fun detachUserListener() {
        userDocListener?.remove()
        userDocListener = null
    }
}