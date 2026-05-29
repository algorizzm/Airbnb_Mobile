package com.airbnb.ui.host.today.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.R
import com.airbnb.data.model.Reservation
import com.airbnb.databinding.ItemTodayActiveStayBinding
import com.airbnb.utils.formatting.DateFormatter
import com.bumptech.glide.Glide
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Adapter for displaying active stays.
 */
class TodayActiveStayAdapter(
    private val onItemClick: (Reservation) -> Unit
) : ListAdapter<Reservation, TodayActiveStayAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTodayActiveStayBinding.inflate(
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
        private val binding: ItemTodayActiveStayBinding,
        private val onItemClick: (Reservation) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(reservation: Reservation) {
            binding.apply {
                // Guest name
                tvGuestName.text = reservation.guestName

                // Listing title
                tvListingTitle.text = reservation.listingTitle

                // Nights remaining
                val checkOutDate = reservation.checkOutDate?.toDate()
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val nightsRemaining = if (checkOutDate != null) {
                    val diff = checkOutDate.time - today.time
                    val nights = TimeUnit.MILLISECONDS.toDays(diff).toInt()
                    when {
                        nights < 0 -> "Check-out overdue"
                        nights == 0 -> "Checking out today"
                        nights == 1 -> "1 night remaining"
                        else -> "$nights nights remaining"
                    }
                } else {
                    "N/A"
                }
                tvNightsRemaining.text = nightsRemaining

                // Reservation info (guest count + code)
                val guestCount = reservation.guestSummary()
                val code = reservation.reservationCode ?: "N/A"
                tvReservationInfo.text = "$guestCount • Code: $code"

                // Listing image
                if (reservation.listingImageUrl.isEmpty()) {
                    ivListingImage.setImageResource(R.drawable.ic_airbnb)
                } else {
                    Glide.with(ivListingImage.context)
                        .load(reservation.listingImageUrl)
                        .placeholder(R.drawable.ic_airbnb)
                        .centerCrop()
                        .into(ivListingImage)
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
