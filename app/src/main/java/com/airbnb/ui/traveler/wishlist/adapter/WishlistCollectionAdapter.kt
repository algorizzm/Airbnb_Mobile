package com.airbnb.ui.traveler.wishlist.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.data.model.WishlistCollection
import com.airbnb.databinding.ItemWishlistCollectionBinding

/**
 * Adapter for displaying wishlist collections in the main wishlist screen.
 */
class WishlistCollectionAdapter(
    private val onCollectionClick: (WishlistCollection) -> Unit,
    private val onCollectionLongClick: (WishlistCollection) -> Unit
) : ListAdapter<WishlistCollection, WishlistCollectionAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWishlistCollectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onCollectionClick, onCollectionLongClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemWishlistCollectionBinding,
        private val onCollectionClick: (WishlistCollection) -> Unit,
        private val onCollectionLongClick: (WishlistCollection) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(collection: WishlistCollection) {
            binding.tvCollectionName.text = collection.name
            binding.tvListingCount.text = collection.sizeSummary()

            binding.root.setOnClickListener {
                onCollectionClick(collection)
            }

            binding.root.setOnLongClickListener {
                onCollectionLongClick(collection)
                true
            }

            // TODO: Load cover image from first listing
            // For now, leave placeholder
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<WishlistCollection>() {
        override fun areItemsTheSame(
            oldItem: WishlistCollection,
            newItem: WishlistCollection
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: WishlistCollection,
            newItem: WishlistCollection
        ): Boolean = oldItem == newItem
    }
}
