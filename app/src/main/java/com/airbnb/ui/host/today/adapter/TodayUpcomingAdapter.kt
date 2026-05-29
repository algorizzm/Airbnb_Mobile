package com.airbnb.ui.host.today.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.data.model.Reservation
import com.airbnb.databinding.ItemTodayUpcomingBinding
import com.airbnb.utils.formatting.DateFormatter

/**
 * Adapter for displaying upcoming reservations.
 */
class TodayUpcomingAdapter(
    private val onItemClick: (Reservation) -> Unit
) : ListAdapter<Reservation, TodayUpcomingAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTodayUpcomingBinding.inflate(
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
        private val binding: ItemTodayUpcomingBinding,
        private val onItemClick: (Reservation) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(reservation: Reservation) {
            binding.apply {
                // Countdown message
                tvCountdownMessage.text = DateFormatter.formatCountdownMessage(
                    reservation.checkInDate,
                    reservation.checkOutDate,
                    reservation.status
                )

                // Guest name
                tvGuestName.text = reservation.guestName

                // Listing title
                tvListingTitle.text = reservation.listingTitle

                // Reservation dates
                val dateRange = DateFormatter.formatReservationRange(
                    reservation.checkInDate,
                    reservation.checkOutDate
                )
                val nights = DateFormatter.formatNights(
                    reservation.checkInDate,
                    reservation.checkOutDate
                )
                tvReservationDates.text = "$dateRange • $nights"

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
