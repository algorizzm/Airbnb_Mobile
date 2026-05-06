package com.hikora.ui.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hikora.databinding.ItemPostBinding
import com.hikora.data.model.HikePost
class PostAdapter(
    private val hikes: List<HikePost>
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
            tvStats.text = "${hike.distance} • ${hike.elevation} • ${hike.duration}"

            // Setup horizontal images
            rvImages.layoutManager =
                LinearLayoutManager(root.context, LinearLayoutManager.HORIZONTAL, false)

            rvImages.adapter = PostImageAdapter(hike.images)
        }
    }

    override fun getItemCount(): Int = hikes.size
}