package com.verdant.data.model

import com.google.firebase.Timestamp

data class Booking(
    val id: String = "",
    val hikeId: String = "",
    val userId: String = "",
    val userName: String = "",
    val guideId: String = "",
    val status: String = "pending",
    val paymentStatus: String = "unpaid",
    val createdAt: Timestamp? = null
)
