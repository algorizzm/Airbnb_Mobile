package com.airbnb.ui.listings.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.R
import com.airbnb.core.ui.ImageLoader
import com.airbnb.databinding.ItemListingGalleryImageBinding

/**
 * Adapter for displaying listing gallery images in a horizontal RecyclerView.
 * 
 * Features:
 * - Displays gallery images with consistent sizing
 * - Uses centralized ImageLoader for consistent loading
 * - Handles empty/null URLs gracefully
 * - Optional click listener for full-screen viewing
 */
class ListingGalleryAdapter(
    private val onImageClick: ((String, Int) -> Unit)? = null
) : ListAdapter<String, ListingGalleryAdapter.GalleryViewHolder>(GalleryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val binding = ItemListingGalleryImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GalleryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class GalleryViewHolder(
        private val binding: ItemListingGalleryImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUrl: String, position: Int) {
            ImageLoader.loadListingImage(
                imageView = binding.imgGallery,
                imageUrl = imageUrl,
                placeholder = R.drawable.img_hike_placeholder,
                errorDrawable = R.drawable.img_hike_placeholder
            )

            // Optional click listener for full-screen viewing
            onImageClick?.let { clickListener ->
                binding.root.setOnClickListener {
                    clickListener(imageUrl, position)
                }
            }
        }
    }

    private class GalleryDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}
