package com.verdant.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.verdant.R
import com.verdant.data.model.AppNotification
import com.verdant.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NotificationAdapter(
    private val onClick: (AppNotification) -> Unit
) : ListAdapter<AppNotification, NotificationAdapter.VH>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        private val binding: ItemNotificationBinding,
        private val onClick: (AppNotification) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(n: AppNotification) {
            binding.tvTitle.text = n.title
            binding.tvBody.text = n.body
            binding.tvTime.text = relativeTime(n.timestamp)
            binding.viewUnread.visibility = if (n.read) View.GONE else View.VISIBLE

            // Icon per type
            val iconRes = when (n.type) {
                "booking_approved"   -> R.drawable.ic_check_circle_green
                "booking_rejected"   -> R.drawable.ic_error_vector
                "booking_cancelled"  -> R.drawable.ic_error_vector
                "hike_update"        -> R.drawable.ic_explore
                "message"            -> R.drawable.ic_message
                else                 -> R.drawable.ic_notification_bell
            }
            binding.ivIcon.setImageResource(iconRes)

            binding.root.setOnClickListener { onClick(n) }
        }

        private fun relativeTime(millis: Long): String {
            val diff = System.currentTimeMillis() - millis
            return when {
                diff < TimeUnit.MINUTES.toMillis(1)  -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1)    -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
                diff < TimeUnit.DAYS.toMillis(1)     -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
                diff < TimeUnit.DAYS.toMillis(7)     -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<AppNotification>() {
        override fun areItemsTheSame(a: AppNotification, b: AppNotification) = a.id == b.id
        override fun areContentsTheSame(a: AppNotification, b: AppNotification) = a == b
    }
}
