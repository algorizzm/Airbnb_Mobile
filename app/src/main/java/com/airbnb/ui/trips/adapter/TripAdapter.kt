package com.airbnb.ui.trips.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.R
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

            // Set trip details
            binding.tvTitle.text = tripItem.title()
            binding.tvLocation.text = tripItem.location()

            // Format dates
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val checkIn = reservation.checkInDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
            val checkOut = reservation.checkOutDate?.toDate()?.let { dateFormat.format(it) } ?: "N/A"
            binding.tvDates.text = "$checkIn - $checkOut"

            // Set status
            binding.tvStatus.text = reservation.statusLabel()
            binding.tvStatus.setTextColor(
                when {
                    tripItem.isUpcoming() -> binding.root.context.getColor(R.color.green)
                    tripItem.isCancelled() -> binding.root.context.getColor(R.color.red)
                    else -> binding.root.context.getColor(R.color.gray)
                }
            )

            // Set price
            binding.tvPrice.text = reservation.formattedTotalPrice()

            // Show cancel button only for upcoming trips
            if (tripItem.isUpcoming() && reservation.isCancellable()) {
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
