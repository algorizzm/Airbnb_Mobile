package com.verdant.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.verdant.data.model.FeedItem
import com.verdant.data.model.HikePost
import com.verdant.databinding.ItemHomeCtaBinding
import com.verdant.databinding.ItemPostBinding
import com.verdant.utils.Permissions

class PostAdapter(
    private val items: List<FeedItem>,
    private val userRole: String?,
    private val onHikeClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CTA = 0
        private const val TYPE_POST = 1
    }

    // =====================================================
    // VIEW HOLDERS
    // =====================================================

    inner class CTAViewHolder(
        val binding: ItemHomeCtaBinding
    ) : RecyclerView.ViewHolder(binding.root)

    inner class HikeViewHolder(
        val binding: ItemPostBinding
    ) : RecyclerView.ViewHolder(binding.root)

    // =====================================================
    // VIEW TYPES
    // =====================================================

    override fun getItemViewType(position: Int): Int {

        return when (items[position]) {

            is FeedItem.CTA -> TYPE_CTA

            is FeedItem.Post -> TYPE_POST
        }
    }

    // =====================================================
    // CREATE VIEW HOLDER
    // =====================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        return when (viewType) {

            TYPE_CTA -> {

                val binding = ItemHomeCtaBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

                CTAViewHolder(binding)
            }

            else -> {

                val binding = ItemPostBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

                HikeViewHolder(binding)
            }
        }
    }

    // =====================================================
    // BIND VIEW HOLDER
    // =====================================================

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        when (val item = items[position]) {

            // -------------------------------------------------
            // CTA ITEM
            // -------------------------------------------------

            is FeedItem.CTA -> {

                val binding =
                    (holder as CTAViewHolder).binding

                binding.btnHike.setOnClickListener {
                    onHikeClick()
                }
            }

            // -------------------------------------------------
            // POST ITEM
            // -------------------------------------------------

            is FeedItem.Post -> {

                bindPost(
                    holder as HikeViewHolder,
                    item.hikePost
                )
            }
        }
    }

    // =====================================================
    // POST BINDING
    // =====================================================

    private fun bindPost(
        holder: HikeViewHolder,
        hike: HikePost
    ) {

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

            rvImages.adapter =
                PostImageAdapter(hike.images)

            // ------------------------------------------------
            // RBAC
            // ------------------------------------------------

            val canLike =
                Permissions.canLikePosts(userRole)

            val canComment =
                Permissions.canCommentPosts(userRole)

            val canShare =
                Permissions.canSharePosts(userRole)

            // ------------------------------------------------
            // LIKE
            // ------------------------------------------------

            btnLike.isEnabled = canLike
            btnLike.alpha = if (canLike) 1f else 0.4f

            // ------------------------------------------------
            // COMMENT
            // ------------------------------------------------

            btnComment.isEnabled = canComment
            btnComment.alpha = if (canComment) 1f else 0.4f

            // ------------------------------------------------
            // SHARE
            // ------------------------------------------------

            btnShare.isEnabled = canShare
            btnShare.alpha = if (canShare) 1f else 0.4f
        }
    }

    override fun getItemCount(): Int = items.size
}