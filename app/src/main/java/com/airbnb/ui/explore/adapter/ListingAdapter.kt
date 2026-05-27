package com.airbnb.ui.explore.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.data.model.Listing
import com.airbnb.databinding.ItemListingBinding

class ListingAdapter(
    private val onItemClick: (Listing) -> Unit
) : ListAdapter<Listing, ListingAdapter.ListingViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val binding = ItemListingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListingViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ListingViewHolder(
        private val binding: ItemListingBinding,
        private val onItemClick: (Listing) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listing: Listing) {
            binding.tvTitle.text = listing.title
            binding.tvLocation.text = listing.location
            binding.tvGuests.text = listing.guestSummary()
            binding.tvPrice.text = listing.formattedPrice()
            binding.root.setOnClickListener { onItemClick(listing) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Listing>() {
        override fun areItemsTheSame(oldItem: Listing, newItem: Listing): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Listing, newItem: Listing): Boolean = oldItem == newItem
    }
}
