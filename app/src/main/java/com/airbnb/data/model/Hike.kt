package com.airbnb.data.model

import com.google.firebase.Timestamp
import com.airbnb.utils.HikeStatus

data class Hike(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    /** Legacy field; mirrors primary meetup for older Firestore documents. */
    val location: String = "",
    val meetupPoint: String = "",
    val destination: String = "",
    val difficulty: String = "",
    val distanceKm: Double = 0.0,
    val estimatedDistanceKm: Double = 0.0,
    val elevationM: Double = 0.0,
    val price: Double = 0.0,
    val durationHours: Double = 0.0,
    val guideId: String = "",
    val guideName: String = "",
    val maxParticipants: Int = 0,
    val status: String = HikeStatus.OPEN,
    /** Cover image URL (legacy key in Firestore: [imageUrl]). */
    val imageUrl: String = "",
    val galleryImageUrls: List<String> = emptyList(),
    val startDateTime: Timestamp? = null,
    val endDateTime: Timestamp? = null,
    val inclusions: List<String> = emptyList(),
    val requirements: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val paymentMethods: List<String> = emptyList(),
    val pricingNotes: String = "",
    val createdAt: Timestamp? = null
) {
    /** Draft hikes are not discoverable in Explore; driven by [status]. */
    val isDraft: Boolean
        get() = status.equals(HikeStatus.DRAFT, ignoreCase = true)

    fun summaryLocation(): String = when {
        meetupPoint.isNotBlank() && destination.isNotBlank() ->
            "$meetupPoint → $destination"
        meetupPoint.isNotBlank() -> meetupPoint
        destination.isNotBlank() -> destination
        location.isNotBlank() -> location
        else -> ""
    }

    fun effectiveDistanceKm(): Double = when {
        estimatedDistanceKm > 0 -> estimatedDistanceKm
        distanceKm > 0 -> distanceKm
        else -> 0.0
    }

    fun coverImageUrl(): String = imageUrl.trim()
}
