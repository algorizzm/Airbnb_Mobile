package com.hikora.data.model

import com.google.firebase.Timestamp

data class Hike(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val difficulty: String = "",
    val distanceKm: Double = 0.0,
    val price: Double = 0.0,
    val guideId: String = "",
    val guideName: String = "",
    val maxParticipants: Int = 0,
    val status: String = "open",
    val createdAt: Timestamp? = null
)
