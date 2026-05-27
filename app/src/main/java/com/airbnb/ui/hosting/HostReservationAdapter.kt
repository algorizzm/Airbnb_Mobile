package com.airbnb.ui.hosting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.R
import com.airbnb.data.model.Reservation
import com.airbnb.databinding.ItemHostReservationBinding
import java.text.SimpleDateFormat
import java.util.Locale

class HostReservationAdapter(
    private val onApproveClick: (Reservation) -> Unit,
    private val onRejectClick: (Reservation) -> Unit,
    private val onCancelClick: (Reservation) -> Unit
) : ListAdapter<Reservation, HostReservationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHostReservationBinding.inflate(
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
        private val binding: ItemHostReservationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(reservation: Reservation) {
            binding.apply {
                // Guest name
                tvGuestName.text = reservation.guestName

                // Dates
                val checkIn = reservation.checkInDate?.toDate()
                val checkOut = reservation.checkOutDate?.toDate()
                
                if (checkIn != null && checkOut != null) {
                    tvDates.text = "${dateFormat.format(checkIn)} - ${dateFormat.format(checkOut)}"
                } else {
                    tvDates.text = "Dates unavailable"
                }

                // Guests
                tvGuests.text = "${reservation.numberOfGuests} guest${if (reservation.numberOfGuests > 1) "s" else ""}"

                // Total price
                tvTotalPrice.text = root.context.getString(
                    R.string.price_format,
                    reservation.totalPrice.toInt()
                )

                // Status
                tvStatus.text = reservation.status.replaceFirstChar { it.uppercase() }
                
                val statusColor = when (reservation.status) {
                    "confirmed" -> ContextCompat.getColor(root.context, R.color.status_active)
                    "pending" -> ContextCompat.getColor(root.context, R.color.status_pending)
                    "rejected" -> ContextCompat.getColor(root.context, R.color.status_rejected)
                    "cancelled" -> ContextCompat.getColor(root.context, R.color.status_cancelled)
                    "completed" -> ContextCompat.getColor(root.context, R.color.status_completed)
                    else -> ContextCompat.getColor(root.context, R.color.gray)
                }
                tvStatus.setTextColor(statusColor)

                // Action buttons based on status
                when (reservation.status) {
                    "pending" -> {
                        // Show approve and reject buttons for pending reservations
                        btnApprove.visibility = View.VISIBLE
                        btnReject.visibility = View.VISIBLE
                        btnCancel.visibility = View.GONE
                        
                        btnApprove.setOnClickListener { onApproveClick(reservation) }
                        btnReject.setOnClickListener { onRejectClick(reservation) }
                    }
                    "confirmed" -> {
                        // Show cancel button for confirmed reservations
                        btnApprove.visibility = View.GONE
                        btnReject.visibility = View.GONE
                        btnCancel.visibility = View.VISIBLE
                        
                        btnCancel.setOnClickListener { onCancelClick(reservation) }
                    }
                    else -> {
                        // No actions for rejected, cancelled, or completed
                        btnApprove.visibility = View.GONE
                        btnReject.visibility = View.GONE
                        btnCancel.visibility = View.GONE
                    }
                }
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
