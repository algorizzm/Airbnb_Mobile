package com.airbnb.ui.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.airbnb.data.repository.AuthRepository

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableLiveData<Boolean>()
    val authState: LiveData<Boolean> = _authState

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _emailLinkSent = MutableLiveData<Boolean>()
    val emailLinkSent: LiveData<Boolean> = _emailLinkSent

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // =========================================================
    // EMAIL LINK AUTHENTICATION
    // =========================================================

    fun sendSignInLinkToEmail(email: String, context: Context) {
        _loading.postValue(true)
        try {
            repository.sendSignInLinkToEmail(
                email,
                context,
                onSuccess = {
                    _loading.postValue(false)
                    _emailLinkSent.postValue(true)
                },
                onFailure = { exception ->
                    _loading.postValue(false)
                    _error.postValue(exception.message ?: "Failed to send sign-in link")
                }
            )
        } catch (e: Exception) {
            _loading.postValue(false)
            _error.postValue(e.message ?: "Email link send crashed")
        }
    }

    fun signInWithEmailLink(email: String, link: String) {
        _loading.postValue(true)
        try {
            repository.signInWithEmailLink(
                email,
                link,
                onSuccess = {
                    _loading.postValue(false)
                    _authState.postValue(true)
                },
                onFailure = { exception ->
                    _loading.postValue(false)
                    _error.postValue(exception.message ?: "Email link sign-in failed")
                }
            )
        } catch (e: Exception) {
            _loading.postValue(false)
            _error.postValue(e.message ?: "Email link sign-in crashed")
        }
    }

    fun isSignInWithEmailLink(link: String): Boolean {
        return repository.isSignInWithEmailLink(link)
    }

    fun getPendingEmail(context: Context): String? {
        return repository.getPendingEmail(context)
    }

    fun clearPendingEmail(context: Context) {
        repository.clearPendingEmail(context)
    }

    fun resetEmailLinkSent() {
        _emailLinkSent.value = false
    }

    // =========================================================
    // GOOGLE AUTHENTICATION
    // =========================================================

    fun loginWithGoogle(idToken: String) {
        _loading.postValue(true)
        try {
            repository.loginWithGoogle(
                idToken,
                onSuccess = {
                    _loading.postValue(false)
                    _authState.postValue(true)
                },
                onFailure = { exception ->
                    _loading.postValue(false)
                    _error.postValue(exception.message ?: "Google sign-in failed")
                }
            )
        } catch (e: Exception) {
            _loading.postValue(false)
            _error.postValue(e.message ?: "Google login crashed")
        }
    }

    // =========================================================
    // MANUAL EMAIL/PASSWORD AUTHENTICATION (SPRINT 10)
    // =========================================================

    /**
     * Login with email and password.
     * Validates input before attempting authentication.
     */
    fun loginWithEmail(email: String, password: String) {
        // Validate inputs
        if (email.isBlank()) {
            _error.postValue("Email is required")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _error.postValue("Please enter a valid email address")
            return
        }
        if (password.isBlank()) {
            _error.postValue("Password is required")
            return
        }

        _loading.postValue(true)
        try {
            repository.loginWithEmail(
                email,
                password,
                onSuccess = {
                    _loading.postValue(false)
                    _authState.postValue(true)
                },
                onFailure = { exception ->
                    _loading.postValue(false)
                    _error.postValue(parseAuthError(exception))
                }
            )
        } catch (e: Exception) {
            _loading.postValue(false)
            _error.postValue(e.message ?: "Login failed")
        }
    }

    /**
     * Sign up with email and password.
     * Validates all inputs before creating account.
     */
    fun signUpWithEmail(name: String, email: String, password: String, confirmPassword: String) {
        // Validate name
        if (name.isBlank()) {
            _error.postValue("Name is required")
            return
        }
        if (name.length < 2) {
            _error.postValue("Name must be at least 2 characters")
            return
        }

        // Validate email
        if (email.isBlank()) {
            _error.postValue("Email is required")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _error.postValue("Please enter a valid email address")
            return
        }

        // Validate password
        if (password.isBlank()) {
            _error.postValue("Password is required")
            return
        }
        if (password.length < 6) {
            _error.postValue("Password must be at least 6 characters")
            return
        }

        // Validate password match
        if (password != confirmPassword) {
            _error.postValue("Passwords do not match")
            return
        }

        _loading.postValue(true)
        try {
            repository.signUpWithEmail(
                name,
                email,
                password,
                onSuccess = {
                    _loading.postValue(false)
                    _authState.postValue(true)
                },
                onFailure = { exception ->
                    _loading.postValue(false)
                    _error.postValue(parseAuthError(exception))
                }
            )
        } catch (e: Exception) {
            _loading.postValue(false)
            _error.postValue(e.message ?: "Sign up failed")
        }
    }

    /**
     * Parses Firebase auth exceptions into user-friendly messages.
     */
    private fun parseAuthError(exception: Exception): String {
        val message = exception.message ?: return "Authentication failed"
        
        return when {
            message.contains("network", ignoreCase = true) -> 
                "Network error. Please check your connection"
            message.contains("user-not-found", ignoreCase = true) -> 
                "No account found with this email"
            message.contains("wrong-password", ignoreCase = true) -> 
                "Incorrect password"
            message.contains("invalid-credential", ignoreCase = true) -> 
                "Invalid email or password"
            message.contains("email-already-in-use", ignoreCase = true) -> 
                "An account with this email already exists"
            message.contains("weak-password", ignoreCase = true) -> 
                "Password is too weak. Use at least 6 characters"
            message.contains("invalid-email", ignoreCase = true) -> 
                "Invalid email address"
            message.contains("user-disabled", ignoreCase = true) -> 
                "This account has been disabled"
            message.contains("too-many-requests", ignoreCase = true) -> 
                "Too many attempts. Please try again later"
            else -> message
        }
    }

    // =========================================================
    // LEGACY PASSWORD AUTHENTICATION (Deprecated)
    // =========================================================

    @Deprecated("Use loginWithEmail instead", ReplaceWith("loginWithEmail(email, password)"))
    fun login(email: String, password: String) {
        _loading.postValue(true)
        try {
            repository.login(
                email,
                password,
                onSuccess = {
                    _loading.postValue(false)
                    _authState.postValue(true)
                },
                onFailure = { exception ->
                    _loading.postValue(false)
                    _error.postValue(exception.message ?: "Login failed")
                }
            )
        } catch (e: Exception) {
            _loading.postValue(false)
            _error.postValue(e.message ?: "Login crashed")
        }
    }

    @Deprecated("Use signUpWithEmail instead", ReplaceWith("signUpWithEmail(name, email, password, confirmPassword)"))
    fun signup(name: String, email: String, password: String, role: String) {
        _loading.postValue(true)
        repository.signup(
            name,
            email,
            password,
            role,
            onSuccess = {
                _loading.postValue(false)
                _authState.postValue(true)
            },
            onFailure = {
                _loading.postValue(false)
                _error.postValue(it.message ?: "Signup failed")
            }
        )
    }

    fun resetAuthState() {
        _authState.value = false
    }

    fun clearError() {
        _error.value = ""
    }

}

