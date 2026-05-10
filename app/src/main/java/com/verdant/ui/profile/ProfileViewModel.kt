package com.verdant.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.verdant.core.auth.AuthState
import com.verdant.data.repository.UserRepository
import com.verdant.data.session.UserSessionManager
import kotlinx.coroutines.flow.map

class ProfileViewModel : ViewModel() {

    private val repository = UserRepository()

    /**
     * =========================
     * USER STATE
     * =========================
     */

    // Uses cached session user instead of re-fetching every time
    val user = UserSessionManager.currentUser.asLiveData()

    // Observe auth state globally
    val isGuest = UserSessionManager.authState
        .map { state -> state is AuthState.Guest }
        .asLiveData()

    /**
     * =========================
     * UI STATE
     * =========================
     */
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> = _updateStatus

    /**
     * =========================
     * NAVIGATION EVENTS
     * =========================
     */
    private val _navigateToAuth = MutableLiveData<Unit>()
    val navigateToAuth: LiveData<Unit> = _navigateToAuth

    private val _navigateToSettings = MutableLiveData<Unit>()
    val navigateToSettings: LiveData<Unit> = _navigateToSettings

    /**
     * =========================
     * REFRESH PROFILE
     * =========================
     */
    fun refreshProfile() {

        _loading.value = true

        UserSessionManager.refresh()

        _loading.postValue(false)
    }

    /**
     * =========================
     * UPDATE BIO
     * =========================
     */
    fun updateBio(newBio: String) {

        _loading.value = true

        repository.updateUserBio(newBio) { success ->

            _loading.postValue(false)
            _updateStatus.postValue(success)

            if (success) {

                // Refresh cached session user
                UserSessionManager.refresh()

            } else {

                _errorMessage.postValue("Failed to update bio")
            }
        }
    }

    /**
     * =========================
     * CLICK EVENTS
     * =========================
     */
    fun onLoginClicked() {
        _navigateToAuth.value = Unit
    }

    fun onSignupClicked() {
        _navigateToAuth.value = Unit
    }

    fun onSettingsClicked() {
        _navigateToSettings.value = Unit
    }

    /**
     * =========================
     * CLEAR ERROR
     * =========================
     */
    fun clearError() {
        _errorMessage.value = null
    }
}