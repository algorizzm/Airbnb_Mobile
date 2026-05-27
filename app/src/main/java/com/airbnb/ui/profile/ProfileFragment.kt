package com.airbnb.ui.profile

import android.Manifest
import android.app.AlertDialog
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
import com.airbnb.R
import com.airbnb.core.auth.AuthManager
import com.airbnb.core.ui.AvatarHelper
import com.airbnb.core.ui.EditTextDialog
import com.airbnb.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // =========================================================
    // VIEWMODEL
    // =========================================================

    private val viewModel: ProfileViewModel by viewModels()

    // =========================================================
    // IMAGE PICKER
    // =========================================================

    private val imagePickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            uri ?: return@registerForActivityResult

            showSelectedAvatar(uri)

            viewModel.uploadAvatar(uri)
        }

    // =========================================================
    // PERMISSION
    // =========================================================

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                openGallery()
            } else {

                toast("Gallery permission denied")
            }
        }

    // =========================================================
    // LIFECYCLE
    // =========================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProfileBinding.bind(view)

        setupClicks()

        observeState()
    }

    // =========================================================
    // ALL CLICKS
    // =========================================================

    private fun setupClicks() {

        // =====================================================
        // TOP BAR
        // =====================================================

        binding.btnNotifications.setOnClickListener {

            findNavController().navigate(
                R.id.notificationsFragment
            )
        }

        // =====================================================
        // GUEST
        // =====================================================

        binding.btnLogin.setOnClickListener {

            findNavController().navigate(
                R.id.auth_graph
            )
        }

        binding.btnSignup.setOnClickListener {

            findNavController().navigate(
                R.id.auth_graph
            )
        }

        // =====================================================
        // AVATAR
        // =====================================================

        binding.imgAvatar.setOnClickListener {

            requestGalleryPermission()
        }

        binding.btnUploadPhoto.setOnClickListener {

            requestGalleryPermission()
        }

        // =====================================================
        // EDIT BIO
        // =====================================================

        binding.btnEditBio.setOnClickListener {

            showEditBioDialog()
        }

        // =====================================================
        // EDIT PROFILE
        // =====================================================

        binding.rowEditProfile.setOnClickListener {

            findNavController().navigate(
                R.id.editProfileFragment
            )
        }

        // =====================================================
        // HOSTING
        // =====================================================

        binding.rowHostingAccess.setOnClickListener {

            openHosting()
        }

        binding.btnSwitchHosting.setOnClickListener {

            openHosting()
        }

        // =====================================================
        // TRIPS
        // =====================================================

        binding.tvSeeAllHikes.setOnClickListener {

            findNavController().navigate(
                R.id.tripsFragment
            )
        }

        // =====================================================
        // APP INFO
        // =====================================================

        binding.rowAppInfo.setOnClickListener {

            showAppInfoDialog()
        }

        // =====================================================
        // LOGOUT
        // =====================================================

        binding.rowLogout.setOnClickListener {

            showLogoutDialog()
        }
    }

    // =========================================================
    // HOSTING
    // =========================================================

    private fun openHosting() {

        findNavController().navigate(
            R.id.action_profileFragment_to_hostListingsFragment
        )
    }

    // =========================================================
    // BIO DIALOG
    // =========================================================

    private fun showEditBioDialog() {

        EditTextDialog.show(
            context = requireContext(),
            title = "Edit Bio",
            initial = viewModel.state.value.user?.bio.orEmpty(),
            hint = "Tell guests about yourself",
            maxLength = 200,
            multiLine = true
        ) { newBio ->

            viewModel.updateBio(newBio)
        }
    }

    // =========================================================
    // APP INFO
    // =========================================================

    private fun showAppInfoDialog() {

        AlertDialog.Builder(requireContext())
            .setTitle("About Airbnb Clone")
            .setMessage(
                "Version ${appVersionName()}\n\nBuilt with Kotlin and Firebase."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    // =========================================================
    // LOGOUT
    // =========================================================

    private fun showLogoutDialog() {

        AlertDialog.Builder(requireContext())
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log out") { _, _ ->

                AuthManager.signOut()

                findNavController().navigate(
                    R.id.auth_graph
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // =========================================================
    // PERMISSION
    // =========================================================

    private fun requestGalleryPermission() {

        val permission = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {

            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {

                openGallery()
            }

            else -> {

                permissionLauncher.launch(permission)
            }
        }
    }

    // =========================================================
    // GALLERY
    // =========================================================

    private fun openGallery() {

        imagePickerLauncher.launch("image/*")
    }

    // =========================================================
    // SHOW AVATAR
    // =========================================================

    private fun showSelectedAvatar(uri: Uri) {

        binding.tvAvatarInitial.visibility = View.GONE

        Glide.with(this)
            .load(uri)
            .circleCrop()
            .into(binding.imgAvatar)
    }

    // =========================================================
    // OBSERVE
    // =========================================================

    private fun observeState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.state.collect { state ->

                    renderGuestState(state.isGuest)

                    renderUser(state)

                    renderLoading(state.avatarUploading)

                    renderMessage(state.message)
                }
            }
        }
    }

    // =========================================================
    // GUEST STATE
    // =========================================================

    private fun renderGuestState(isGuest: Boolean) {

        binding.layoutGuest.visibility =
            if (isGuest) View.VISIBLE else View.GONE

        binding.layoutProfile.visibility =
            if (isGuest) View.GONE else View.VISIBLE

        binding.btnSwitchHosting.visibility =
            if (isGuest) View.GONE else View.VISIBLE
    }

    // =========================================================
    // USER
    // =========================================================

    private fun renderUser(
        state: ProfileUiState
    ) {

        val user = state.user ?: return

        binding.tvFullName.text =
            user.name.ifBlank { "Guest User" }

        binding.tvUsername.text =
            if (user.name.isBlank()) {
                "@guest"
            } else {
                "@${user.name.lowercase().replace(" ", "")}"
            }

        binding.tvLocation.text =
            user.location.ifBlank {
                "Philippines"
            }

        binding.tvBio.text =
            user.bio?.takeIf {
                it.isNotBlank()
            } ?: "Tell guests about yourself."

        AvatarHelper.bind(
            imgView = binding.imgAvatar,
            tvInitial = binding.tvAvatarInitial,
            name = user.name,
            imageUrl = user.profileImage
        )
    }

    // =========================================================
    // LOADING
    // =========================================================

    private fun renderLoading(
        loading: Boolean
    ) {

        binding.progressAvatar.visibility =
            if (loading) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    // =========================================================
    // MESSAGE
    // =========================================================

    private fun renderMessage(
        message: String?
    ) {

        message ?: return

        toast(message)

        viewModel.consumeMessage()
    }

    // =========================================================
    // VERSION
    // =========================================================

    private fun appVersionName(): String {

        return runCatching {

            val packageInfo =
                requireContext()
                    .packageManager
                    .getPackageInfo(
                        requireContext().packageName,
                        0
                    )

            packageInfo.versionName ?: "1.0"

        }.getOrDefault("1.0")
    }

    // =========================================================
    // TOAST
    // =========================================================

    private fun toast(message: String) {

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // DESTROY
    // =========================================================

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}