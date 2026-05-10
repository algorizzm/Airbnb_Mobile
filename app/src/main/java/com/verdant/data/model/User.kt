package com.verdant.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "guest", // hiker, guide, admin, guest
    val profileImage: String = "",
    val bannerImage: String = "",
    val bio: String? = null,
    val totalHikes: Int = 0,
    val totalDistance: Double = 0.0,
    val totalSummits: Int = 0,
    val badges: List<String> = emptyList()
)