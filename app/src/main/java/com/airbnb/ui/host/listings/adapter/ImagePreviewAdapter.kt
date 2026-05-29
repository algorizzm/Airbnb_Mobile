package com.airbnb.ui.host.listings.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.databinding.ItemImagePreviewBinding
import com.bumptech.glide.Glide

/**
 * Horizontal image preview strip shown in [CreateListingFragment] after the
 * user picks images but before they are uploaded to Cloudinary.
 *
 * Each item displays a thumbnail with a remove (✕) button. Tapping remove
 * fires [onRemove] so the ViewModel can drop that URI from its queue.
 *
 * Uses [ListAdapter] + [DiffUtil] for efficient updates.
 */
class ImagePreviewAdapter(
    private val onRemove: (Uri) -> Unit
) : ListAdapter<Uri, ImagePreviewAdapter.ViewHolder>(UriDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImagePreviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemImagePreviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri) {
            Glide.with(binding.imgPreview.context)
                .load(uri)
                .centerCrop()
                .into(binding.imgPreview)

            binding.btnRemove.setOnClickListener { onRemove(uri) }
        }
    }

    private class UriDiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Uri, newItem: Uri) = oldItem == newItem
    }
}
