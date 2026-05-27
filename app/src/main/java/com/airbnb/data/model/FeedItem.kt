package com.airbnb.data.model

sealed class FeedItem {

    data object CTA : FeedItem()

    data class Post(
        val hikePost: HikePost
    ) : FeedItem()
}