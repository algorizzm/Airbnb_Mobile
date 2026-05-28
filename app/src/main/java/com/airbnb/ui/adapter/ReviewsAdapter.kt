package com.airbnb.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.R
import com.airbnb.core.ui.AvatarHelper
import com.airbnb.data.model.Review
import com.airbnb.databinding.ItemReviewBinding
import com.airbnb.utils.formatting.DateFormatter

/**
 * Adapter for displaying reviews in a RecyclerView.
 * 
 * Features:
 * - Displays reviewer avatar, name, and rating
 * - Shows relative timestamp
 * - Displays review comment with expandable text
 * - Handles missing avatars with placeholder
 */
class ReviewsAdapter : ListAdapter<Review, ReviewsAdapter.ReviewViewHolder>(ReviewDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ReviewViewHolder(
        private val binding: ItemReviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var isExpanded = false
        
        fun bind(review: Review) {
            with(binding) {
                // Reviewer info
                reviewerNameTextView.text = review.reviewerName
                reviewDateTextView.text = DateFormatter.formatRelativeDate(review.createdAt)
                ratingTextView.text = review.formattedRating()
                
                // Reviewer avatar
                if (review.reviewerAvatarUrl.isNullOrBlank()) {
                    AvatarHelper.bind(
                        reviewerAvatarImageView,
                        null,
                        review.reviewerName,
                        null
                    )
                } else {
                    AvatarHelper.bind(
                        reviewerAvatarImageView,
                        null,
                        review.reviewerName,
                        review.reviewerAvatarUrl
                    )
                }
                
                // Review comment
                if (review.hasComment()) {
                    commentTextView.text = review.comment
                    commentTextView.visibility = View.VISIBLE
                    
                    // Check if comment is long enough to need expansion
                    commentTextView.post {
                        val lineCount = commentTextView.lineCount
                        if (lineCount > 5 && !isExpanded) {
                            showMoreTextView.visibility = View.VISIBLE
                        } else {
                            showMoreTextView.visibility = View.GONE
                        }
                    }
                    
                    // Handle show more/less
                    showMoreTextView.setOnClickListener {
                        isExpanded = !isExpanded
                        if (isExpanded) {
                            commentTextView.maxLines = Int.MAX_VALUE
                            showMoreTextView.text = "Show less"
                        } else {
                            commentTextView.maxLines = 5
                            showMoreTextView.text = "Show more"
                        }
                    }
                } else {
                    commentTextView.visibility = View.GONE
                    showMoreTextView.visibility = View.GONE
                }
            }
        }
    }
    
    private class ReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem == newItem
        }
    }
}
