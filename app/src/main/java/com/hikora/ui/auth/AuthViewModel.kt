package com.hikora.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hikora.data.repository.AuthRepository

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _authState = MutableLiveData<Boolean>()
    val authState: LiveData<Boolean> = _authState

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun login(email: String, password: String) {
        repository.login(
            email,
            password,
            onSuccess = {
                _authState.value = true
            },
            onFailure = { exception ->
                _error.value = exception.message ?: "Login failed"
            }
        )
    }

    fun signup(name: String, email: String, password: String, role: String) {
        repository.signup(
            name,
            email,
            password,
            role, // Pass role as a String
            onSuccess = {
                _authState.value = true
            },
            onFailure = { exception ->
                _error.value = exception.message ?: "Signup failed"
            }
        )
    }
}