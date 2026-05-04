package com.hikora.ui.hikes.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hikora.databinding.ItemUserBookingBinding
import com.hikora.ui.hikes.UserBookingRow
import com.hikora.utils.BookingStatus

class UserBookingsAdapter(
    private val onCancelOrLeave: (UserBookingRow) -> Unit
) : ListAdapter<UserBookingRow, UserBookingsAdapter.VH>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUserBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding, onCancelOrLeave)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemUserBookingBinding,
        private val onCancelOrLeave: (UserBookingRow) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: UserBookingRow) {
            val b = row.booking
            binding.tvHikeTitle.text = row.hikeTitle
            binding.tvStatus.text = "Status: ${b.status}"
            val canAct = b.status == BookingStatus.PENDING || b.status == BookingStatus.APPROVED
            binding.btnCancelOrLeave.visibility = if (canAct) View.VISIBLE else View.GONE
            binding.btnCancelOrLeave.text = if (b.status == BookingStatus.APPROVED) "Leave hike" else "Cancel"
            binding.btnCancelOrLeave.setOnClickListener { onCancelOrLeave(row) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<UserBookingRow>() {
        override fun areItemsTheSame(oldItem: UserBookingRow, newItem: UserBookingRow): Boolean =
            oldItem.booking.id == newItem.booking.id

        override fun areContentsTheSame(oldItem: UserBookingRow, newItem: UserBookingRow): Boolean =
            oldItem == newItem
    }
}
