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
    // LEGACY PASSWORD AUTHENTICATION
    // =========================================================

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

}

