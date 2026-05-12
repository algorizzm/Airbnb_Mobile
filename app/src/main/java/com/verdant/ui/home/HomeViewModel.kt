package com.verdant.ui.home

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.verdant.data.model.User
import com.verdant.data.remote.WeatherRepository
import com.verdant.data.remote.WeatherState
import com.verdant.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val userRepo = UserRepository()
    private val fusedLocation =
        LocationServices.getFusedLocationProviderClient(app)

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _weather = MutableStateFlow<WeatherState?>(null)
    val weather: StateFlow<WeatherState?> = _weather.asStateFlow()

    private val _weatherLoading = MutableStateFlow(false)
    val weatherLoading: StateFlow<Boolean> = _weatherLoading.asStateFlow()

    fun loadUser() {
        userRepo.getCurrentUser { _user.postValue(it) }
    }

    /** Call after location permission is granted. */
    @SuppressLint("MissingPermission")
    fun loadWeather() {
        if (_weather.value != null) return   // already loaded — use cache
        viewModelScope.launch {
            _weatherLoading.value = true
            try {
                val loc: Location? = fusedLocation
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .await()

                val lat = loc?.latitude  ?: 10.3157   // fallback: Cebu City
                val lon = loc?.longitude ?: 123.8854

                WeatherRepository.fetch(lat, lon)
                    .onSuccess { _weather.value = it }
                    .onFailure { /* silently ignore — CTA shows static text */ }
            } catch (_: Exception) {
                // Location unavailable — fetch with fallback coords
                WeatherRepository.fetch(10.3157, 123.8854)
                    .onSuccess { _weather.value = it }
            } finally {
                _weatherLoading.value = false
            }
        }
    }
}