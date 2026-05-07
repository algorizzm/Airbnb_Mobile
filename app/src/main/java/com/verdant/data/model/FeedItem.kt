package com.verdant.data.model

import com.verdant.data.model.HikePost

sealed class FeedItem {

    data object CTA : FeedItem()

    data class Post(
        val hikePost: HikePost
    ) : FeedItem()
}