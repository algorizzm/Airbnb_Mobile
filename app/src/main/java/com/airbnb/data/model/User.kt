package com.airbnb.data.model

data class User(
    val id: String = "",
    val fname: String = "",
    val lname: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "guest", // guest, host, admin
    val location: String = "Cebu, Philippines",
    val profileImage: String = "",
    val bannerImage: String = "",
    val bio: String? = null,
    val totalTrips: Int = 0,
    val totalListings: Int = 0,
    val hostModeEnabled: Boolean = false,
    val badges: List<String> = emptyList(),
    val userCode: String? = null
)