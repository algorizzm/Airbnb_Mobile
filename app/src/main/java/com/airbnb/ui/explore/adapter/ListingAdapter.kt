package com.airbnb.ui.explore.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.airbnb.R
import com.airbnb.data.model.Listing
import com.airbnb.databinding.ItemListingBinding

class ListingAdapter(
    private val onItemClick: (Listing) -> Unit,
    private val onWishlistClick: ((Listing) -> Unit)? = null,
    private var wishlistIds: Set<String> = emptySet()
) : ListAdapter<Listing, ListingAdapter.ListingViewHolder>(DiffCallback) {

    /**
     * Updates wishlist state in-place and redraws affected items.
     * Avoids full adapter teardown/recreation on every wishlist emission.
     */
    fun updateWishlistIds(ids: Set<String>) {
        wishlistIds = ids
        notifyItemRangeChanged(0, itemCount)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListingViewHolder {

        val binding = ItemListingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ListingViewHolder(
            binding,
            onItemClick,
            onWishlistClick
        )
    }

    override fun onBindViewHolder(
        holder: ListingViewHolder,
        position: Int
    ) {

        val listing = getItem(position)

        holder.bind(
            listing,
            wishlistIds.contains(listing.id)
        )
    }

    class ListingViewHolder(
        private val binding: ItemListingBinding,
        private val onItemClick: (Listing) -> Unit,
        private val onWishlistClick: ((Listing) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            listing: Listing,
            isInWishlist: Boolean
        ) {

            // LOCATION
            binding.tvLocation.text = listing.location

            // TITLE
            binding.tvTitle.text = listing.title

            // GUEST INFO
            binding.tvGuests.text = listing.guestSummary()

            // PRICE
            binding.tvPrice.text =
                "${listing.formattedPrice()} night"

            // RATING
            binding.tvRating.text =
                "★ ${listing.rating}"

            // LOAD IMAGE
            binding.imgListing.load(listing.imageUrl) {

                crossfade(true)

                placeholder(R.drawable.img_hike_placeholder)

                error(R.drawable.img_hike_placeholder)
            }

            // WISHLIST STATE
            if (isInWishlist) {
                binding.btnWishlist.setImageResource(R.drawable.ic_heart_filled)
                binding.btnWishlist.imageTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#FF385C")
                )
            } else {
                binding.btnWishlist.setImageResource(R.drawable.ic_heart)
                binding.btnWishlist.imageTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.WHITE
                )
            }

            // CARD CLICK
            binding.root.setOnClickListener {

                onItemClick(listing)
            }

            // HEART CLICK
            binding.btnWishlist.setOnClickListener {

                onWishlistClick?.invoke(listing)
            }
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