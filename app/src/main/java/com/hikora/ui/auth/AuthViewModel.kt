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
        try {
            repository.login(
                email,
                password,
                onSuccess = {
                    _authState.postValue(true)
                },
                onFailure = { exception ->
                    _error.postValue(exception.message ?: "Login failed")
                }
            )
        } catch (e: Exception) {
            _error.postValue(e.message ?: "Login crashed")
        }
    }

    fun signup(name: String, email: String, password: String, role: String) {
        repository.signup(
            name,
            email,
            password,
            role,
            onSuccess = {
                _authState.postValue(true)
            },
            onFailure = {
                _error.postValue(it.message ?: "Signup failed")
            }
        )
    }
    fun resetAuthState() {
        _authState.value = false
    }

}

