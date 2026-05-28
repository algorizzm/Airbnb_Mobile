package com.airbnb.ui.traveler.trips.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.R
import com.airbnb.data.model.ReservationStatus
import com.airbnb.data.model.TripItem
import com.airbnb.databinding.ItemTripBinding
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class TripAdapter(
    private val onItemClick: (TripItem) -> Unit,
    private val onCancelClick: (TripItem) -> Unit
) : ListAdapter<TripItem, TripAdapter.TripViewHolder>(TripDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TripViewHolder(
        private val binding: ItemTripBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tripItem: TripItem) {
            val reservation = tripItem.reservation

            // Set listing image
            if (tripItem.imageUrl().isNotBlank()) {
                binding.imgTrip.visibility = View.VISIBLE
                Glide.with(binding.root.context)
                    .load(tripItem.imageUrl())
                    .placeholder(R.drawable.img_hike_placeholder)
                    .error(R.drawable.img_hike_placeholder)
                    .centerCrop()
                    .into(binding.imgTrip)
            } else {
                binding.imgTrip.visibility = View.GONE
            }

            // Set status chip overlay
            binding.tvStatusChip.text = reservation.statusLabel()
            binding.tvStatusChip.setBackgroundResource(getStatusChipBackground(reservation.status))

            // Set trip details
            binding.tvTitle.text = tripItem.title()
            binding.tvLocation.text = tripItem.location()

            // Set host info
            binding.tvHostName.text = "Hosted by ${tripItem.hostName()}"
            
            // Load host avatar
            val hostAvatarUrl = tripItem.hostAvatarUrl()
            if (!hostAvatarUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(hostAvatarUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(binding.imgHostAvatar)
            } else {
                binding.imgHostAvatar.setImageResource(R.drawable.ic_profile)
            }

            // Format dates with nights
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val checkIn = reservation.checkInDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
            val checkOut = reservation.checkOutDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
            val nightsSummary = tripItem.nightsSummary()
            binding.tvDates.text = if (nightsSummary.isNotBlank()) {
                "$checkIn - $checkOut • $nightsSummary"
            } else {
                "$checkIn - $checkOut"
            }

            // Set countdown message
            val countdown = tripItem.countdownMessage()
            binding.tvCountdown.text = countdown
            binding.tvCountdown.visibility = if (tripItem.isUpcoming() || tripItem.isActiveStay()) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Set reservation code
            binding.tvReservationCode.text = "Code: ${tripItem.reservationCode()}"

            // Set guest count
            binding.tvGuests.text = reservation.guestSummary()

            // Set status
            binding.tvStatus.text = reservation.statusLabel()
            binding.tvStatus.setTextColor(getStatusColor(reservation.status))

            // Set price
            binding.tvPrice.text = reservation.formattedTotalPrice()

            // Show cancel button only for cancellable trips
            if (reservation.isCancellable()) {
                binding.btnCancel.visibility = View.VISIBLE
                binding.btnCancel.setOnClickListener {
                    onCancelClick(tripItem)
                }
            } else {
                binding.btnCancel.visibility = View.GONE
            }

            // Handle item click
            binding.root.setOnClickListener {
                onItemClick(tripItem)
            }
        }

        private fun getStatusChipBackground(status: String): Int {
            return when (status.lowercase()) {
                ReservationStatus.UPCOMING -> R.drawable.bg_chip_selected
                ReservationStatus.ACTIVE_STAY -> R.drawable.bg_chip_selected
                ReservationStatus.CONFIRMED -> R.drawable.bg_chip_selected
                ReservationStatus.PENDING -> R.drawable.bg_chip_unselected
                else -> R.drawable.bg_chip_unselected
            }
        }

        private fun getStatusColor(status: String): Int {
            return when (status.lowercase()) {
                ReservationStatus.UPCOMING -> binding.root.context.getColor(R.color.green)
                ReservationStatus.ACTIVE_STAY -> binding.root.context.getColor(R.color.green)
                ReservationStatus.CONFIRMED -> binding.root.context.getColor(R.color.green)
                ReservationStatus.PENDING -> binding.root.context.getColor(R.color.status_pending)
                ReservationStatus.CANCELLED -> binding.root.context.getColor(R.color.red)
                ReservationStatus.REJECTED -> binding.root.context.getColor(R.color.red)
                ReservationStatus.COMPLETED -> binding.root.context.getColor(R.color.gray)
                else -> binding.root.context.getColor(R.color.gray)
            }
        }
    }

    private class TripDiffCallback : DiffUtil.ItemCallback<TripItem>() {
        override fun areItemsTheSame(oldItem: TripItem, newItem: TripItem): Boolean {
            return oldItem.reservation.id == newItem.reservation.id
        }

        override fun areContentsTheSame(oldItem: TripItem, newItem: TripItem): Boolean {
            return oldItem == newItem
        }
    }
}
