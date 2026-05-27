package com.airbnb.ui.hikes.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.databinding.ItemUserBookingBinding
import com.airbnb.ui.hikes.history.UserBookingRow
import com.airbnb.utils.BookingStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserBookingsAdapter(
    private val onItemClick: (UserBookingRow) -> Unit,
    private val onCancelOrLeave: (UserBookingRow) -> Unit
) : ListAdapter<UserBookingRow, UserBookingsAdapter.VH>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUserBookingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding, onItemClick, onCancelOrLeave)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemUserBookingBinding,
        private val onItemClick: (UserBookingRow) -> Unit,
        private val onCancelOrLeave: (UserBookingRow) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        fun bind(row: UserBookingRow) {
            val b = row.booking

            binding.tvHikeTitle.text = row.hikeTitle

            // Date
            val dateStr = b.createdAt?.toDate()?.let { dateFmt.format(it) }
                ?: dateFmt.format(Date())
            binding.tvBookingDate.text = "Applied $dateStr"

            // Status pill — text + background tint
            binding.tvStatus.text = b.status.replaceFirstChar { it.uppercase() }
            val (textColor, bgColor) = statusColors(b.status)
            binding.tvStatus.setTextColor(Color.parseColor(textColor))
            binding.tvStatus.setBackgroundColor(Color.parseColor(bgColor))

            // Cancel / Leave button — only for active bookings
            val canAct = b.status == BookingStatus.PENDING || b.status == BookingStatus.APPROVED
            binding.btnCancelOrLeave.visibility = if (canAct) View.VISIBLE else View.GONE
            binding.btnCancelOrLeave.text =
                if (b.status == BookingStatus.APPROVED) "Leave hike" else "Cancel"
            binding.btnCancelOrLeave.setOnClickListener { onCancelOrLeave(row) }

            // Whole card navigates to hike detail
            binding.root.setOnClickListener { onItemClick(row) }
        }

        private fun statusColors(status: String): Pair<String, String> = when (status) {
            BookingStatus.APPROVED   -> "#000000" to "#02D083"   // green
            BookingStatus.PENDING    -> "#000000" to "#FFA726"   // amber
            BookingStatus.REJECTED   -> "#FFFFFF" to "#FF4444"   // red
            BookingStatus.CANCELLED  -> "#FFFFFF" to "#555555"   // grey
            else                     -> "#FFFFFF" to "#333333"
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<UserBookingRow>() {
        override fun areItemsTheSame(a: UserBookingRow, b: UserBookingRow) =
            a.booking.id == b.booking.id
        override fun areContentsTheSame(a: UserBookingRow, b: UserBookingRow) = a == b
    }
}
