package com.airbnb.ui.host.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.data.model.BlockedDate
import com.airbnb.databinding.ItemBlockedDateBinding
import com.airbnb.utils.formatting.DateFormatter
import java.util.*

/**
 * Adapter for displaying blocked date ranges in the host calendar.
 */
class BlockedDatesAdapter(
    private val onUnblockClick: (BlockedDate) -> Unit
) : ListAdapter<BlockedDate, BlockedDatesAdapter.ViewHolder>(DiffCallback) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBlockedDateBinding.inflate(
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
        private val binding: ItemBlockedDateBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(blockedDate: BlockedDate) {
            binding.apply {
                // Use centralized date formatter
                tvDateRange.text = DateFormatter.formatReservationRange(
                    blockedDate.startDate,
                    blockedDate.endDate
                )
                
                tvReason.text = if (blockedDate.reason.isNotBlank()) {
                    blockedDate.reason
                } else {
                    "No reason provided"
                }
                
                btnUnblock.setOnClickListener {
                    onUnblockClick(blockedDate)
                }
            }
        }
    }
    
    private object DiffCallback : DiffUtil.ItemCallback<BlockedDate>() {
        override fun areItemsTheSame(oldItem: BlockedDate, newItem: BlockedDate): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: BlockedDate, newItem: BlockedDate): Boolean {
            return oldItem == newItem
        }
    }
}
