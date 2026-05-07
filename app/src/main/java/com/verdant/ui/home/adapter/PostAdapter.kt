package com.verdant.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.verdant.databinding.ItemPostBinding
import com.verdant.data.model.HikePost
import androidx.core.view.isVisible
import com.verdant.utils.Permissions

class PostAdapter(
    private val hikes: List<HikePost>,
    private val userRole: String?
) : RecyclerView.Adapter<PostAdapter.HikeViewHolder>() {

    inner class HikeViewHolder(val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HikeViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HikeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HikeViewHolder, position: Int) {

        val hike = hikes[position]

        with(holder.binding) {

            tvUsername.text = hike.username
            tvDate.text = "${hike.date} at ${hike.time}"
            tvLoc.text = "@${hike.location}"
            tvTitle.text = hike.title
            tvStats.text =
                "${hike.distance} • ${hike.elevation} • ${hike.duration}"

            // ------------------------------------------------
            // IMAGES
            // ------------------------------------------------

            rvImages.layoutManager =
                LinearLayoutManager(
                    root.context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            rvImages.adapter = PostImageAdapter(hike.images)

            // ------------------------------------------------
            // RBAC PERMISSIONS
            // ------------------------------------------------

            val canLike =
                Permissions.canLikePosts(userRole)

            val canComment =
                Permissions.canCommentPosts(userRole)

            val canShare =
                Permissions.canSharePosts(userRole)

            // ------------------------------------------------
            // LIKE BUTTON
            // ------------------------------------------------

            btnLike.isEnabled = canLike
            btnLike.alpha = if (canLike) 1f else 0.4f

            btnLike.setOnClickListener {

                if (!canLike) {

                    // TODO:
                    // Show login/signup dialog

                    return@setOnClickListener
                }

                // TODO:
                // Like post logic
            }

            // ------------------------------------------------
            // COMMENT BUTTON
            // ------------------------------------------------

            btnComment.isEnabled = canComment
            btnComment.alpha = if (canComment) 1f else 0.4f

            btnComment.setOnClickListener {

                if (!canComment) {

                    // TODO:
                    // Show login/signup dialog

                    return@setOnClickListener
                }

                // TODO:
                // Open comments
            }

            // ------------------------------------------------
            // SHARE BUTTON
            // ------------------------------------------------

            btnShare.isEnabled = canShare
            btnShare.alpha = if (canShare) 1f else 0.4f

            btnShare.setOnClickListener {

                if (!canShare) {

                    // TODO:
                    // Show login/signup dialog

                    return@setOnClickListener
                }

                // TODO:
                // Share logic
            }
        }
    }

    override fun getItemCount(): Int = hikes.size
}