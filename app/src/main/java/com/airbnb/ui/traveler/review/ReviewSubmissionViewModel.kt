package com.airbnb.ui.traveler.review

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airbnb.data.model.Reservation
import com.airbnb.data.model.Review
import com.airbnb.data.repository.ReservationRepository
import com.airbnb.data.repository.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for review submission screen.
 * 
 * Responsibilities:
 * - Load reservation details
 * - Validate review eligibility
 * - Submit review to repository
 * - Manage UI state (loading, success, error)
 */
class ReviewSubmissionViewModel : ViewModel() {
    
    private val reviewRepository = ReviewRepository()
    private val reservationRepository = ReservationRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _uiState = MutableStateFlow<ReviewSubmissionUiState>(ReviewSubmissionUiState.Loading)
    val uiState: StateFlow<ReviewSubmissionUiState> = _uiState.asStateFlow()
    
    private val _reservation = MutableStateFlow<Reservation?>(null)
    val reservation: StateFlow<Reservation?> = _reservation.asStateFlow()
    
    companion object {
        private const val TAG = "ReviewSubmissionVM"
    }
    
    /**
     * Loads reservation details and validates review eligibility.
     * 
     * @param reservationId Reservation ID
     */
    fun loadReservation(reservationId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = ReviewSubmissionUiState.Loading
                
                // Fetch reservation
                val result = reservationRepository.getReservation(reservationId)
                
                if (result.isFailure || result.getOrNull() == null) {
                    _uiState.value = ReviewSubmissionUiState.Error("Reservation not found")
                    return@launch
                }
                
                val reservation = result.getOrNull()!!
                _reservation.value = reservation
                
                // Validate eligibility
                if (!reservation.canSubmitReview()) {
                    _uiState.value = ReviewSubmissionUiState.Error(
                        "You can only review completed stays that you've checked out from"
                    )
                    return@launch
                }
                
                _uiState.value = ReviewSubmissionUiState.Ready
            } catch (e: Exception) {
                Log.e(TAG, "Error loading reservation", e)
                _uiState.value = ReviewSubmissionUiState.Error("Failed to load reservation: ${e.message}")
            }
        }
    }
    
    /**
     * Submits a review.
     * 
     * @param overallRating Overall rating (1-5)
     * @param cleanlinessRating Cleanliness rating (1-5)
     * @param communicationRating Communication rating (1-5)
     * @param checkInRating Check-in rating (1-5)
     * @param accuracyRating Accuracy rating (1-5)
     * @param locationRating Location rating (1-5)
     * @param valueRating Value rating (1-5)
     * @param comment Optional comment
     */
    fun submitReview(
        overallRating: Int,
        cleanlinessRating: Int,
        communicationRating: Int,
        checkInRating: Int,
        accuracyRating: Int,
        locationRating: Int,
        valueRating: Int,
        comment: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = ReviewSubmissionUiState.Submitting
                
                val reservation = _reservation.value
                if (reservation == null) {
                    _uiState.value = ReviewSubmissionUiState.Error("Reservation not found")
                    return@launch
                }
                
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    _uiState.value = ReviewSubmissionUiState.Error("User not authenticated")
                    return@launch
                }
                
                // Validate ratings
                if (!validateRatings(
                        overallRating,
                        cleanlinessRating,
                        communicationRating,
                        checkInRating,
                        accuracyRating,
                        locationRating,
                        valueRating
                    )
                ) {
                    _uiState.value = ReviewSubmissionUiState.Error("All ratings must be between 1 and 5")
                    return@launch
                }
                
                // Create review
                val review = Review(
                    reservationId = reservation.id,
                    listingId = reservation.listingId,
                    hostId = reservation.hostId,
                    reviewerId = currentUser.uid,
                    reviewerName = currentUser.displayName ?: "Anonymous",
                    reviewerAvatarUrl = currentUser.photoUrl?.toString(),
                    rating = overallRating,
                    cleanlinessRating = cleanlinessRating,
                    communicationRating = communicationRating,
                    checkInRating = checkInRating,
                    accuracyRating = accuracyRating,
                    locationRating = locationRating,
                    valueRating = valueRating,
                    comment = comment.trim()
                )
                
                // Submit review
                val result = reviewRepository.submitReview(review)
                
                if (result.isSuccess) {
                    Log.d(TAG, "Review submitted successfully: ${result.getOrNull()}")
                    _uiState.value = ReviewSubmissionUiState.Success
                } else {
                    Log.e(TAG, "Failed to submit review", result.exceptionOrNull())
                    _uiState.value = ReviewSubmissionUiState.Error(
                        "Failed to submit review: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting review", e)
                _uiState.value = ReviewSubmissionUiState.Error("Failed to submit review: ${e.message}")
            }
        }
    }
    
    /**
     * Validates all ratings are within valid range (1-5).
     */
    private fun validateRatings(vararg ratings: Int): Boolean {
        return ratings.all { it in 1..5 }
    }
}

/**
 * UI state for review submission screen.
 */
sealed class ReviewSubmissionUiState {
    object Loading : ReviewSubmissionUiState()
    object Ready : ReviewSubmissionUiState()
    object Submitting : ReviewSubmissionUiState()
    object Success : ReviewSubmissionUiState()
    data class Error(val message: String) : ReviewSubmissionUiState()
}
