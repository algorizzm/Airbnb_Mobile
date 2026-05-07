package com.verdant.ui.hikes.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.verdant.data.model.Hike
import com.verdant.databinding.ItemHikeBinding

class HikeAdapter(
    private val onItemClick: (Hike) -> Unit
) : ListAdapter<Hike, HikeAdapter.HikeViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HikeViewHolder {
        val binding = ItemHikeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HikeViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: HikeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HikeViewHolder(
        private val binding: ItemHikeBinding,
        private val onItemClick: (Hike) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(hike: Hike) {
            binding.tvTitle.text = hike.title
            binding.tvLocation.text = hike.location
            binding.tvDifficulty.text = hike.difficulty
            binding.tvPrice.text = "₱%.2f".format(hike.price)
            binding.root.setOnClickListener { onItemClick(hike) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Hike>() {
        override fun areItemsTheSame(oldItem: Hike, newItem: Hike): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Hike, newItem: Hike): Boolean = oldItem == newItem
    }
}
