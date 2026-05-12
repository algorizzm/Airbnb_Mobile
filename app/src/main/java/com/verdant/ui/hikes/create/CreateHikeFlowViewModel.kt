package com.verdant.ui.hikes.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.verdant.core.auth.AuthState
import com.verdant.data.model.Hike
import com.verdant.data.remote.StorageService
import com.verdant.data.repository.HikeRepository
import com.verdant.data.session.UserSessionManager
import com.verdant.utils.HikeDifficulty
import com.verdant.utils.HikeStatus
import com.verdant.utils.Permissions
import com.verdant.utils.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.max

const val CREATE_HIKE_MAX_GALLERY = 3

enum class CreateHikeStep(val index: Int) {
    DETAILS(0),
    SCHEDULE(1),
    ROUTE(2),
    MEDIA(3),
    PRICING(4),
    REVIEW(5);

    companion object {
        fun fromIndex(i: Int): CreateHikeStep =
            entries.firstOrNull { it.index == i } ?: DETAILS
    }
}

data class CreateHikeUiState(
    val stepIndex: Int = 0,
    val title: String = "",
    val description: String = "",
    val maxSlotsText: String = "",
    val inclusionsText: String = "",
    val requirementsText: String = "",
    val tagsText: String = "",
    val startMillis: Long? = null,
    val endMillis: Long? = null,
    val meetupPoint: String = "",
    val destination: String = "",
    val routeDifficulty: String = HikeDifficulty.BEGINNER,
    val elevationMText: String = "",
    val estimatedDistanceKmText: String = "",
    val coverImageUrl: String = "",
    val galleryImageUrls: List<String> = List(CREATE_HIKE_MAX_GALLERY) { "" },
    val priceText: String = "",
    val paymentMethodsText: String = "",
    val pricingNotes: String = "",
    val isEditMode: Boolean = false,
    val loading: Boolean = false,
    val message: String? = null,
    val finished: Boolean = false
) {
    val step: CreateHikeStep get() = CreateHikeStep.fromIndex(stepIndex)
}

class CreateHikeFlowViewModel(
    private val existingHikeId: String?,
    private val hikeRepository: HikeRepository = HikeRepository(),
    private val storageService: StorageService = StorageService()
) : ViewModel() {

    private val _ui = MutableStateFlow(CreateHikeUiState(isEditMode = !existingHikeId.isNullOrBlank()))
    val ui: StateFlow<CreateHikeUiState> = _ui.asStateFlow()

    init {
        if (!existingHikeId.isNullOrBlank()) {
            viewModelScope.launch {
                _ui.value = _ui.value.copy(loading = true)
                hikeRepository.getHike(existingHikeId).onSuccess { hike ->
                    val state = UserSessionManager.authState.value
                    val currentUser = (state as? AuthState.Authenticated)?.user
                    if (state is AuthState.Loading || (state is AuthState.Authenticated && currentUser == null)) {
                        _ui.value = _ui.value.copy(loading = false, message = "Loading your account…")
                        UserSessionManager.refresh()
                        return@onSuccess
                    }
                    if (!Permissions.canManageHike(currentUser?.role, hike.guideId, currentUser?.id)) {
                        _ui.value = _ui.value.copy(loading = false, message = "You cannot edit this hike.")
                        return@onSuccess
                    }
                    _ui.value = hike.toUiState(isEditMode = true, loading = false)
                }.onFailure {
                    _ui.value = _ui.value.copy(loading = false, message = it.message)
                }
            }
        }
    }

    fun goToStep(index: Int) {
        val clamped = index.coerceIn(0, CreateHikeStep.entries.last().index)
        _ui.value = _ui.value.copy(stepIndex = clamped)
    }

    fun goNext(): Boolean {
        val s = _ui.value
        val msg = validateStep(s.step, s, forPublish = false)
        if (msg != null) {
            _ui.value = s.copy(message = msg)
            return false
        }
        if (s.stepIndex < CreateHikeStep.REVIEW.index) {
            _ui.value = s.copy(stepIndex = s.stepIndex + 1, message = null)
        }
        return true
    }

    fun goBack() {
        val s = _ui.value
        if (s.stepIndex > 0) {
            _ui.value = s.copy(stepIndex = s.stepIndex - 1, message = null)
        }
    }

    fun updateTitle(v: String) {
        _ui.value = _ui.value.copy(title = v)
    }

    fun updateDescription(v: String) {
        _ui.value = _ui.value.copy(description = v)
    }

    fun updateMaxSlotsText(v: String) {
        _ui.value = _ui.value.copy(maxSlotsText = v)
    }

    fun updateInclusionsText(v: String) {
        _ui.value = _ui.value.copy(inclusionsText = v)
    }

    fun updateRequirementsText(v: String) {
        _ui.value = _ui.value.copy(requirementsText = v)
    }

    fun updateTagsText(v: String) {
        _ui.value = _ui.value.copy(tagsText = v)
    }

    fun setStartMillis(millis: Long?) {
        _ui.value = _ui.value.copy(startMillis = millis)
    }

    fun setEndMillis(millis: Long?) {
        _ui.value = _ui.value.copy(endMillis = millis)
    }

    fun updateMeetupPoint(v: String) {
        _ui.value = _ui.value.copy(meetupPoint = v)
    }

    fun updateDestination(v: String) {
        _ui.value = _ui.value.copy(destination = v)
    }

    fun updateRouteDifficulty(v: String) {
        _ui.value = _ui.value.copy(routeDifficulty = v)
    }

    fun updateElevationMText(v: String) {
        _ui.value = _ui.value.copy(elevationMText = v)
    }

    fun updateEstimatedDistanceKmText(v: String) {
        _ui.value = _ui.value.copy(estimatedDistanceKmText = v)
    }

    fun updateCoverImageUrl(url: String) {
        _ui.value = _ui.value.copy(coverImageUrl = url)
    }

    fun updateGalleryUrl(index: Int, url: String) {
        val cur = _ui.value.galleryImageUrls
        if (index !in cur.indices) return
        val next = cur.toMutableList().also { it[index] = url }
        _ui.value = _ui.value.copy(galleryImageUrls = next)
    }

    fun updatePriceText(v: String) {
        _ui.value = _ui.value.copy(priceText = v)
    }

    fun updatePaymentMethodsText(v: String) {
        _ui.value = _ui.value.copy(paymentMethodsText = v)
    }

    fun updatePricingNotes(v: String) {
        _ui.value = _ui.value.copy(pricingNotes = v)
    }

    fun uploadCover(uri: Uri) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            val key = existingHikeId ?: "temp_${System.currentTimeMillis()}"
            storageService.uploadHikeImage(key, uri)
                .onSuccess { url -> _ui.value = _ui.value.copy(loading = false, coverImageUrl = url) }
                .onFailure {
                    _ui.value = _ui.value.copy(
                        loading = false,
                        message = "Image upload failed: ${it.message}"
                    )
                }
        }
    }

    fun uploadGallerySlot(index: Int, uri: Uri) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            val key = "${existingHikeId ?: "temp"}_${index}_${System.currentTimeMillis()}"
            storageService.uploadHikeGalleryImage(key, uri)
                .onSuccess { url ->
                    updateGalleryUrl(index, url)
                    _ui.value = _ui.value.copy(loading = false)
                }
                .onFailure {
                    _ui.value = _ui.value.copy(
                        loading = false,
                        message = "Gallery upload failed: ${it.message}"
                    )
                }
        }
    }

    fun saveDraft() {
        save(asDraft = true)
    }

    fun publish() {
        val s = _ui.value
        val err = validateAllForPublish(s)
        if (err != null) {
            _ui.value = s.copy(message = err)
            return
        }
        save(asDraft = false)
    }

    private fun save(asDraft: Boolean) {
        viewModelScope.launch {
            val user = when (val state = UserSessionManager.authState.value) {
                AuthState.Guest -> {
                    _ui.value = _ui.value.copy(message = "Please log in to continue.")
                    return@launch
                }
                is AuthState.Loading -> {
                    _ui.value = _ui.value.copy(message = "Loading your account…")
                    return@launch
                }
                is AuthState.Authenticated -> {
                    state.user ?: run {
                        _ui.value = _ui.value.copy(message = "Finishing account setup…")
                        UserSessionManager.refresh()
                        return@launch
                    }
                }
            }
            if (user.role != UserRole.GUIDE) {
                _ui.value = _ui.value.copy(message = "Only guides can publish hikes.")
                return@launch
            }

            val s = _ui.value
            if (!asDraft) {
                val err = validateAllForPublish(s)
                if (err != null) {
                    _ui.value = s.copy(message = err)
                    return@launch
                }
            } else {
                if (s.title.isBlank()) {
                    _ui.value = s.copy(message = "Add a title before saving a draft.")
                    return@launch
                }
            }

            val hikeToPersist = if (existingHikeId.isNullOrBlank()) {
                s.toHike(user.id, user.name.ifBlank { user.email }, asDraft)
            } else {
                val existing = hikeRepository.getHike(existingHikeId).getOrNull()
                if (existing == null) {
                    _ui.value = s.copy(loading = false, message = "Hike not found.")
                    return@launch
                }
                if (!Permissions.canManageHike(user.role, existing.guideId, user.id)) {
                    _ui.value = s.copy(loading = false, message = "Not allowed.")
                    return@launch
                }
                s.toHike(user.id, user.name.ifBlank { user.email }, asDraft).copy(
                    id = existingHikeId,
                    guideId = existing.guideId,
                    createdAt = existing.createdAt ?: Timestamp.now()
                )
            }
            _ui.value = s.copy(loading = true, message = null)

            val result = if (existingHikeId.isNullOrBlank()) {
                hikeRepository.createHike(hikeToPersist)
            } else {
                hikeRepository.updateHike(hikeToPersist)
            }

            result.onSuccess {
                _ui.value = _ui.value.copy(loading = false, finished = true, message = null)
            }.onFailure {
                _ui.value = _ui.value.copy(loading = false, message = it.message)
            }
        }
    }

    fun deleteHike() {
        val id = existingHikeId ?: return
        viewModelScope.launch {
            val user = when (val state = UserSessionManager.authState.value) {
                AuthState.Guest -> {
                    _ui.value = _ui.value.copy(message = "Please log in to continue.")
                    return@launch
                }
                is AuthState.Loading -> {
                    _ui.value = _ui.value.copy(message = "Loading your account…")
                    return@launch
                }
                is AuthState.Authenticated -> {
                    state.user ?: run {
                        _ui.value = _ui.value.copy(message = "Finishing account setup…")
                        UserSessionManager.refresh()
                        return@launch
                    }
                }
            }
            val hike = hikeRepository.getHike(id).getOrNull() ?: return@launch
            if (!Permissions.canManageHike(user.role, hike.guideId, user.id)) {
                _ui.value = _ui.value.copy(message = "Not allowed.")
                return@launch
            }
            _ui.value = _ui.value.copy(loading = true, message = null)
            hikeRepository.deleteHike(id).onSuccess {
                _ui.value = _ui.value.copy(loading = false, finished = true)
            }.onFailure {
                _ui.value = _ui.value.copy(loading = false, message = it.message)
            }
        }
    }

    fun consumeMessage() {
        _ui.value = _ui.value.copy(message = null)
    }

    private fun validateStep(step: CreateHikeStep, s: CreateHikeUiState, forPublish: Boolean): String? {
        if (!forPublish && step != CreateHikeStep.REVIEW) {
            return when (step) {
                CreateHikeStep.DETAILS -> when {
                    s.title.isBlank() -> "Title is required."
                    s.maxSlotsText.toIntOrNull() == null || s.maxSlotsText.toIntOrNull()!! <= 0 ->
                        "Enter a valid max slots number."
                    else -> null
                }
                CreateHikeStep.SCHEDULE -> when {
                    s.startMillis == null || s.endMillis == null -> "Pick start and end date/time."
                    s.endMillis <= s.startMillis -> "End must be after start."
                    else -> null
                }
                CreateHikeStep.ROUTE -> when {
                    s.meetupPoint.isBlank() -> "Meetup point is required."
                    s.destination.isBlank() -> "Destination is required."
                    s.routeDifficulty.isBlank() -> "Select difficulty."
                    s.estimatedDistanceKmText.toDoubleOrNull() == null ||
                        s.estimatedDistanceKmText.toDoubleOrNull()!! <= 0 -> "Enter estimated distance (km)."
                    else -> null
                }
                CreateHikeStep.MEDIA -> null
                CreateHikeStep.PRICING -> when {
                    s.priceText.toDoubleOrNull() == null -> "Enter a valid price (0 allowed)."
                    else -> null
                }
                CreateHikeStep.REVIEW -> null
            }
        }
        return null
    }

    private fun validateAllForPublish(s: CreateHikeUiState): String? {
        validateStep(CreateHikeStep.DETAILS, s, forPublish = false)?.let { return it }
        validateStep(CreateHikeStep.SCHEDULE, s, forPublish = false)?.let { return it }
        validateStep(CreateHikeStep.ROUTE, s, forPublish = false)?.let { return it }
        validateStep(CreateHikeStep.PRICING, s, forPublish = false)?.let { return it }
        return null
    }

    private fun CreateHikeUiState.toHike(guideId: String, guideName: String, asDraft: Boolean): Hike {
        val maxP = max(1, maxSlotsText.toIntOrNull() ?: 1)
        val dist = estimatedDistanceKmText.toDoubleOrNull() ?: 0.0
        val price = priceText.toDoubleOrNull() ?: 0.0
        val elev = elevationMText.toDoubleOrNull() ?: 0.0
        val startTs = startMillis?.let { Timestamp(Date(it)) }
        val endTs = endMillis?.let { Timestamp(Date(it)) }
        val duration = if (startMillis != null && endMillis != null && endMillis > startMillis) {
            (endMillis - startMillis).toDouble() / TimeUnit.HOURS.toMillis(1)
        } else 0.0
        val gallery = galleryImageUrls.map { it.trim() }.filter { it.isNotBlank() }
        val status = when {
            asDraft -> HikeStatus.DRAFT
            else -> HikeStatus.OPEN
        }
        return Hike(
            id = "",
            title = title.trim(),
            description = description.trim(),
            location = meetupPoint.trim(),
            meetupPoint = meetupPoint.trim(),
            destination = destination.trim(),
            difficulty = routeDifficulty.trim(),
            distanceKm = dist,
            estimatedDistanceKm = dist,
            elevationM = elev,
            price = price,
            durationHours = duration,
            guideId = guideId,
            guideName = guideName,
            maxParticipants = maxP,
            status = status,
            imageUrl = coverImageUrl.trim(),
            galleryImageUrls = gallery,
            startDateTime = startTs,
            endDateTime = endTs,
            inclusions = splitListField(inclusionsText),
            requirements = splitListField(requirementsText),
            tags = splitListField(tagsText),
            paymentMethods = splitListField(paymentMethodsText),
            pricingNotes = pricingNotes.trim(),
            createdAt = Timestamp.now()
        )
    }

    private fun Hike.toUiState(isEditMode: Boolean, loading: Boolean): CreateHikeUiState {
        val gallery = galleryImageUrls.toMutableList()
        while (gallery.size < CREATE_HIKE_MAX_GALLERY) gallery.add("")
        val trimmed = gallery.take(CREATE_HIKE_MAX_GALLERY)
        val startMillis = startDateTime?.toDate()?.time
        val endMillis = endDateTime?.toDate()?.time
        val meet = meetupPoint.ifBlank { location }
        return CreateHikeUiState(
            stepIndex = 0,
            title = title,
            description = description,
            maxSlotsText = maxParticipants.toString(),
            inclusionsText = inclusions.joinToString("\n"),
            requirementsText = requirements.joinToString("\n"),
            tagsText = tags.joinToString("\n"),
            startMillis = startMillis,
            endMillis = endMillis,
            meetupPoint = meet,
            destination = destination,
            routeDifficulty = difficulty.normalizeStoredDifficulty(),
            elevationMText = if (elevationM > 0) elevationM.toString() else "",
            estimatedDistanceKmText = when {
                estimatedDistanceKm > 0 -> estimatedDistanceKm.toString()
                distanceKm > 0 -> distanceKm.toString()
                else -> ""
            },
            coverImageUrl = imageUrl,
            galleryImageUrls = trimmed,
            priceText = price.toString(),
            paymentMethodsText = paymentMethods.joinToString("\n"),
            pricingNotes = pricingNotes,
            isEditMode = isEditMode,
            loading = loading,
            message = null,
            finished = false
        )
    }

    private fun String.normalizeStoredDifficulty(): String {
        val u = uppercase()
        if (u in HikeDifficulty.ALL) return u
        return when (lowercase()) {
            "easy" -> HikeDifficulty.BEGINNER
            "moderate" -> HikeDifficulty.INTERMEDIATE
            "hard", "expert" -> HikeDifficulty.ADVANCED
            else -> HikeDifficulty.INTERMEDIATE
        }
    }

    private fun splitListField(raw: String): List<String> =
        raw.split("\n", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    class Factory(
        private val hikeId: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CreateHikeFlowViewModel::class.java)) {
                return CreateHikeFlowViewModel(hikeId) as T
            }
            error("Unknown ViewModel class")
        }
    }
}
