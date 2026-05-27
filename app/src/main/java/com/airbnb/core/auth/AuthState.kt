package com.airbnb.core.auth

import com.airbnb.data.model.User

sealed class AuthState {
    data object Guest : AuthState()

    /**
     * Firebase has a current user, but the profile document is still loading/refreshing.
     */
    data class Loading(val uid: String) : AuthState()

    /**
     * Firebase has a current user. If the Firestore profile couldn't be loaded yet,
     * [user] may be null (treat as "authenticated but profile unresolved").
     */
    data class Authenticated(val uid: String, val user: User?) : AuthState()
}

