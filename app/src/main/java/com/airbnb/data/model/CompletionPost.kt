package com.airbnb.data.model

import com.google.firebase.Timestamp

/**
 * A lightweight post auto-created when a guide completes a hike.
 * Stored in Firestore: collection "posts".
 */
data class CompletionPost(
    val id: String = "",
    val hikeId: String = "",
    val hikeTitle: String = "",
    val guideId: String = "",
    val guideName: String = "",
    val location: String = "",
    val distanceKm: Double = 0.0,
    val elevationM: Double = 0.0,
    val participantCount: Int = 0,
    val coverImageUrl: String = "",
    val createdAt: Timestamp? = null
)
