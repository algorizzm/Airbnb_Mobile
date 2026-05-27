package com.airbnb.ui.explore.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.R
import com.airbnb.data.model.Listing
import com.airbnb.databinding.ItemListingBinding

class ListingAdapter(
    private val onItemClick: (Listing) -> Unit,
    private val onWishlistClick: ((Listing) -> Unit)? = null,
    private val wishlistIds: Set<String> = emptySet()
) : ListAdapter<Listing, ListingAdapter.ListingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val binding = ItemListingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListingViewHolder(binding, onItemClick, onWishlistClick)
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        holder.bind(getItem(position), wishlistIds.contains(getItem(position).id))
    }

    class ListingViewHolder(
        private val binding: ItemListingBinding,
        private val onItemClick: (Listing) -> Unit,
        private val onWishlistClick: ((Listing) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listing: Listing, isInWishlist: Boolean) {
            binding.tvTitle.text = listing.title
            binding.tvLocation.text = listing.location
            binding.tvGuests.text = listing.guestSummary()
            binding.tvPrice.text = listing.formattedPrice()
            
            binding.root.setOnClickListener { onItemClick(listing) }

            // Update wishlist button appearance
            if (isInWishlist) {
                binding.btnWishlist.setImageResource(R.drawable.ic_heart_filled)
            } else {
                binding.btnWishlist.setImageResource(R.drawable.ic_heart)
            }

            // Handle wishlist button click
            binding.btnWishlist.setOnClickListener {
                onWishlistClick?.invoke(listing)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Listing>() {
        override fun areItemsTheSame(oldItem: Listing, newItem: Listing): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Listing, newItem: Listing): Boolean = oldItem == newItem
    }
}
