package com.airbnb.ui.host.today.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.R
import com.airbnb.data.model.Reservation
import com.airbnb.databinding.ItemTodayCheckinBinding
import com.airbnb.utils.formatting.DateFormatter
import com.bumptech.glide.Glide

/**
 * Adapter for displaying today's check-ins.
 */
class TodayCheckInAdapter(
    private val onItemClick: (Reservation) -> Unit
) : ListAdapter<Reservation, TodayCheckInAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTodayCheckinBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemTodayCheckinBinding,
        private val onItemClick: (Reservation) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(reservation: Reservation) {
            binding.apply {
                // Guest name
                tvGuestName.text = reservation.guestName

                // Listing title
                tvListingTitle.text = reservation.listingTitle

                // Check-in message
                tvCheckInMessage.text = DateFormatter.formatCheckInMessage(
                    reservation.checkInDate,
                    reservation.checkedIn
                )

                // Guest avatar
                if (reservation.guestAvatarUrl.isNullOrEmpty()) {
                    ivGuestAvatar.setImageResource(R.drawable.ic_airbnb)
                } else {
                    Glide.with(ivGuestAvatar.context)
                        .load(reservation.guestAvatarUrl)
                        .placeholder(R.drawable.ic_airbnb)
                        .circleCrop()
                        .into(ivGuestAvatar)
                }

                // Click listeners
                root.setOnClickListener { onItemClick(reservation) }
                btnViewReservation.setOnClickListener { onItemClick(reservation) }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Reservation>() {
        override fun areItemsTheSame(oldItem: Reservation, newItem: Reservation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Reservation, newItem: Reservation): Boolean {
            return oldItem == newItem
        }
    }
}
