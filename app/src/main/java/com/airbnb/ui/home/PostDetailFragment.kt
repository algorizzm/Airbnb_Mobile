package com.airbnb.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.R
import com.airbnb.core.auth.AuthManager
import com.airbnb.core.auth.AuthState
import com.airbnb.core.ui.AvatarHelper
import com.airbnb.core.ui.GuestPromptDialog
import com.airbnb.databinding.FragmentPostDetailBinding
import com.airbnb.ui.home.adapter.PostImageAdapter
import com.airbnb.utils.Permissions

class PostDetailFragment : Fragment(R.layout.fragment_post_detail) {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostDetailBinding.bind(view)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Retrieve args
        val args = arguments ?: return
        val username  = args.getString(ARG_USERNAME, "")
        val date      = args.getString(ARG_DATE, "")
        val time      = args.getString(ARG_TIME, "")
        val location  = args.getString(ARG_LOCATION, "")
        val title     = args.getString(ARG_TITLE, "")
        val stats     = args.getString(ARG_STATS, "")
        val images    = args.getIntArray(ARG_IMAGES)?.toList() ?: emptyList()
        val userRole  = args.getString(ARG_ROLE)

        // Bind data
        binding.tvUsername.text = username
        binding.tvDate.text = "$date at $time"
        binding.tvLocation.text = "@$location"
        binding.tvTitle.text = title
        binding.tvStats.text = stats

        AvatarHelper.bind(binding.imgAvatar, binding.tvAvatarInitial, username, null)

        binding.rvImages.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )
        binding.rvImages.adapter = PostImageAdapter(images)

        // RBAC action buttons
        val canLike    = Permissions.canLikePosts(userRole)
        val canComment = Permissions.canCommentPosts(userRole)
        val canShare   = Permissions.canSharePosts(userRole)

        fun guestGuard(action: () -> Unit) {
            if (AuthManager.stateSnapshot() is AuthState.Guest) {
                GuestPromptDialog.show(childFragmentManager)
            } else {
                action()
            }
        }

        binding.btnLike.alpha    = if (canLike) 1f else 0.4f
        binding.btnComment.alpha = if (canComment) 1f else 0.4f
        binding.btnShare.alpha   = if (canShare) 1f else 0.4f

        binding.btnLike.setOnClickListener {
            guestGuard { binding.btnLike.setColorFilter(0xFF02D083.toInt()) }
        }
        binding.btnComment.setOnClickListener {
            guestGuard { /* open comment sheet */ }
        }
        binding.btnShare.setOnClickListener {
            guestGuard { /* open share sheet */ }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_USERNAME = "username"
        const val ARG_DATE     = "date"
        const val ARG_TIME     = "time"
        const val ARG_LOCATION = "location"
        const val ARG_TITLE    = "title"
        const val ARG_STATS    = "stats"
        const val ARG_IMAGES   = "images"
        const val ARG_ROLE     = "role"

        fun args(
            username: String, date: String, time: String, location: String,
            title: String, stats: String, images: List<Int>, role: String?
        ) = Bundle().apply {
            putString(ARG_USERNAME, username)
            putString(ARG_DATE, date)
            putString(ARG_TIME, time)
            putString(ARG_LOCATION, location)
            putString(ARG_TITLE, title)
            putString(ARG_STATS, stats)
            putIntArray(ARG_IMAGES, images.toIntArray())
            putString(ARG_ROLE, role)
        }
    }
}