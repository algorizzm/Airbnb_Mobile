package com.verdant.core.auth

import com.google.firebase.auth.FirebaseAuth
import com.verdant.data.session.UserSessionManager
import kotlinx.coroutines.flow.StateFlow

object AuthManager {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    fun isAuthenticated(): Boolean = auth.currentUser != null

    fun authState(): StateFlow<AuthState> = UserSessionManager.authState

    fun stateSnapshot(): AuthState = UserSessionManager.authState.value

    fun signOut() {
        auth.signOut()
    }

    fun currentUserFlow() = UserSessionManager.currentUser
}

