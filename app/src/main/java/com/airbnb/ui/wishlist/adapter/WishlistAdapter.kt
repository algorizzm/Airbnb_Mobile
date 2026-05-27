package com.airbnb.ui.wishlist.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.data.model.Listing
import com.airbnb.databinding.ItemWishlistBinding

class WishlistAdapter(
    private val onItemClick: (Listing) -> Unit,
    private val onRemoveClick: (Listing) -> Unit
) : ListAdapter<Listing, WishlistAdapter.WishlistViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val binding = ItemWishlistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WishlistViewHolder(binding, onItemClick, onRemoveClick)
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WishlistViewHolder(
        private val binding: ItemWishlistBinding,
        private val onItemClick: (Listing) -> Unit,
        private val onRemoveClick: (Listing) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listing: Listing) {
            binding.tvTitle.text = listing.title
            binding.tvLocation.text = listing.location
            binding.tvGuests.text = listing.guestSummary()
            binding.tvPrice.text = listing.formattedPrice()

            binding.root.setOnClickListener {
                onItemClick(listing)
            }

            binding.btnRemove.setOnClickListener {
                onRemoveClick(listing)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Listing>() {
        override fun areItemsTheSame(oldItem: Listing, newItem: Listing): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Listing, newItem: Listing): Boolean {
            return oldItem == newItem
        }
    }
}
