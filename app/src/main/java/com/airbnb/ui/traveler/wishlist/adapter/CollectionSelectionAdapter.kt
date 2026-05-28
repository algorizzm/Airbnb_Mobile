package com.airbnb.ui.traveler.wishlist.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.data.model.WishlistCollection
import com.airbnb.databinding.ItemCollectionSelectionBinding

/**
 * Adapter for displaying collections in the selection dialog.
 */
class CollectionSelectionAdapter(
    private val onCollectionClick: (WishlistCollection) -> Unit
) : ListAdapter<WishlistCollection, CollectionSelectionAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCollectionSelectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onCollectionClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemCollectionSelectionBinding,
        private val onCollectionClick: (WishlistCollection) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(collection: WishlistCollection) {
            binding.tvCollectionName.text = collection.name
            binding.tvListingCount.text = collection.sizeSummary()

            binding.root.setOnClickListener {
                onCollectionClick(collection)
            }
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
