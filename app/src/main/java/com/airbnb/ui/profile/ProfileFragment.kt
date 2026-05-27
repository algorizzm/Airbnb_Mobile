package com.airbnb.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.airbnb.R
import com.airbnb.core.ui.AvatarHelper
import com.airbnb.databinding.FragmentProfileBinding
import com.airbnb.ui.explore.ExploreFragment
import com.airbnb.ui.hikes.history.UserBookingRow
import kotlinx.coroutines.launch
import com.airbnb.core.ui.EditTextDialog

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    // "avatar" or "banner"
    private var pendingPickTarget = "avatar"

    // ── Gallery picker ───────────────────────────────────────────────────────
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        when (pendingPickTarget) {
            "avatar" -> {
                binding.tvAvatarInitial.visibility = View.GONE
                Glide.with(this).load(uri).circleCrop().into(binding.imgAvatar)
                viewModel.uploadAvatar(uri)
            }            "banner" -> {
                Glide.with(this).load(uri).centerCrop().into(binding.imgBanner)
                viewModel.uploadBanner(uri)
            }
        }
    }

    // ── Permission launcher ──────────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openGallery()
        else Toast.makeText(requireContext(), "Gallery permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        setupTopBar()
        setupGuestActions()
        setupImagePickers()
        setupBioEdit()
        setupHostingButton()
        setupHikeNavigation()
        observeState()
    }

    // ── Top bar ──────────────────────────────────────────────────────────────
    private fun setupTopBar() {
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
    }

    // ── Guest buttons ────────────────────────────────────────────────────────
    private fun setupGuestActions() {
        binding.btnLogin.setOnClickListener { findNavController().navigate(R.id.auth_graph) }
        binding.btnSignup.setOnClickListener { findNavController().navigate(R.id.auth_graph) }
    }

    // ── Avatar + Banner pickers ──────────────────────────────────────────────
    private fun setupImagePickers() {
        // Tapping the avatar circle or the small upload button both open gallery
        binding.imgAvatar.setOnClickListener {
            pendingPickTarget = "avatar"
            requestGalleryPermissionOrOpen()
        }
        binding.btnUploadPhoto.setOnClickListener {
            pendingPickTarget = "avatar"
            requestGalleryPermissionOrOpen()
        }
        // Tapping the banner opens gallery for banner
        binding.imgBanner.setOnClickListener {
            pendingPickTarget = "banner"
            requestGalleryPermissionOrOpen()
        }
    }

    private fun requestGalleryPermissionOrOpen() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            == PackageManager.PERMISSION_GRANTED
        ) openGallery()
        else permissionLauncher.launch(permission)
    }

    private fun openGallery() = imagePickerLauncher.launch("image/*")

    // ── Edit Bio ─────────────────────────────────────────────────────────────
    private fun setupBioEdit() {
        binding.btnEditBio.setOnClickListener {
            EditTextDialog.show(
                context   = requireContext(),
                title     = "Edit Bio",
                initial   = viewModel.state.value.user?.bio ?: "",
                hint      = "Tell us about yourself…",
                maxLength = 200,
                multiLine = true
            ) { newBio -> viewModel.updateBio(newBio) }
        }
    }

    // ── Hosting Button ───────────────────────────────────────────────────────
    private fun setupHostingButton() {
        binding.btnSwitchToHosting.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_hostListingsFragment)
        }
    }

    // ── Recent hikes thumbnails ──────────────────────────────────────────────
    private fun setupHikeNavigation() {
        binding.tvSeeAllHikes.setOnClickListener {
            findNavController().navigate(R.id.myBookingsFragment)
        }
    }

    private fun bindRecentHikes(rows: List<UserBookingRow>) {
        val slots = listOf(
            Triple(binding.cardRecentHike1, binding.imgRecentHike1, binding.tvRecentHike1),
            Triple(binding.cardRecentHike2, binding.imgRecentHike2, binding.tvRecentHike2),
            Triple(binding.cardRecentHike3, binding.imgRecentHike3, binding.tvRecentHike3)
        )

        slots.forEachIndexed { i, (card, img, title) ->
            val row = rows.getOrNull(i)
            if (row != null) {
                title.text = row.hikeTitle
                if (row.hikeImageUrl.isNotBlank()) {
                    Glide.with(this)
                        .load(row.hikeImageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.img_hike_placeholder)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(img)
                } else {
                    img.setImageResource(R.drawable.img_hike_placeholder)
                }
                card.setOnClickListener {
                    val bundle = Bundle().apply {
                        putString(ExploreFragment.ARG_HIKE_ID, row.booking.hikeId)
                    }
                    try {
                        findNavController().navigate(R.id.action_profileFragment_to_hikeDetailFragment, bundle)
                    } catch (e: IllegalArgumentException) {
                        // ignore double click
                    }
                }
                card.visibility = View.VISIBLE
            } else {
                card.visibility = View.GONE
            }
        }

        // Show section only when there is at least one real booking
        binding.layoutRecentHikes.visibility =
            if (rows.isNotEmpty()) View.VISIBLE else View.GONE
    }

    // ── State observer ───────────────────────────────────────────────────────
    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->

                    // Guest / authenticated toggle
                    if (state.isGuest) {
                        binding.layoutGuest.visibility = View.VISIBLE
                        binding.layoutProfile.visibility = View.GONE
                        bindRecentHikes(emptyList())
                        return@collect
                    }
                    binding.layoutGuest.visibility = View.GONE
                    binding.layoutProfile.visibility = View.VISIBLE

                    // User fields
                    state.user?.let { user ->

                        binding.tvFullName.text = user.name.ifBlank { "—" }

                        binding.tvUsername.text =
                            if (user.name.isNotBlank())
                                "@${user.name.lowercase().replace(" ", "")}"
                            else "—"

                        binding.tvBio.text =
                            user.bio?.takeIf { it.isNotBlank() } ?: "No bio yet."

                        binding.tvLocation.text =
                            user.location.ifBlank { "Unknown location" }

                        binding.tvStatHikes.text = user.totalHikes.toString()

                        binding.tvStatDistance.text =
                            "%.1fkm".format(user.totalDistance)

                        binding.tvStatSummits.text =
                            user.totalSummits.toString()

                        // Avatar
                        AvatarHelper.bind(
                            imgView = binding.imgAvatar,
                            tvInitial = binding.tvAvatarInitial,
                            name = user.name,
                            imageUrl = user.profileImage
                        )

                        // Banner
                        if (user.bannerImage.isNotBlank()) {
                            Glide.with(this@ProfileFragment)
                                .load(user.bannerImage)
                                .centerCrop()
                                .placeholder(R.drawable.img_hike_placeholder)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(binding.imgBanner)
                        }
                    }

                    // Upload spinners
                    binding.progressAvatar.visibility =
                        if (state.avatarUploading) View.VISIBLE else View.GONE
                    binding.progressBanner.visibility =
                        if (state.bannerUploading) View.VISIBLE else View.GONE

                    // Notification badge
                    val badge = state.unreadCount
                    binding.tvNotifBadge.visibility = if (badge > 0) View.VISIBLE else View.GONE
                    if (badge > 0) {
                        binding.tvNotifBadge.text = if (badge > 99) "99+" else badge.toString()
                    }

                    // Recent hikes
                    bindRecentHikes(state.recentHikes)

                    // Toast
                    state.message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
