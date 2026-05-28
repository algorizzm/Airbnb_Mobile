package com.airbnb.ui.host.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.data.model.Reservation
import com.airbnb.databinding.ItemCalendarReservationBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying reservations in the host calendar.
 */
class CalendarReservationsAdapter : ListAdapter<Reservation, CalendarReservationsAdapter.ViewHolder>(DiffCallback) {
    
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCalendarReservationBinding.inflate(
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
        private val binding: ItemCalendarReservationBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(reservation: Reservation) {
            binding.apply {
                tvGuestName.text = reservation.guestName
                tvReservationCode.text = reservation.reservationCode ?: reservation.id.take(8)
                tvStatus.text = reservation.statusLabel()
                
                val checkIn = reservation.checkInDate?.toDate()
                val checkOut = reservation.checkOutDate?.toDate()
                
                if (checkIn != null && checkOut != null) {
                    tvDates.text = "${dateFormatter.format(checkIn)} - ${dateFormatter.format(checkOut)}"
                } else {
                    tvDates.text = "Invalid dates"
                }
                
                tvGuests.text = reservation.guestSummary()
                tvPrice.text = reservation.formattedTotalPrice()
                
                // Status color
                val statusColor = when (reservation.status.lowercase()) {
                    "pending" -> 0xFFFFA500.toInt() // Orange
                    "confirmed", "upcoming", "active_stay" -> 0xFF4CAF50.toInt() // Green
                    "completed" -> 0xFF2196F3.toInt() // Blue
                    "cancelled", "rejected" -> 0xFFF44336.toInt() // Red
                    else -> 0xFF757575.toInt() // Gray
                }
                tvStatus.setTextColor(statusColor)
            }
        }
    }
    
    private object DiffCallback : DiffUtil.ItemCallback<Reservation>() {
        override fun areItemsTheSame(oldItem: Reservation, newItem: Reservation): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Reservation, newItem: Reservation): Boolean {
            return oldItem == newItem
        }
    }
}
