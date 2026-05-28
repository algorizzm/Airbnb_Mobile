package com.airbnb.ui.shared.profile

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController

import com.airbnb.R
import com.airbnb.core.auth.AuthManager
import com.airbnb.core.mode.AppMode
import com.airbnb.core.mode.AppModeManager
import com.airbnb.core.ui.AvatarHelper
import com.airbnb.core.ui.EditTextDialog
import com.airbnb.databinding.FragmentProfileBinding
import com.airbnb.ui.auth.GuestPromptDialog
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
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->

            uri ?: return@registerForActivityResult

            showSelectedAvatar(uri)
            viewModel.uploadAvatar(uri)
        }

    // =========================================================
    // PERMISSION
    // =========================================================

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

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
        adjustFloatingCardForBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // =========================================================
    // OBSERVE
    // =========================================================

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.state.collect { state ->
                    renderAll(state)
                }
            }
        }
    }

    // =========================================================
    // RENDER
    // =========================================================

    private fun renderAll(state: ProfileUiState) {
        renderGuestLayout(state.isGuest)
        renderProfile(state)
        renderSwitchCard(state.isGuest, state.currentMode)
        renderLoading(state.avatarUploading)
        renderMessage(state.message)
    }

    private fun renderGuestLayout(isGuest: Boolean) {
        binding.layoutGuest.visibility =
            if (isGuest) View.VISIBLE else View.GONE

        binding.layoutProfile.visibility =
            if (isGuest) View.GONE else View.VISIBLE
    }

    private fun renderProfile(state: ProfileUiState) {

        val user = state.user ?: return

        binding.tvFullName.text =
            user.name.ifBlank { "Guest User" }

        binding.tvUsername.text =
            if (user.email.isBlank()) {
                "@guest"
            } else {
                "@${user.email.substringBefore("@")}"
            }

        binding.tvLocation.text =
            user.location.ifBlank { "Philippines" }

        binding.tvBio.text =
            user.bio?.takeIf { it.isNotBlank() }
                ?: "Tell guests about yourself."

        AvatarHelper.bind(
            imgView = binding.imgAvatar,
            tvInitial = binding.tvAvatarInitial,
            name = user.name,
            imageUrl = user.profileImage
        )
    }

    private fun renderSwitchCard(
        isGuest: Boolean,
        mode: AppMode
    ) {

        if (isGuest) {
            binding.floatingHostingCard.visibility = View.GONE
            return
        }

        binding.floatingHostingCard.visibility = View.VISIBLE

        binding.tvFloatingHostingTitle.text =
            when (mode) {
                AppMode.TRAVELER -> getString(R.string.switch_to_hosting)
                AppMode.HOST -> getString(R.string.switch_to_traveling)
            }
    }

    private fun renderLoading(loading: Boolean) {
        binding.progressAvatar.visibility =
            if (loading) View.VISIBLE else View.GONE
    }

    private fun renderMessage(message: String?) {

        message ?: return

        toast(message)
        viewModel.consumeMessage()
    }

    // =========================================================
    // CLICKS
    // =========================================================

    private fun setupClicks() {

        binding.imgAvatar.setOnClickListener {
            requestGalleryPermission()
        }

        binding.btnUploadPhoto.setOnClickListener {
            requestGalleryPermission()
        }

        binding.btnEditBio.setOnClickListener {
            showEditBioDialog()
        }

        binding.rowEditProfile.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }

        binding.rowHostingAccess.setOnClickListener {
            openHosting()
        }

        binding.tvSeeAllHikes.setOnClickListener {
            findNavController().navigate(R.id.tripsFragment)
        }

        binding.rowAppInfo.setOnClickListener {
            showAppInfoDialog()
        }

        binding.rowSwitchAccount.setOnClickListener {
            showSwitchAccountDialog()
        }

        binding.rowLogout.setOnClickListener {
            showLogoutDialog()
        }

        binding.floatingHostingCard.setOnClickListener {
            onSwitchModeClicked()
        }

        binding.btnLogin.setOnClickListener {
            GuestPromptDialog.show(childFragmentManager)
        }

        binding.btnSignup.setOnClickListener {
            GuestPromptDialog.show(childFragmentManager)
        }
    }

    // =========================================================
    // SWITCH ACCOUNT
    // =========================================================

    private fun showSwitchAccountDialog() {

        AlertDialog.Builder(requireContext())
            .setTitle("Switch account")
            .setMessage("Do you want to switch to another account?")
            .setPositiveButton("Switch") { _, _ ->
                switchAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun switchAccount() {
        AppModeManager.resetToTraveler()
        AuthManager.signOut()

        GuestPromptDialog.show(childFragmentManager)
    }

    // =========================================================
    // MODE
    // =========================================================

    private fun onSwitchModeClicked() {

        if (!AuthManager.isAuthenticated()) {
            GuestPromptDialog.show(childFragmentManager)
            return
        }

        val navController = findNavController()

        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(
                navController.graph.startDestinationId,
                false
            )
            .build()

        when (AppModeManager.toggleMode()) {

            AppMode.HOST -> {
                navController.navigate(
                    R.id.hostTodayFragment,
                    null,
                    navOptions
                )
            }

            AppMode.TRAVELER -> {
                navController.navigate(
                    R.id.exploreFragment,
                    null,
                    navOptions
                )
            }

            null -> Unit
        }
    }

    // =========================================================
    // GALLERY
    // =========================================================

    private fun requestGalleryPermission() {

        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

    private fun openGallery() {
        imagePickerLauncher.launch("image/*")
    }

    private fun showSelectedAvatar(uri: Uri) {

        binding.tvAvatarInitial.visibility = View.GONE

        Glide.with(this)
            .load(uri)
            .circleCrop()
            .into(binding.imgAvatar)
    }

    // =========================================================
    // DIALOGS
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

    private fun showAppInfoDialog() {

        AlertDialog.Builder(requireContext())
            .setTitle("About Airbnb Clone")
            .setMessage("Built with Kotlin and Firebase.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showLogoutDialog() {

        AlertDialog.Builder(requireContext())
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log out") { _, _ ->

                AppModeManager.resetToTraveler()
                AuthManager.signOut()

                findNavController().navigate(R.id.exploreFragment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    private fun openHosting() {
        findNavController().navigate(
            R.id.action_profileFragment_to_hostListingsFragment
        )
    }

    // =========================================================
    // UI
    // =========================================================

    private fun adjustFloatingCardForBottomNav() {

        (binding.floatingHostingCard.layoutParams as FrameLayout.LayoutParams)
            .bottomMargin = 20.dp()
    }

    private fun Int.dp(): Int {
        return (
                this *
                        requireContext()
                            .resources
                            .displayMetrics
                            .density
                ).toInt()
    }

    private fun toast(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}