package com.airbnb.core.ui

import android.widget.ImageView
import com.airbnb.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

/**
 * Centralized image loading utility for consistent Glide usage across the app.
 * 
 * Provides standardized image loading with:
 * - Placeholder handling
 * - Error fallback
 * - Null/empty URL safety
 * - Consistent transformations
 * - Disk caching strategy
 */
object ImageLoader {

    /**
     * Loads a listing image into an ImageView with standard configuration.
     * 
     * Features:
     * - Gracefully handles null/empty URLs
     * - Shows placeholder during loading
     * - Shows error drawable on failure
     * - Uses centerCrop for consistent aspect ratio
     * - Enables disk caching for performance
     * 
     * @param imageView Target ImageView
     * @param imageUrl Image URL (can be null or empty)
     * @param placeholder Optional custom placeholder (defaults to img_hike_placeholder)
     * @param errorDrawable Optional custom error drawable (defaults to img_hike_placeholder)
     */
    fun loadListingImage(
        imageView: ImageView,
        imageUrl: String?,
        placeholder: Int = R.drawable.img_hike_placeholder,
        errorDrawable: Int = R.drawable.img_hike_placeholder
    ) {
        // Handle null or empty URLs
        if (imageUrl.isNullOrBlank()) {
            imageView.setImageResource(placeholder)
            return
        }

        Glide.with(imageView.context)
            .load(imageUrl.trim())
            .centerCrop()
            .placeholder(placeholder)
            .error(errorDrawable)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)
    }

    /**
     * Loads a listing image with custom transformation (no centerCrop).
     * Useful for detail screens where full image should be visible.
     * 
     * @param imageView Target ImageView
     * @param imageUrl Image URL (can be null or empty)
     * @param placeholder Optional custom placeholder
     * @param errorDrawable Optional custom error drawable
     */
    fun loadListingImageFitCenter(
        imageView: ImageView,
        imageUrl: String?,
        placeholder: Int = R.drawable.img_hike_placeholder,
        errorDrawable: Int = R.drawable.img_hike_placeholder
    ) {
        if (imageUrl.isNullOrBlank()) {
            imageView.setImageResource(placeholder)
            return
        }

        Glide.with(imageView.context)
            .load(imageUrl.trim())
            .fitCenter()
            .placeholder(placeholder)
            .error(errorDrawable)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imageView)
    }

    /**
     * Loads the first image from a list of image URLs.
     * Useful for listings with multiple images where only the cover is needed.
     * 
     * @param imageView Target ImageView
     * @param imageUrls List of image URLs
     * @param placeholder Optional custom placeholder
     * @param errorDrawable Optional custom error drawable
     */
    fun loadFirstListingImage(
        imageView: ImageView,
        imageUrls: List<String>?,
        placeholder: Int = R.drawable.img_hike_placeholder,
        errorDrawable: Int = R.drawable.img_hike_placeholder
    ) {
        val firstUrl = imageUrls?.firstOrNull { it.isNotBlank() }
        loadListingImage(imageView, firstUrl, placeholder, errorDrawable)
    }

    /**
     * Loads a listing cover image from a Listing model.
     * Uses the listing's coverImageUrl() method for consistency.
     * 
     * @param imageView Target ImageView
     * @param listing Listing object
     * @param placeholder Optional custom placeholder
     * @param errorDrawable Optional custom error drawable
     */
    fun loadListingCoverImage(
        imageView: ImageView,
        listing: com.airbnb.data.model.Listing,
        placeholder: Int = R.drawable.img_hike_placeholder,
        errorDrawable: Int = R.drawable.img_hike_placeholder
    ) {
        loadListingImage(imageView, listing.coverImageUrl(), placeholder, errorDrawable)
    }
}
