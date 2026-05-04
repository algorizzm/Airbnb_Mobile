package com.hikora.ui.hikes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.hikora.data.model.Hike
import com.hikora.data.repository.HikeRepository
import com.hikora.data.session.UserSessionManager
import com.hikora.utils.HikeStatus
import com.hikora.utils.HikingRbac
import com.hikora.utils.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateEditHikeFormState(
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val difficulty: String = "",
    val distanceKm: String = "",
    val price: String = "",
    val maxParticipants: String = "",
    val status: String = HikeStatus.OPEN,
    val isEditMode: Boolean = false,
    val loading: Boolean = false,
    val message: String? = null,
    val finished: Boolean = false
)

class CreateEditHikeViewModel(
    private val existingHikeId: String?,
    private val hikeRepository: HikeRepository = HikeRepository()
) : ViewModel() {

    private val _form = MutableStateFlow(CreateEditHikeFormState(isEditMode = !existingHikeId.isNullOrBlank()))
    val form: StateFlow<CreateEditHikeFormState> = _form.asStateFlow()

    init {
        if (!existingHikeId.isNullOrBlank()) {
            viewModelScope.launch {
                _form.value = _form.value.copy(loading = true)
                hikeRepository.getHike(existingHikeId).onSuccess { hike ->
                    if (!HikingRbac.canManageHike(
                            UserSessionManager.currentUser.value?.role ?: UserRole.CLIENT,
                            hike.guideId,
                            UserSessionManager.currentUser.value?.id
                        )
                    ) {
                        _form.value = _form.value.copy(
                            loading = false,
                            message = "You cannot edit this hike."
                        )
                        return@onSuccess
                    }
                    _form.value = CreateEditHikeFormState(
                        title = hike.title,
                        description = hike.description,
                        location = hike.location,
                        difficulty = hike.difficulty,
                        distanceKm = hike.distanceKm.toString(),
                        price = hike.price.toString(),
                        maxParticipants = hike.maxParticipants.toString(),
                        status = hike.status,
                        isEditMode = true,
                        loading = false
                    )
                }.onFailure {
                    _form.value = _form.value.copy(loading = false, message = it.message)
                }
            }
        }
    }

    fun updateTitle(v: String) {
        _form.value = _form.value.copy(title = v)
    }

    fun updateDescription(v: String) {
        _form.value = _form.value.copy(description = v)
    }

    fun updateLocation(v: String) {
        _form.value = _form.value.copy(location = v)
    }

    fun updateDifficulty(v: String) {
        _form.value = _form.value.copy(difficulty = v)
    }

    fun updateDistanceKm(v: String) {
        _form.value = _form.value.copy(distanceKm = v)
    }

    fun updatePrice(v: String) {
        _form.value = _form.value.copy(price = v)
    }

    fun updateMaxParticipants(v: String) {
        _form.value = _form.value.copy(maxParticipants = v)
    }

    fun updateStatus(v: String) {
        _form.value = _form.value.copy(status = v)
    }

    fun save() {
        viewModelScope.launch {
            val user = UserSessionManager.currentUser.value
            if (user == null || user.role != UserRole.GUIDE) {
                _form.value = _form.value.copy(message = "Only guides can publish hikes.")
                return@launch
            }
            val f = _form.value
            val distance = f.distanceKm.toDoubleOrNull()
            val price = f.price.toDoubleOrNull()
            val maxP = f.maxParticipants.toIntOrNull()
            if (f.title.isBlank() || f.location.isBlank() || distance == null || price == null || maxP == null || maxP <= 0) {
                _form.value = f.copy(message = "Please fill all fields with valid numbers.")
                return@launch
            }

            val hike = Hike(
                id = existingHikeId.orEmpty(),
                title = f.title.trim(),
                description = f.description.trim(),
                location = f.location.trim(),
                difficulty = f.difficulty.trim().ifBlank { "Moderate" },
                distanceKm = distance,
                price = price,
                guideId = user.id,
                guideName = user.name.ifBlank { user.email },
                maxParticipants = maxP,
                status = f.status.ifBlank { HikeStatus.OPEN },
                createdAt = Timestamp.now()
            )

            _form.value = f.copy(loading = true, message = null)

            val result = if (existingHikeId.isNullOrBlank()) {
                hikeRepository.createHike(hike)
            } else {
                if (!HikingRbac.canManageHike(user.role, hike.guideId, user.id)) {
                    _form.value = _form.value.copy(loading = false, message = "Not allowed.")
                    return@launch
                }
                hikeRepository.updateHike(hike.copy(id = existingHikeId))
            }

            result.onSuccess {
                _form.value = _form.value.copy(loading = false, finished = true, message = null)
            }.onFailure {
                _form.value = _form.value.copy(loading = false, message = it.message)
            }
        }
    }

    fun deleteHike() {
        val id = existingHikeId ?: return
        viewModelScope.launch {
            val user = UserSessionManager.currentUser.value ?: return@launch
            val hike = hikeRepository.getHike(id).getOrNull() ?: return@launch
            if (!HikingRbac.canManageHike(user.role, hike.guideId, user.id)) {
                _form.value = _form.value.copy(message = "Not allowed.")
                return@launch
            }
            _form.value = _form.value.copy(loading = true, message = null)
            hikeRepository.deleteHike(id).onSuccess {
                _form.value = _form.value.copy(loading = false, finished = true)
            }.onFailure {
                _form.value = _form.value.copy(loading = false, message = it.message)
            }
        }
    }

    fun consumeMessage() {
        _form.value = _form.value.copy(message = null)
    }

    class Factory(
        private val hikeId: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CreateEditHikeViewModel::class.java)) {
                return CreateEditHikeViewModel(hikeId) as T
            }
            error("Unknown ViewModel class")
        }
    }
}
