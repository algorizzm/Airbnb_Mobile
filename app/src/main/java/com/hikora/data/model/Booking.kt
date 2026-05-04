package com.hikora.data.model

import com.google.firebase.Timestamp

data class Booking(
    val id: String = "",
    val hikeId: String = "",
    val userId: String = "",
    val userName: String = "",
    val status: String = "pending",
    val createdAt: Timestamp? = null
)
