package com.airbnb.ui.listings.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.databinding.ItemAmenityChipBinding

/**
 * Adapter for displaying listing amenities as chips in a RecyclerView.
 * 
 * Features:
 * - Displays amenities in a clean chip format
 * - Supports grid layout for compact display
 * - Uses Material Design 3 chip styling
 */
class AmenitiesAdapter : ListAdapter<String, AmenitiesAdapter.AmenityViewHolder>(AmenityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AmenityViewHolder {
        val binding = ItemAmenityChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AmenityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AmenityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AmenityViewHolder(
        private val binding: ItemAmenityChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(amenity: String) {
            binding.chipAmenity.text = amenity
        }
    }

    private class AmenityDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}
