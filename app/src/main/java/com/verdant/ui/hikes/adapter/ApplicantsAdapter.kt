package com.verdant.ui.hikes.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.verdant.data.model.Booking
import com.verdant.databinding.ItemApplicantBinding
import com.verdant.core.ui.AvatarHelper
import com.verdant.utils.BookingStatus

class ApplicantsAdapter(
    private val onApprove: (Booking) -> Unit,
    private val onReject: (Booking) -> Unit
) : ListAdapter<Booking, ApplicantsAdapter.VH>(DiffCallback) {

    var managementEnabled: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemApplicantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding, onApprove, onReject)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), managementEnabled)
    }

    class VH(
        private val binding: ItemApplicantBinding,
        private val onApprove: (Booking) -> Unit,
        private val onReject: (Booking) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking, canManage: Boolean) {
            val name = booking.userName.ifBlank { booking.userId }
            binding.tvUserName.text = name
            binding.tvStatus.text = "Status: ${booking.status}"

            AvatarHelper.bind(
                imgView = binding.imgApplicantAvatar,
                tvInitial = binding.tvApplicantInitial,
                name = name,
                imageUrl = null
            )
            val showActions = canManage && booking.status == BookingStatus.PENDING
            binding.actionsRow.visibility = if (showActions) View.VISIBLE else View.GONE
            binding.btnApprove.setOnClickListener { onApprove(booking) }
            binding.btnReject.setOnClickListener { onReject(booking) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean = oldItem == newItem
    }
}
