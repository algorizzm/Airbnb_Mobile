package com.airbnb.ui.traveler.wishlist.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.core.ui.ImageLoader
import com.airbnb.data.model.Listing
import com.airbnb.databinding.ItemWishlistGridBinding

class WishlistAdapter(
    private val onItemClick: (Listing) -> Unit,
    private val onRemoveClick: (Listing) -> Unit
) : ListAdapter<Listing, WishlistAdapter.WishlistViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): WishlistViewHolder {

        val binding = ItemWishlistGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return WishlistViewHolder(
            binding,
            onItemClick,
            onRemoveClick
        )
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WishlistViewHolder(
        private val binding: ItemWishlistGridBinding,
        private val onItemClick: (Listing) -> Unit,
        private val onRemoveClick: (Listing) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listing: Listing) {

            // Wishlist title
            binding.tvTitle.text = listing.title

            // Subtitle
            binding.tvSubtitle.text = "1 saved"

            // Click listener
            binding.root.setOnClickListener {
                onItemClick(listing)
            }

            // Load listing cover image
            ImageLoader.loadListingCoverImage(
                imageView = binding.listingImage,
                listing = listing
            )
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Listing>() {

        override fun areItemsTheSame(
            oldItem: Listing,
            newItem: Listing
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: Listing,
            newItem: Listing
        ): Boolean {
            return oldItem == newItem
        }
    }
}