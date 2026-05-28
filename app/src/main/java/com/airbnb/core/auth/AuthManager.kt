package com.airbnb.core.auth

import com.google.firebase.auth.FirebaseAuth
import com.airbnb.data.session.UserSessionManager
import kotlinx.coroutines.flow.StateFlow
import com.airbnb.core.mode.AppModeManager

object AuthManager {

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    fun isAuthenticated(): Boolean =
        auth.currentUser != null

    fun authState(): StateFlow<AuthState> =
        UserSessionManager.authState

    fun stateSnapshot(): AuthState =
        UserSessionManager.authState.value

    fun signOut() {
        AppModeManager.resetToTraveler()
        auth.signOut()
    }

    fun currentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun currentUserFlow() =
        UserSessionManager.currentUser

    // =========================================================
    // GUEST PROMPT
    // =========================================================

    fun dismissGuestPrompt() {
        UserSessionManager.dismissGuestPrompt()
    }

    fun guestPromptDismissed(): StateFlow<Boolean> {
        return UserSessionManager.guestPromptDismissed
    }
}