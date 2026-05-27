package com.airbnb.data.model

data class HikePost(
    val username: String,
    val date: String,
    val time: String,
    val location: String,
    val title: String,
    val distance: String,
    val elevation: String,
    val duration: String,
    val images: List<Int> // drawable resource IDs for now
)