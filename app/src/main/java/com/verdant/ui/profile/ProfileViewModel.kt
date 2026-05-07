package com.verdant.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdant.data.model.User
import com.verdant.data.repository.UserRepository
import com.verdant.data.session.UserSessionManager
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> = _updateStatus

    private val _logoutStatus = MutableLiveData<Boolean>()
    val logoutStatus: LiveData<Boolean> = _logoutStatus

    fun loadUser() {
        repository.getCurrentUser { user ->
            _user.postValue(user)
        }
    }

    fun updateName(newName: String) {
        repository.updateUserName(newName) { success ->
            _updateStatus.postValue(success)

            if (success) {
                UserSessionManager.refresh()
                loadUser()
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                repository.clearUserSession() // Implement this in UserRepository
                _logoutStatus.postValue(true)
                _user.postValue(null)
            } catch (e: Exception) {
                _logoutStatus.postValue(false)
            }
        }
    }
}