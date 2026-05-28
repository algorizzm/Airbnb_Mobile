package com.airbnb.ui.host.listings.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.airbnb.R
import com.airbnb.data.model.Listing
import com.airbnb.databinding.ItemHostListingBinding

class HostListingAdapter(
    private val onEditClick: (Listing) -> Unit,
    private val onDeleteClick: (Listing) -> Unit,
    private val onViewReservationsClick: (Listing) -> Unit
) : ListAdapter<Listing, HostListingAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHostListingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemHostListingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listing: Listing) {
            binding.apply {
                // Title
                tvTitle.text = if (listing.listingCode.isNullOrBlank()) {
                    listing.title
                } else {
                    "${listing.title} (${listing.listingCode})"
                }

                // Location
                tvLocation.text = listing.location

                // Price
                tvPrice.text = root.context.getString(
                    R.string.price_per_night_format,
                    listing.pricePerNight.toInt()
                )

                // Property type
                tvPropertyType.text = listing.propertyType

                // Image
                Glide.with(root.context)
                    .load(listing.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.img_hike_placeholder)
                    .into(imgListing)

                // Click listeners
                btnEdit.setOnClickListener { onEditClick(listing) }
                btnDelete.setOnClickListener { onDeleteClick(listing) }
                btnViewReservations.setOnClickListener { onViewReservationsClick(listing) }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Listing>() {
        override fun areItemsTheSame(oldItem: Listing, newItem: Listing): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Listing, newItem: Listing): Boolean {
            return oldItem == newItem
        }
    }
}
