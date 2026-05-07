package com.verdant.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "client", // client, guide, admin, guest
    val profileImage: String = "",
    val totalHikes: Int = 0,
    val totalDistance: Double = 0.0,
    val badges: List<String> = emptyList()
)